package com.kraiven.taskmanager.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kraiven.taskmanager.dto.UserUpdateRequest;
import com.kraiven.taskmanager.entity.Role;
import com.kraiven.taskmanager.entity.User;
import com.kraiven.taskmanager.exception.ResourceConflictException;
import com.kraiven.taskmanager.exception.ResourceNotFoundException;
import com.kraiven.taskmanager.repository.UserRepository;
import com.kraiven.taskmanager.repository.taskRepository;

/** Applies account rules, password hashing, and user-management operations. */
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final taskRepository taskRepository;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            taskRepository taskRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.taskRepository = taskRepository;
    }

    /** Creates a standard account and stores only its BCrypt password hash. */
    public User register(String username, String password) {
        if (findByUsername(username)) {
            throw new ResourceConflictException("A user with this username already exists");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    public boolean findByUsername(String username) {
        Optional<User> byUsername = userRepository.findByUsername(username);
        return byUsername.isPresent();
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    /** Updates account fields; a supplied password is hashed before saving. */
    public User update(Long id, UserUpdateRequest request) {
        User user = findById(id);
        if (userRepository.existsByUsernameAndIdNot(request.username(), id)) {
            throw new ResourceConflictException("A user with this username already exists");
        }
        user.setUsername(request.username());
        user.setRole(request.role());
        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        return userRepository.save(user);
    }

    /** Keeps task ownership intact by refusing to delete users with tasks. */
    public void delete(Long id) {
        User user = findById(id);
        if (taskRepository.existsByUser_Id(id)) {
            throw new ResourceConflictException(
                    "Cannot delete a user while tasks are assigned to them");
        }
        userRepository.delete(user);
    }
}