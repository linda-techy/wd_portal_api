package com.wd.api.service.dpc;

import com.wd.api.dto.dpc.DpcDocumentDto;
import com.wd.api.dto.dpc.DpcMasterCostSummaryDto;
import com.wd.api.model.Document;
import com.wd.api.model.DocumentCategory;
import com.wd.api.repository.DocumentCategoryRepository;
import com.wd.api.repository.DocumentRepository;
import com.wd.api.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Issue-and-persist orchestrator for DPC documents.
 *
 * <p>Combines three steps into one transactional operation:
 * <ol>
 *   <li>Render the PDF via {@link DpcRenderService};</li>
 *   <li>Persist the bytes to disk under {@code storage/dpc/{projectId}/...}
 *       and create a matching {@code project_documents} row (category =
 *       "Detailed Project Costing");</li>
 *   <li>Stamp the DPC as ISSUED via {@link DpcDocumentService#issue}.</li>
 * </ol>
 *
 * <p>The DPC PDF is treated as a project asset (referenceType = PROJECT,
 * referenceId = projectId) so it shows up alongside other project documents
 * in the unified document feed without a special-case loader.
 */
@Service
@Transactional
public class DpcIssueService {

    private static final Logger log = LoggerFactory.getLogger(DpcIssueService.class);

    private static final String DPC_CATEGORY_NAME = "Detailed Project Costing";
    private static final DateTimeFormatter STAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final DpcRenderService dpcRenderService;
    private final DpcDocumentService dpcDocumentService;
    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;
    private final DocumentCategoryRepository documentCategoryRepository;

    public DpcIssueService(DpcRenderService dpcRenderService,
                           DpcDocumentService dpcDocumentService,
                           FileStorageService fileStorageService,
                           DocumentRepository documentRepository,
                           DocumentCategoryRepository documentCategoryRepository) {
        this.dpcRenderService = dpcRenderService;
        this.dpcDocumentService = dpcDocumentService;
        this.fileStorageService = fileStorageService;
        this.documentRepository = documentRepository;
        this.documentCategoryRepository = documentCategoryRepository;
    }

    /**
     * Render, persist, and issue the DPC document with the given id.
     *
     * @param dpcDocumentId the DPC primary key
     * @param currentUserId the portal user performing the issue (audit only)
     * @return the updated dto (status = ISSUED, issuedPdfDocumentId set)
     */
    public DpcDocumentDto issueAndPersist(Long dpcDocumentId, Long currentUserId) {
        // 1. Load to get projectId and revision; fail fast if not found.
        DpcDocumentDto dto = dpcDocumentService.getById(dpcDocumentId);
        if (dto.projectId() == null) {
            throw new IllegalStateException("DPC " + dpcDocumentId + " has no project — cannot issue");
        }

        // 1b. Refuse to issue a customer-facing artifact whose customized total
        //     rolls up to zero — the resulting PDF would print "INR 0" across
        //     every scope. Always indicates the BoQ items are not mapped to
        //     scope categories or the project has no items at all. The fix is
        //     in the BoQ data, not in the issued document.
        DpcMasterCostSummaryDto summary = dto.masterCostSummary();
        BigDecimal customized = summary != null ? summary.totalCustomized() : null;
        if (customized == null || customized.signum() <= 0) {
            throw new IllegalStateException(
                    "DPC " + dpcDocumentId + " has zero customized total — verify that the "
                            + "BoQ has approved items mapped to scope categories before issuing.");
        }

        // 2. Render PDF.
        byte[] pdfBytes = dpcRenderService.renderPdf(dpcDocumentId);
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalStateException("Renderer produced empty PDF for DPC " + dpcDocumentId);
        }

        // 3. Resolve / create the DPC document category.
        DocumentCategory category = documentCategoryRepository
                .findByName(DPC_CATEGORY_NAME)
                .orElseGet(() -> {
                    DocumentCategory c = new DocumentCategory(DPC_CATEGORY_NAME,
                            "PDFs of issued Detailed Project Costing documents", 95);
                    c.setReferenceType("PROJECT");
                    return documentCategoryRepository.save(c);
                });

        // 4. Write bytes to disk under storage/dpc/{projectId}/.
        int rev = dto.revisionNumber() != null ? dto.revisionNumber() : 1;
        String stamp = LocalDateTime.now().format(STAMP_FMT);
        String fileName = String.format("dpc-rev%02d-%s.pdf", rev, stamp);
        String relPath = "dpc/" + dto.projectId() + "/" + fileName;

        Path absoluteTarget = fileStorageService.getStorageRoot().resolve(relPath);
        try {
            Files.createDirectories(absoluteTarget.getParent());
            Files.write(absoluteTarget, pdfBytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write DPC PDF to " + absoluteTarget, e);
        }

        // 5. Persist a Document row pointing at the file.
        Document doc = new Document();
        doc.setReferenceId(dto.projectId());
        doc.setReferenceType("PROJECT");
        doc.setFilename(String.format("Detailed_Project_Costing_REV_%02d.pdf", rev));
        doc.setFilePath(relPath);
        doc.setFileSize((long) pdfBytes.length);
        doc.setFileType("application/pdf");
        doc.setDescription("Issued DPC revision " + rev);
        doc.setCategory(category);
        doc.setIsActive(true);
        doc.setUploadedByType("PORTAL");
        Document saved = documentRepository.save(doc);

        // 6. Stamp the DPC as ISSUED.
        DpcDocumentDto updated = dpcDocumentService.issue(
                dpcDocumentId, currentUserId, pdfBytes, saved.getId());

        log.info("Issued DPC {} (rev {}) for project {} — PDF {} bytes saved as Document {} at {}",
                dpcDocumentId, rev, dto.projectId(), pdfBytes.length, saved.getId(), relPath);
        return updated;
    }
}
