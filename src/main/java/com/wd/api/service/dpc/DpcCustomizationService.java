package com.wd.api.service.dpc;

import com.wd.api.dto.dpc.AddCustomizationFromCatalogRequest;
import com.wd.api.dto.dpc.UpsertCustomizationLineRequest;
import com.wd.api.model.BoqItem;
import com.wd.api.model.DpcCustomizationCatalogItem;
import com.wd.api.model.DpcCustomizationLine;
import com.wd.api.model.DpcDocument;
import com.wd.api.model.enums.DpcCustomizationSource;
import com.wd.api.model.enums.ItemKind;
import com.wd.api.repository.BoqItemRepository;
import com.wd.api.repository.DpcCustomizationCatalogRepository;
import com.wd.api.repository.DpcCustomizationLineRepository;
import com.wd.api.repository.DpcDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the customization-lines page on a DPC document.
 *
 * Two row sources are tracked:
 *   AUTO_FROM_BOQ_ADDON — generated from ADDON BoQ items; refreshed on demand.
 *   MANUAL              — added by editors; preserved across refresh.
 *
 * AUTO rows cannot be hand-edited (amount tracks the BoQ) or hand-deleted —
 * the source of truth is the BoQ ADDON itself.  MANUAL rows are fully editable
 * until the parent DPC is ISSUED, after which the document is locked.
 */
@Service
@Transactional
public class DpcCustomizationService {

    private static final Logger log = LoggerFactory.getLogger(DpcCustomizationService.class);

    /** Customization line title is bounded at 255 chars by the DB column. */
    private static final int MAX_TITLE_LENGTH = 255;

    private final DpcDocumentRepository dpcDocumentRepository;
    private final DpcCustomizationLineRepository dpcCustomizationLineRepository;
    private final BoqItemRepository boqItemRepository;
    private final DpcCustomizationCatalogRepository dpcCustomizationCatalogRepository;
    private final DpcCustomizationCatalogService dpcCustomizationCatalogService;

    public DpcCustomizationService(DpcDocumentRepository dpcDocumentRepository,
                                   DpcCustomizationLineRepository dpcCustomizationLineRepository,
                                   BoqItemRepository boqItemRepository,
                                   DpcCustomizationCatalogRepository dpcCustomizationCatalogRepository,
                                   DpcCustomizationCatalogService dpcCustomizationCatalogService) {
        this.dpcDocumentRepository = dpcDocumentRepository;
        this.dpcCustomizationLineRepository = dpcCustomizationLineRepository;
        this.boqItemRepository = boqItemRepository;
        this.dpcCustomizationCatalogRepository = dpcCustomizationCatalogRepository;
        this.dpcCustomizationCatalogService = dpcCustomizationCatalogService;
    }

    /**
     * Replace the AUTO customization rows of {@code dpc} with a fresh snapshot
     * of the current ADDON BoQ items.  MANUAL rows are left untouched.
     */
    public void autoPopulateFromBoq(DpcDocument dpc) {
        if (dpc == null || dpc.getId() == null) {
            throw new IllegalArgumentException("DPC document must be persisted before auto-populating customizations");
        }
        Long projectId = dpc.getProject() != null ? dpc.getProject().getId() : null;
        Long boqDocumentId = dpc.getBoqDocument() != null ? dpc.getBoqDocument().getId() : null;
        if (projectId == null || boqDocumentId == null) {
            throw new IllegalStateException("DPC must be linked to a project and BoQ before auto-populate");
        }

        // Wipe existing AUTO rows for this DPC; MANUAL rows are intentionally left.
        dpcCustomizationLineRepository.deleteByDpcDocumentIdAndSource(
                dpc.getId(), DpcCustomizationSource.AUTO_FROM_BOQ_ADDON);

        List<BoqItem> addons = new ArrayList<>();
        for (BoqItem item : boqItemRepository.findByProjectIdWithAssociations(projectId)) {
            if (item.getBoqDocument() == null) continue;
            if (!boqDocumentId.equals(item.getBoqDocument().getId())) continue;
            if (!Boolean.TRUE.equals(item.getIsActive())) continue;
            if (ItemKind.ADDON != item.getItemKind()) continue;
            addons.add(item);
        }

        List<DpcCustomizationLine> newLines = new ArrayList<>(addons.size());
        int order = 0;
        for (BoqItem item : addons) {
            DpcCustomizationLine line = new DpcCustomizationLine();
            line.setDpcDocument(dpc);
            line.setSource(DpcCustomizationSource.AUTO_FROM_BOQ_ADDON);
            line.setBoqItemId(item.getId());
            line.setTitle(truncateTitle(item.getDescription()));
            line.setDescription(item.getSpecifications());
            line.setAmount(item.getTotalAmount() != null ? item.getTotalAmount() : BigDecimal.ZERO);
            line.setDisplayOrder(order++);
            newLines.add(line);
        }

        dpcCustomizationLineRepository.saveAll(newLines);
        log.info("DPC {} auto-populated {} AUTO customization rows from BoQ {}",
                dpc.getId(), newLines.size(), boqDocumentId);
    }

    /**
     * Append a MANUAL customization line to the given DPC.
     *
     * @throws IllegalStateException when the DPC is locked (ISSUED).
     */
    public DpcCustomizationLine addManualLine(Long dpcDocumentId, UpsertCustomizationLineRequest request) {
        DpcDocument dpc = loadDpc(dpcDocumentId);
        assertNotLocked(dpc);

        Integer displayOrder = request.displayOrder();
        if (displayOrder == null) {
            displayOrder = nextDisplayOrder(dpcDocumentId);
        }

        DpcCustomizationLine line = new DpcCustomizationLine();
        line.setDpcDocument(dpc);
        line.setSource(DpcCustomizationSource.MANUAL);
        line.setTitle(truncateTitle(request.title()));
        line.setDescription(request.description());
        line.setAmount(request.amount() != null ? request.amount() : BigDecimal.ZERO);
        line.setDisplayOrder(displayOrder);
        return dpcCustomizationLineRepository.save(line);
    }

    /**
     * Append a MANUAL customization line to the given DPC, sourced from the
     * master DPC customization catalog.
     *
     * <p>The catalog row's {@code defaultAmount} seeds the line amount, but
     * the caller may pass {@code amountOverride} to capture a project-specific
     * tweak. Increments the catalog row's {@code timesUsed} counter.
     *
     * @throws IllegalArgumentException when the DPC or catalog item is missing
     * @throws IllegalStateException when the DPC is locked (ISSUED)
     */
    public DpcCustomizationLine addCustomizationFromCatalog(Long dpcDocumentId, AddCustomizationFromCatalogRequest req) {
        DpcDocument dpc = loadDpc(dpcDocumentId);
        assertNotLocked(dpc);

        DpcCustomizationCatalogItem catalogItem = dpcCustomizationCatalogRepository.findById(req.catalogItemId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "DPC customization catalog item not found: " + req.catalogItemId()));

        BigDecimal amount = req.amountOverride() != null
                ? req.amountOverride()
                : (catalogItem.getDefaultAmount() != null ? catalogItem.getDefaultAmount() : BigDecimal.ZERO);

        DpcCustomizationLine line = new DpcCustomizationLine();
        line.setDpcDocument(dpc);
        line.setSource(DpcCustomizationSource.MANUAL);
        line.setTitle(truncateTitle(catalogItem.getName()));
        line.setDescription(catalogItem.getDescription());
        line.setAmount(amount);
        line.setDisplayOrder(nextDisplayOrder(dpcDocumentId));
        line.setCatalogItem(catalogItem);

        DpcCustomizationLine saved = dpcCustomizationLineRepository.save(line);

        // Bump usage count on the catalog row.
        dpcCustomizationCatalogService.incrementUsageCount(catalogItem.getId());

        log.info("Added MANUAL customization line id={} to DPC {} from catalog id={}",
                saved.getId(), dpcDocumentId, catalogItem.getId());
        return saved;
    }

    /**
     * Patch an existing line.  AUTO lines accept only title/description/order
     * edits — their amount tracks the source BoQ item.
     *
     * @throws IllegalStateException when the parent DPC is locked.
     */
    public DpcCustomizationLine updateLine(Long lineId, UpsertCustomizationLineRequest request) {
        DpcCustomizationLine line = dpcCustomizationLineRepository.findById(lineId)
                .orElseThrow(() -> new IllegalArgumentException("Customization line not found: " + lineId));

        DpcDocument dpc = line.getDpcDocument();
        assertNotLocked(dpc);

        if (request.title() != null) line.setTitle(truncateTitle(request.title()));
        if (request.description() != null) line.setDescription(request.description());
        if (request.displayOrder() != null) line.setDisplayOrder(request.displayOrder());

        // AUTO rows: amount tracks BoQ — silently ignore amount changes.
        if (DpcCustomizationSource.MANUAL == line.getSource() && request.amount() != null) {
            line.setAmount(request.amount());
        }

        return dpcCustomizationLineRepository.save(line);
    }

    /**
     * Soft-delete a customization line.  AUTO rows refuse direct deletion —
     * delete the source BoQ ADDON and re-run auto-populate instead.
     */
    public void deleteLine(Long lineId) {
        DpcCustomizationLine line = dpcCustomizationLineRepository.findById(lineId)
                .orElseThrow(() -> new IllegalArgumentException("Customization line not found: " + lineId));

        if (DpcCustomizationSource.AUTO_FROM_BOQ_ADDON == line.getSource()) {
            throw new IllegalStateException(
                    "Auto lines refresh from BoQ - delete the BoQ ADDON instead");
        }

        assertNotLocked(line.getDpcDocument());

        // Soft delete is wired via @SQLDelete on the entity.
        dpcCustomizationLineRepository.delete(line);
    }

    // ---- Private Helpers ----

    private DpcDocument loadDpc(Long id) {
        return dpcDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DPC document not found: " + id));
    }

    private void assertNotLocked(DpcDocument dpc) {
        if (dpc != null && dpc.isLocked()) {
            throw new IllegalStateException("DPC " + dpc.getId() + " is ISSUED and cannot be modified");
        }
    }

    private int nextDisplayOrder(Long dpcDocumentId) {
        List<DpcCustomizationLine> existing =
                dpcCustomizationLineRepository.findByDpcDocumentIdOrderByDisplayOrderAsc(dpcDocumentId);
        int max = -1;
        for (DpcCustomizationLine l : existing) {
            if (l.getDisplayOrder() != null && l.getDisplayOrder() > max) {
                max = l.getDisplayOrder();
            }
        }
        return max + 1;
    }

    private String truncateTitle(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        return trimmed.length() > MAX_TITLE_LENGTH ? trimmed.substring(0, MAX_TITLE_LENGTH) : trimmed;
    }
}
