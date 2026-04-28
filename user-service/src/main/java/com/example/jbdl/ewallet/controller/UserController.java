package com.example.jbdl.ewallet.controller;

import com.example.jbdl.ewallet.dto.UserCreateRequest;
import com.example.jbdl.ewallet.entity.User;
import com.example.jbdl.ewallet.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/user")
    public User createUser(@Valid @RequestBody UserCreateRequest userCreateRequest) throws JsonProcessingException {
        return userService.create(userCreateRequest.to());
    }

    @GetMapping("/user")
    public User getUser(@RequestParam("id") int id){
        return userService.get(id);
    }
}
