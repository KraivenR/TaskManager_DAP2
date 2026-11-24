package com.kraiven.taskmanager.entity;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.persistence.Column;

/** JPA entity for a task, including its owner, category, status, and deadline. */
@Entity
@Table(name="tasks")
@Data 
@NoArgsConstructor
@AllArgsConstructor
@Builder 


public class Task {

    public enum TaskStatus {
    
        COMPLETED,
        TODO,
        In_Progress
        
    }
    @Id
    @GeneratedValue (strategy =  GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Title is mandatory")
    @Size(min = 3, max=100, message = "Title must be between 3 and 100 characters")
    @Column(nullable = false)
    private String title;

    @Size(max=500, message = "Description can be up to 500 characters")
    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private String priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn ( name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    @ToString.Exclude
    private User user;

    @Column(nullable = false)
    private LocalDate deadline;

    @Builder.Default
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.TODO;

}
