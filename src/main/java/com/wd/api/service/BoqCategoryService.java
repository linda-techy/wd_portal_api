package com.wd.api.service;

import com.wd.api.dto.BoqCategoryDto;
import com.wd.api.dto.CreateBoqCategoryRequest;
import com.wd.api.model.BoqCategory;
import com.wd.api.model.CustomerProject;
import com.wd.api.repository.BoqCategoryRepository;
import com.wd.api.repository.CustomerProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BoqCategoryService {

    private final BoqCategoryRepository categoryRepository;
    private final CustomerProjectRepository projectRepository;
    private final BoqAuditService auditService;

    public BoqCategoryService(BoqCategoryRepository categoryRepository,
                              CustomerProjectRepository projectRepository,
                              BoqAuditService auditService) {
        this.categoryRepository = categoryRepository;
        this.projectRepository = projectRepository;
        this.auditService = auditService;
    }

    public BoqCategoryDto createCategory(CreateBoqCategoryRequest request, Long userId) {
        CustomerProject project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        BoqCategory category = new BoqCategory();
        category.setProject(project);
        category.setName(request.name());
        category.setDescription(request.description());
        category.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        category.setIsActive(true);

        if (request.parentId() != null) {
            BoqCategory parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
            category.setParent(parent);
        }

        category = categoryRepository.save(category);

        auditService.logCreate("BOQ_CATEGORY", category.getId(), project.getId(), userId, category);

        int itemCount = categoryRepository.countItemsByCategory(category.getId());
        return BoqCategoryDto.fromEntity(category, itemCount);
    }

    @Transactional(readOnly = true)
    public List<BoqCategoryDto> getCategoriesByProject(Long projectId) {
        List<BoqCategory> categories = categoryRepository.findByProjectIdAndDeletedAtIsNullOrderByDisplayOrderAscNameAsc(projectId);

        return categories.stream()
                .map(cat -> {
                    int itemCount = categoryRepository.countItemsByCategory(cat.getId());
                    return BoqCategoryDto.fromEntity(cat, itemCount);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BoqCategoryDto> getTopLevelCategories(Long projectId) {
        List<BoqCategory> categories = categoryRepository.findTopLevelCategoriesByProject(projectId);

        return categories.stream()
                .map(cat -> {
                    int itemCount = categoryRepository.countItemsByCategory(cat.getId());
                    return BoqCategoryDto.fromEntity(cat, itemCount);
                })
                .collect(Collectors.toList());
    }

    public void softDeleteCategory(Long categoryId, Long userId) {
        BoqCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (category.isDeleted()) {
            throw new IllegalStateException("Category is already deleted");
        }

        // CRITICAL FIX: Enhanced validation with clear error messages
        int itemCount = categoryRepository.countItemsByCategory(categoryId);
        if (itemCount > 0) {
            throw new IllegalStateException(
                String.format("Cannot delete category '%s'. It contains %d active BOQ item(s). " +
                    "Please reassign or delete these items first.",
                    category.getName(), itemCount)
            );
        }
        
        // CRITICAL FIX: Check for subcategories
        long subcategoryCount = categoryRepository.countByParentIdAndIsActiveTrue(categoryId);
        if (subcategoryCount > 0) {
            throw new IllegalStateException(
                String.format("Cannot delete category '%s'. It has %d subcategory(ies). " +
                    "Please delete or reassign subcategories first.",
                    category.getName(), subcategoryCount)
            );
        }

        category.setDeletedAt(LocalDateTime.now());
        category.setDeletedByUserId(userId);
        category.setIsActive(false);

        categoryRepository.save(category);

        auditService.logDelete("BOQ_CATEGORY", categoryId, category.getProject().getId(), userId);
    }
}
