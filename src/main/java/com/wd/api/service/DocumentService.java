package com.wd.api.service;

import com.wd.api.dto.DocumentResponse;
import com.wd.api.model.ActivityFeed;
import com.wd.api.model.ActivityType;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.CustomerUser;
import com.wd.api.model.Document;
import com.wd.api.model.DocumentCategory;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.DocumentCategoryRepository;
import com.wd.api.repository.DocumentRepository;
import com.wd.api.repository.PortalUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentCategoryRepository categoryRepository;
    private final PortalUserRepository portalUserRepository;
    private final FileStorageService fileStorageService;

    public DocumentService(DocumentRepository documentRepository,
            DocumentCategoryRepository categoryRepository,
            PortalUserRepository portalUserRepository,
            FileStorageService fileStorageService) {
        this.documentRepository = documentRepository;
        this.categoryRepository = categoryRepository;
        this.portalUserRepository = portalUserRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public DocumentResponse uploadDocument(Long referenceId, String referenceType, MultipartFile file,
            Long categoryId, String description) {
        DocumentCategory category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));
        }

        // Store file with sanitized folder structure
        String subFolder = referenceType.toLowerCase() + "s/" + referenceId;
        if (category != null) {
            subFolder += "/" + sanitizeFolderName(category.getName());
        }

        String filePath = fileStorageService.storeFile(file, subFolder);

        Document document = new Document();
        document.setReferenceId(referenceId);
        document.setReferenceType(referenceType.toUpperCase());
        document.setFilename(file.getOriginalFilename());
        document.setFilePath(filePath);
        document.setFileSize(file.getSize());
        document.setFileType(file.getContentType());
        document.setDescription(description);
        document.setCategory(category);
        document.setIsActive(true);

        document = documentRepository.save(document);

        return toResponse(document);
    }

    public List<DocumentResponse> getDocuments(Long referenceId, String referenceType) {
        return documentRepository
                .findByReferenceIdAndReferenceTypeAndIsActiveTrue(referenceId, referenceType.toUpperCase())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        document.setIsActive(false);
        documentRepository.save(document);
    }

    public DocumentResponse toResponse(Document doc) {
        String uploaderName = "System";
        if (doc.getCreatedByUserId() != null) {
            uploaderName = portalUserRepository.findById(doc.getCreatedByUserId())
                    .map(u -> u.getFirstName() + " " + u.getLastName())
                    .orElse("Unknown User");
        }
        return new DocumentResponse(doc, uploaderName);
    }

    @Transactional
    public void migrateLeadDocumentsToProject(Long leadId, Long projectId) {
        List<Document> leadDocs = documentRepository.findByReferenceIdAndReferenceTypeAndIsActiveTrue(leadId, "LEAD");

        for (Document leadDoc : leadDocs) {
            Document projectDoc = new Document();
            projectDoc.setReferenceId(projectId);
            projectDoc.setReferenceType("PROJECT");
            projectDoc.setFilename(leadDoc.getFilename());
            projectDoc.setFilePath(leadDoc.getFilePath());
            projectDoc.setFileSize(leadDoc.getFileSize());
            projectDoc.setFileType(leadDoc.getFileType());
            projectDoc.setDescription(leadDoc.getDescription());
            projectDoc.setCategory(leadDoc.getCategory());
            projectDoc.setIsActive(true);

            documentRepository.save(projectDoc);
        }
    }

    private String sanitizeFolderName(String name) {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
