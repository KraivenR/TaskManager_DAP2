package com.kraiven.taskmanager.Service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.kraiven.taskmanager.dto.TaskRequest;
import com.kraiven.taskmanager.entity.Category;
import com.kraiven.taskmanager.entity.Task;
import com.kraiven.taskmanager.entity.TaskAuditEntry;
import com.kraiven.taskmanager.entity.TaskAuditEntry.Action;
import com.kraiven.taskmanager.entity.User;
import com.kraiven.taskmanager.exception.TaskNotFoundException;
import com.kraiven.taskmanager.repository.TaskAuditRepository;
import com.kraiven.taskmanager.repository.UserRepository;
import com.kraiven.taskmanager.repository.taskRepository;

import jakarta.transaction.Transactional;

/** Implements task ownership, searching, CRUD, and audit-history rules. */
@Service 
@Transactional 
public class taskService {

    private final taskRepository taskRepository;
    private final TaskAuditRepository taskAuditRepository;
    private final CategoryService categoryService;
    private final UserRepository userRepository;

    public taskService(
            taskRepository taskRepository,
            TaskAuditRepository taskAuditRepository,
            CategoryService categoryService,
            UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.taskAuditRepository = taskAuditRepository;
        this.categoryService = categoryService;
        this.userRepository = userRepository;
    }

    public List<Task> getAllTasks() {
       return isAdmin() ? taskRepository.findAll() : taskRepository.findByUserUsername(currentUsername());
    }

     public Page<Task> getAllTasks(Pageable pageable) {
       return isAdmin()
               ? taskRepository.findAll(pageable)
               : taskRepository.findByUserUsername(currentUsername(), pageable);
    }

     public Task getTaskByID(Long id) {
       return findAccessibleTask(id);
    }

    public ResponseEntity<Task>getTaskByPriority(@PathVariable String priority) {
       return (isAdmin()
               ? taskRepository.findByPriority(priority)
               : taskRepository.findByPriorityAndUserUsername(priority, currentUsername()))
                       .map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }
    
    public List<Task> getCompletedTasks() {
       return isAdmin()
               ? taskRepository.findByStatus(Task.TaskStatus.COMPLETED)
               : taskRepository.findByStatusAndUserUsername(
                       Task.TaskStatus.COMPLETED, currentUsername());
}

  public Page<Task> getCompletedTasks(Pageable pageable) {
       return isAdmin()
               ? taskRepository.findByStatus(Task.TaskStatus.COMPLETED, pageable)
               : taskRepository.findByStatusAndUserUsername(
                       Task.TaskStatus.COMPLETED, currentUsername(), pageable);
}

 public List<Task> searchTasksByTitle(@RequestParam String title) {
       return isAdmin()
               ? taskRepository.findByTitleContainingIgnoreCase(title)
               : taskRepository.findByTitleContainingIgnoreCaseAndUserUsername(
                       title, currentUsername());
}

 public Page<Task> searchTasksByTitle(@RequestParam String title, Pageable pageable) {
       return isAdmin()
               ? taskRepository.findByTitleContainingIgnoreCase(title, pageable)
               : taskRepository.findByTitleContainingIgnoreCaseAndUserUsername(
                       title, currentUsername(), pageable);
}

public Page<Task> searchTasksByStatus(Task.TaskStatus status, Pageable pageable) {
    return isAdmin()
           ? taskRepository.findByStatus(status, pageable)
           : taskRepository.findByStatusAndUserUsername(status, currentUsername(), pageable);
}

public Page<Task> searchTasksByTitleAndStatus(
        String title, Task.TaskStatus status, Pageable pageable) {
    return isAdmin()
           ? taskRepository.findByTitleContainingIgnoreCaseAndStatus(title, status, pageable)
           : taskRepository.findByTitleContainingIgnoreCaseAndStatusAndUserUsername(
                   title, status, currentUsername(), pageable);
}

public Page<Task> getTasksForUser(String username, Pageable pageable) {
    if (!isAdmin() && !currentUsername().equals(username)) {
       throw new AccessDeniedException("You can only view your own tasks");
    }
    return taskRepository.findByUserUsername(username, pageable);
}

/** Associates a new task with the caller and records its creation snapshot. */
public taskResponse createTask(TaskRequest task) {
    Task entityTask = Task.builder()
            .title(task.title())
            .description(task.description())
            .priority(task.priority())
            .category(findCategory(task.categoryId()))
            .deadline(task.deadline())
            .status(task.status() != null ? task.status() : Task.TaskStatus.TODO)
            .user(findCurrentUser())
            .build();

    Task savedTask = taskRepository.save(entityTask);
    recordAudit(savedTask, Action.CREATED);

    return taskResponse.from(savedTask);
}

public taskResponse updateTask(Long id, TaskRequest updatedTask) {
        Task task = findAccessibleTask(id);

        task.setTitle(updatedTask.title());
        task.setDescription(updatedTask.description());
        task.setStatus(updatedTask.status());
        task.setPriority(updatedTask.priority());
        task.setCategory(findCategory(updatedTask.categoryId()));
        task.setDeadline(updatedTask.deadline());
        Task savedTask = taskRepository.save(task);
        recordAudit(savedTask, Action.UPDATED);
    return taskResponse.from(savedTask);
}

/** Records the final task snapshot before removing the task itself. */
public void deleteTask(Long id) {
    Task task = findAccessibleTask(id);
    recordAudit(task, Action.DELETED);
    taskRepository.delete(task);
}

/** Returns history to admins or only to the task's owner. */
public List<TaskAuditResponse> getTaskHistory(Long taskId) {
    List<TaskAuditEntry> entries = isAdmin()
            ? taskAuditRepository.findByTaskIdOrderByOccurredAtAscIdAsc(taskId)
            : taskAuditRepository.findByTaskIdAndTaskOwnerUsernameOrderByOccurredAtAscIdAsc(
                    taskId, currentUsername());
    return entries.stream().map(TaskAuditResponse::from).toList();
}

/** Copies current task values so later edits/deletion do not erase history. */
private void recordAudit(Task task, Action action) {
    String ownerUsername = task.getUser() == null ? null : task.getUser().getUsername();
    String categoryName = task.getCategory() == null ? null : task.getCategory().getName();
    taskAuditRepository.save(new TaskAuditEntry(
            task.getId(),
            ownerUsername,
            currentUsername(),
            action,
            Instant.now(),
            task.getTitle(),
            task.getDescription(),
            task.getPriority(),
            task.getDeadline(),
            task.getStatus(),
            categoryName));
}

private Category findCategory(Long categoryId) {
    return categoryId == null ? null : categoryService.findById(categoryId);
}

/** Hides another user's task as not found to avoid revealing its existence. */
private Task findAccessibleTask(Long id) {
    Task task = taskRepository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));
    if (!isAdmin()
            && (task.getUser() == null || !task.getUser().getUsername().equals(currentUsername()))) {
        throw new TaskNotFoundException(id);
    }
    return task;
}

private User findCurrentUser() {
    String username = currentUsername();
    return userRepository.findByUsername(username)
            .orElseThrow(() -> new AccessDeniedException("Authenticated user no longer exists"));
}

private String currentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
        throw new AccessDeniedException("Authentication is required");
    }
    return authentication.getName();
}

private boolean isAdmin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken)
            && authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
}
}
