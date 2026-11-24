package com.kraiven.taskmanager.repository;

import com.kraiven.taskmanager.entity.Category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Provides standard category persistence and duplicate-name checks. */
@Repository 
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
