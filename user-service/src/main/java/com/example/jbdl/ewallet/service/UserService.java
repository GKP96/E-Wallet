package com.example.jbdl.ewallet.service;

import com.example.jbdl.ewallet.entity.User;
import com.example.jbdl.ewallet.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.jbdl.ewallet.entity.OutboxEvent;
import com.example.jbdl.ewallet.repository.OutboxEventRepository;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;

@Service
public class UserService {

    private static final String USER_CREATE = "user_create";

    @Autowired
    UserRepository userRepository;


    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    StringRedisTemplate redisTemplate;

    private static final String REDIS_USER_PREFIX = "user::";

    @Transactional
    public User create(User user) throws JsonProcessingException {
        user = userRepository.save(user);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userId", user.getId());
        jsonObject.put("userEmail", user.getEmail());
        jsonObject.put("userContact", user.getContact());

        // Save outbox event for user_create
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.eventType = USER_CREATE;
        outboxEvent.payload = jsonObject.toJSONString();
        outboxEventRepository.save(outboxEvent);

        // Instantly cache the new user in Redis
        try {
            String redisKey = REDIS_USER_PREFIX + user.getId();
            String userJson = mapper.writeValueAsString(user);
            redisTemplate.opsForValue().set(redisKey, userJson, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            System.out.println("Warning: Failed to cache user in Redis during creation: " + e.getMessage());
        }

        return user;
    }


    public User get(int id){

        String redisKey = REDIS_USER_PREFIX + id;
        
        try {
            // 1. Search in Redis
            String cachedUserStr = redisTemplate.opsForValue().get(redisKey);
            if (cachedUserStr != null) {
                return mapper.readValue(cachedUserStr, User.class);
            }
        } catch (Exception e) {
            // Log warning, fallback to DB if redis fails
        }

        // 2. Not found in Redis, get from DB
        User user = userRepository.findById(id).orElse(null);

        // 3. Insert into Redis
        if (user != null) {
            try {
                String userJson = mapper.writeValueAsString(user);
                redisTemplate.opsForValue().set(redisKey, userJson, 1, TimeUnit.HOURS);
            } catch (Exception e) {
                // Log warning
            }
        }
        
        return user;
    }
}
