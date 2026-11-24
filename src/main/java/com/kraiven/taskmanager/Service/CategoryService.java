
package com.kraiven.taskmanager.Service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kraiven.taskmanager.dto.CategoryRequest;
import com.kraiven.taskmanager.entity.Category;
import com.kraiven.taskmanager.exception.ResourceConflictException;
import com.kraiven.taskmanager.exception.ResourceNotFoundException;
import com.kraiven.taskmanager.repository.CategoryRepository;
import com.kraiven.taskmanager.repository.taskRepository;

/** Applies category rules before delegating persistence to repositories. */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final taskRepository taskRepository;

    public CategoryService(CategoryRepository categoryRepository, taskRepository taskRepository) {
        this.categoryRepository = categoryRepository;
        this.taskRepository = taskRepository;
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new ResourceConflictException("A category with this name already exists");
        }
        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());
        return categoryRepository.save(category);
    }

    /** Updates a category while preserving the unique-name constraint. */
    public Category update(Long id, CategoryRequest request) {
        Category category = findById(id);
        if (categoryRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new ResourceConflictException("A category with this name already exists");
        }
        category.setName(request.name());
        category.setDescription(request.description());
        return categoryRepository.save(category);
    }

    /** Protects task references by refusing to delete an in-use category. */
    public void delete(Long id) {
        Category category = findById(id);
        if (taskRepository.existsByCategory_Id(id)) {
            throw new ResourceConflictException(
                    "Cannot delete a category while tasks are assigned to it");
        }
        categoryRepository.delete(category);
    }
}