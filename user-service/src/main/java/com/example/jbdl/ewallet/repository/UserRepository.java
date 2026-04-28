package com.example.jbdl.ewallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.jbdl.ewallet.entity.User;

public interface UserRepository extends JpaRepository<User, Integer> {
}
