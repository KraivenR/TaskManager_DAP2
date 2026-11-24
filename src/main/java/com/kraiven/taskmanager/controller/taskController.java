package com.kraiven.taskmanager.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kraiven.taskmanager.Service.taskResponse;
import com.kraiven.taskmanager.Service.TaskAuditResponse;
import com.kraiven.taskmanager.Service.taskService;
import com.kraiven.taskmanager.dto.TaskRequest;
import com.kraiven.taskmanager.entity.Task;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Exposes task CRUD, search, pagination, sorting, and audit-history endpoints. */
@RestController 
@RequestMapping ("/api/v1/tasks")
@Tag(name = "Tasks", description = "Create, search, and manage tasks")
@SecurityRequirement(name = "bearerAuth")
public class taskController {

    private final taskService taskService;

    public taskController(taskService taskService){

        this.taskService = taskService;
    }

    /** Searches by optional title/status filters and returns paging metadata. */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchTasks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false)Task.TaskStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Task> taskPage;

        if (title != null && status != null) {
            taskPage = taskService.searchTasksByTitleAndStatus(title, status, pageable);
        } else if (title != null) {
            taskPage = taskService.searchTasksByTitle(title, pageable);
        } else if (status != null) {
            taskPage = taskService.searchTasksByStatus(status, pageable);
        } else {
            taskPage = taskService.getAllTasks(pageable);
        }


        List<taskResponse> tasks = taskPage.getContent()
            .stream()
            .map(taskResponse::from)
        .toList();

               Map<String, Object> response = new HashMap<>();
        response.put("tasks", tasks);
        response.put("currentPage", taskPage.getNumber());
        response.put("totalItems", taskPage.getTotalElements());
        response.put("totalPages", taskPage.getTotalPages());
        response.put("hasNext", taskPage.hasNext());
        response.put("hasPrevious", taskPage.hasPrevious());

    return new ResponseEntity<>(response, HttpStatus.OK);
}

/** Lists tasks visible to the caller, with configurable page and sort options. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTasks(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "DESC") String sortDir) {

    Sort sort = sortDir.equalsIgnoreCase("ASC")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();

    Pageable pageable = PageRequest.of(page, size, sort);

    Page<Task> taskPage = taskService.getAllTasks(pageable);

    List<taskResponse> tasks = taskPage.getContent()
            .stream()
            .map(taskResponse::from)
        .toList();
            

    Map<String, Object> response = new HashMap<>();
    response.put("tasks", tasks);
    response.put("currentPage", taskPage.getNumber());
    response.put("totalItems", taskPage.getTotalElements());
    response.put("totalPages", taskPage.getTotalPages());
    response.put("hasNext", taskPage.hasNext());
    response.put("hasPrevious", taskPage.hasPrevious());

    return new ResponseEntity<>(response, HttpStatus.OK);
}

/** Returns the task only if the caller owns it or is an administrator. */
    @GetMapping ("/{id}")
    public Task getTaskByID(@PathVariable Long id) {
        return taskService.getTaskByID(id);
    }

    /** Returns chronological task snapshots, including history after deletion. */
    @GetMapping("/{id}/history")
    public List<TaskAuditResponse> getTaskHistory(@PathVariable Long id) {
        return taskService.getTaskHistory(id);
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<Task>getTaskByPriority(@PathVariable String priority) {
        return taskService.getTaskByPriority(priority);
    }


    @GetMapping("/completed")
    public List<Task> getCompletedTasks() {
        return taskService.getCompletedTasks();
}

  @GetMapping("/search-title")
    public List<Task> searchTasksByTitle(@RequestParam String title) {
        return taskService.searchTasksByTitle(title);
}

    @PostMapping
    public taskResponse createTask(@Valid @RequestBody TaskRequest task) {
        return taskService.createTask(task);
}

    @PutMapping("/{id}")
    public ResponseEntity<taskResponse> updateTask(
    
        @PathVariable Long id,
         @Valid @RequestBody TaskRequest updatedTask) {
    return ResponseEntity.ok(taskService.updateTask(id, updatedTask));
}

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
    taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
}

}
