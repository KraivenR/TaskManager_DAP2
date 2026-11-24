package com.kraiven.taskmanager.repository;

import com.kraiven.taskmanager.entity.Task;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;

/** Provides task CRUD plus derived searches by status, title, priority, and owner. */
@Repository 
public interface taskRepository extends JpaRepository<Task, Long > {

    boolean existsByUser_Id(Long userId);

    boolean existsByCategory_Id(Long categoryId);

    Optional<Task> findByPriority(String priority);
    Optional<Task> findByPriorityAndUserUsername(String priority, String username);

    List<Task> findByStatus(Task.TaskStatus status);
    List<Task> findByStatusAndUserUsername(Task.TaskStatus status, String username);

    List<Task> findByTitleContainingIgnoreCase(String title);
    List<Task> findByTitleContainingIgnoreCaseAndUserUsername(String title, String username);

    List<Task> findByUserUsername(String username);

    //new paginated method
    Page <Task> findByStatus(Task.TaskStatus status, Pageable pageable);
    Page<Task> findByStatusAndUserUsername(Task.TaskStatus status, String username, Pageable pageable);
    Page <Task> findByPriority(String priority, Pageable pageable);
    Page <Task> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    Page<Task> findByTitleContainingIgnoreCaseAndUserUsername(String title, String username, Pageable pageable);
    Page<Task> findByTitleContainingIgnoreCaseAndStatus(String title, Task.TaskStatus status, Pageable pageable);
    Page<Task> findByTitleContainingIgnoreCaseAndStatusAndUserUsername(
            String title, Task.TaskStatus status, String username, Pageable pageable);
    Page<Task> findByUserUsername(String username, Pageable pageable);


    

}
