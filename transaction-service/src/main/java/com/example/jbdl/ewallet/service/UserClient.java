package com.example.jbdl.ewallet.service;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class UserClient {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper mapper;

    private static final String REDIS_USER_PREFIX = "user::";

    @Retryable(
            value = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public String getUserEmail(Integer userId) {
        // First try to hit the user-service via HTTP
        JSONObject user = restTemplate.getForObject("http://localhost:9000/user?id=" + userId, JSONObject.class);
        if (user != null && user.containsKey("email")) {
            return (String) user.get("email");
        }
        throw new RuntimeException("User email not found for id: " + userId);
    }

    @Recover
    public String getUserEmailFallback(Exception e, Integer userId) {
        // If HTTP fails 3 times, fallback to checking Redis
        String redisKey = REDIS_USER_PREFIX + userId;
        try {
            String cachedUserStr = redisTemplate.opsForValue().get(redisKey);
            if (cachedUserStr != null) {
                JSONObject jsonObject = mapper.readValue(cachedUserStr, JSONObject.class);
                if (jsonObject.containsKey("email")) {
                    return (String) jsonObject.get("email");
                }
            }
        } catch (Exception redisException) {
            // Ignore redis errors in fallback
        }
        throw new RuntimeException("Failed to fetch user email for id: " + userId + " even from Redis", e);
    }
}
