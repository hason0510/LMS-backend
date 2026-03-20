package com.example.backend.service.impl;

import com.example.backend.dto.request.CategoryRequest;
import com.example.backend.dto.response.CategoryResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.entity.Category;
import com.example.backend.entity.Course;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.CourseRepository;
import com.example.backend.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, CourseRepository courseRepository) {
        this.categoryRepository = categoryRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public CategoryResponse createCategory(CategoryRequest request) {
        Category category = new Category();
        category.setTitle(request.getTitle());
        category.setDescription(request.getDescription());
        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse getCategoryById(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return mapToResponse(category);
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse updateCategory(Integer id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        category.setTitle(request.getTitle());
        category.setDescription(request.getDescription());
        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    public void deleteCategory(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        for (Course course : category.getCourses()) {
            course.setCategory(null);
        }
        categoryRepository.delete(category); // soft delete
    }

    private CategoryResponse mapToResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getTitle(),
                category.getDescription()
        );
    }

    @Override
    public PageResponse<CategoryResponse> getCategoryPage(Pageable pageable) {
        Page<Category> categoryPage = categoryRepository.findAll(pageable);
        Page<CategoryResponse> categoryResponsePage = categoryPage.map(this::convertCategoryToDTO);
        PageResponse<CategoryResponse> pageDTO = new PageResponse<>(
                categoryResponsePage.getNumber() + 1, 
                categoryResponsePage.getTotalPages(),
                (int) categoryResponsePage.getTotalElements(),
                categoryResponsePage.getContent()
        );
        return pageDTO;
    }

    @Override
    public CategoryResponse convertCategoryToDTO(Category category) {
        CategoryResponse responseDTO = new CategoryResponse();
        responseDTO.setId(category.getId());
        responseDTO.setTitle(category.getTitle());
        responseDTO.setDescription(category.getDescription());
        return responseDTO;
    }
}
