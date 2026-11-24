package com.kraiven.taskmanager.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kraiven.taskmanager.entity.User;

/** Provides user persistence and username lookups used by login and registration. */
public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);
}
