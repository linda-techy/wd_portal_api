package com.wd.api.service;

import com.wd.api.dto.DocumentResponse;
import com.wd.api.model.Document;
import com.wd.api.model.DocumentCategory;
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
    private final CustomerNotificationFacade customerNotificationFacade;

    public DocumentService(DocumentRepository documentRepository,
            DocumentCategoryRepository categoryRepository,
            PortalUserRepository portalUserRepository,
            FileStorageService fileStorageService,
            CustomerNotificationFacade customerNotificationFacade) {
        this.documentRepository = documentRepository;
        this.categoryRepository = categoryRepository;
        this.portalUserRepository = portalUserRepository;
        this.fileStorageService = fileStorageService;
        this.customerNotificationFacade = customerNotificationFacade;
    }

    /**
     * Get all document categories sorted by display order.
     * Used for category selection dropdowns in document upload flow.
     * 
     * @param referenceType Optional filter by reference type (LEAD, PROJECT).
     *                      If null, returns all categories.
     */
    public List<com.wd.api.dto.ProjectModuleDtos.DocumentCategoryDto> getAllCategories(String referenceType) {
        List<DocumentCategory> categories;

        if (referenceType != null && !referenceType.isEmpty()) {
            // Filter by reference type (includes BOTH and null for backward compatibility)
            categories = categoryRepository.findByReferenceTypeOrBoth(referenceType.toUpperCase());
        } else {
            // Return all categories
            categories = categoryRepository.findAllByOrderByDisplayOrderAsc();
        }

        return categories.stream()
                .sorted((a, b) -> {
                    int orderA = a.getDisplayOrder() != null ? a.getDisplayOrder() : 100;
                    int orderB = b.getDisplayOrder() != null ? b.getDisplayOrder() : 100;
                    return Integer.compare(orderA, orderB);
                })
                .map(c -> new com.wd.api.dto.ProjectModuleDtos.DocumentCategoryDto(
                        c.getId(), c.getName(), c.getDescription(), c.getDisplayOrder()))
                .collect(Collectors.toList());
    }

    /**
     * Get all document categories (backward compatibility - returns all).
     */
    public List<com.wd.api.dto.ProjectModuleDtos.DocumentCategoryDto> getAllCategories() {
        return getAllCategories(null);
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
        document.setUploadedByType("PORTAL");

        document = documentRepository.save(document);

        // Notify all project customer members when a document is uploaded to a project
        if ("PROJECT".equals(document.getReferenceType())) {
            String fileName = document.getFilename() != null ? document.getFilename() : "a document";
            customerNotificationFacade.notifyAll(
                    document.getReferenceId(),
                    "New Document Added",
                    "A new document (" + fileName + ") has been added to your project.",
                    "DOCUMENT",
                    document.getId()
            );
        }

        return toResponse(document);
    }

    public List<DocumentResponse> getDocuments(Long referenceId, String referenceType) {
        return documentRepository
                .findByReferenceIdAndReferenceTypeAndIsActiveTrue(referenceId, referenceType.toUpperCase())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Walk the project's storage subtree and create project_documents rows
     * for any files on disk that don't have a corresponding active row.
     * Useful when historical uploads succeeded at the storage layer but
     * failed at the DB layer (e.g. older bug where uploaded_by_type or FK
     * violated). The category for each row is inferred from the
     * subdirectory name (mapping back via DocumentCategory.name); files
     * outside any known-category subdir are skipped.
     */
    @Transactional
    public java.util.Map<String, Object> reconcileProjectStorage(Long projectId) {
        java.util.List<String> created = new java.util.ArrayList<>();
        java.util.List<String> alreadyHadRow = new java.util.ArrayList<>();
        java.util.List<String> skippedNoCategory = new java.util.ArrayList<>();

        java.nio.file.Path projectRoot = fileStorageService.getStorageRoot()
                .resolve("projects").resolve(String.valueOf(projectId));
        if (!java.nio.file.Files.isDirectory(projectRoot)) {
            return java.util.Map.of(
                    "created", created,
                    "alreadyHadRow", alreadyHadRow,
                    "skippedNoCategory", skippedNoCategory,
                    "note", "no storage dir for project " + projectId);
        }

        // Build category lookup by sanitized folder name → category
        java.util.Map<String, DocumentCategory> categoryByFolder = new java.util.HashMap<>();
        for (DocumentCategory c : categoryRepository.findAll()) {
            categoryByFolder.put(sanitizeFolderName(c.getName()), c);
        }

        // Existing rows for this project (active OR inactive — we want to
        // know what file_paths are already tracked so we don't duplicate)
        java.util.Set<String> existingFilePaths = documentRepository
                .findByReferenceIdAndReferenceType(projectId, "PROJECT")
                .stream().map(Document::getFilePath).collect(java.util.stream.Collectors.toSet());

        try (java.util.stream.Stream<java.nio.file.Path> walk = java.nio.file.Files.walk(projectRoot)) {
            walk.filter(java.nio.file.Files::isRegularFile).forEach(file -> {
                java.nio.file.Path categoryFolder = file.getParent();
                if (categoryFolder == null) return;
                String folderName = categoryFolder.getFileName().toString();
                DocumentCategory category = categoryByFolder.get(folderName);
                if (category == null) {
                    skippedNoCategory.add(file.toString());
                    return;
                }
                // Build canonical relative file_path: projects/<id>/<folder>/<file>
                String relPath = "projects/" + projectId + "/" + folderName + "/"
                        + file.getFileName().toString();
                if (existingFilePaths.contains(relPath)) {
                    alreadyHadRow.add(relPath);
                    return;
                }
                Document doc = new Document();
                doc.setReferenceId(projectId);
                doc.setReferenceType("PROJECT");
                doc.setFilename(file.getFileName().toString());
                doc.setFilePath(relPath);
                try {
                    doc.setFileSize(java.nio.file.Files.size(file));
                } catch (java.io.IOException ignored) {
                    doc.setFileSize(0L);
                }
                doc.setFileType(probeContentType(file));
                doc.setDescription("Recovered from storage");
                doc.setCategory(category);
                doc.setIsActive(true);
                doc.setUploadedByType("PORTAL");
                documentRepository.save(doc);
                created.add(relPath);
            });
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to walk storage for project " + projectId, e);
        }

        return java.util.Map.of(
                "createdCount", created.size(),
                "created", created,
                "alreadyHadRowCount", alreadyHadRow.size(),
                "alreadyHadRow", alreadyHadRow,
                "skippedNoCategoryCount", skippedNoCategory.size(),
                "skippedNoCategory", skippedNoCategory);
    }

    private String probeContentType(java.nio.file.Path file) {
        try {
            String ct = java.nio.file.Files.probeContentType(file);
            if (ct != null) return ct;
        } catch (java.io.IOException ignored) {}
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".txt")) return "text/plain";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
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
            projectDoc.setUploadedByType(leadDoc.getUploadedByType() != null ? leadDoc.getUploadedByType() : "PORTAL");

            documentRepository.save(projectDoc);
        }
    }

    private String sanitizeFolderName(String name) {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
