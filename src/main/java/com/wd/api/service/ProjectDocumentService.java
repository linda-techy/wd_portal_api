package com.wd.api.service;

import com.wd.api.dto.ProjectModuleDtos.*;
import com.wd.api.model.*;
import com.wd.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectDocumentService {

    private final ProjectDocumentRepository documentRepository;
    private final CustomerProjectRepository projectRepository;
    private final DocumentCategoryRepository categoryRepository;
    private final PortalUserRepository userRepository;
    private final FileStorageService fileStorageService;

    public ProjectDocumentService(ProjectDocumentRepository documentRepository,
            CustomerProjectRepository projectRepository,
            DocumentCategoryRepository categoryRepository,
            PortalUserRepository userRepository,
            FileStorageService fileStorageService) {
        this.documentRepository = documentRepository;
        this.projectRepository = projectRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public ProjectDocumentDto uploadDocument(Long projectId, MultipartFile file,
            DocumentUploadRequest request, Long userId) {
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        DocumentCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        PortalUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Store file
        String sanitizedCategory = sanitizeFolderName(category.getName());
        String relativePath = "projects/" + projectId + "/" + sanitizedCategory;
        String filePath = fileStorageService.storeFile(file, relativePath);

        ProjectDocument document = new ProjectDocument();
        document.setProject(project);
        document.setCategory(category);
        document.setFilename(file.getOriginalFilename());
        document.setFilePath(filePath);
        document.setFileSize(file.getSize());
        document.setFileType(file.getContentType());
        document.setUploadedBy(user);
        document.setDescription(request.description());
        document.setIsActive(true);

        document = documentRepository.save(document);

        return toDto(document);
    }

    public List<ProjectDocumentDto> getProjectDocuments(Long projectId, Long categoryId) {
        List<ProjectDocument> documents;
        if (categoryId != null) {
            documents = documentRepository.findByProjectIdAndCategoryIdActiveOrNull(projectId, categoryId);
        } else {
            documents = documentRepository.findByProjectIdActiveOrNull(projectId);
        }
        return documents.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<DocumentCategoryDto> getAllCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(c -> new DocumentCategoryDto(c.getId(), c.getName(), c.getDescription(), c.getDisplayOrder()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        ProjectDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        document.setIsActive(false);
        documentRepository.save(document);
    }

    private ProjectDocumentDto toDto(ProjectDocument doc) {
        // Generate download URL - adjust base URL based on your configuration
        String downloadUrl = "/api/storage/" + doc.getFilePath();

        return new ProjectDocumentDto(
                doc.getId(),
                doc.getProject().getId(),
                doc.getCategory().getId(),
                doc.getCategory().getName(),
                doc.getFilename(),
                doc.getFilePath(),
                downloadUrl,
                doc.getFileSize(),
                doc.getFileType(),
                doc.getUploadedBy().getId(),
                doc.getUploadedBy().getFirstName() + " " + doc.getUploadedBy().getLastName(),
                doc.getUploadDate(),
                doc.getDescription(),
                doc.getVersion(),
                doc.getIsActive());
    }

    private String sanitizeFolderName(String name) {
        // Replace invalid characters with underscore
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
