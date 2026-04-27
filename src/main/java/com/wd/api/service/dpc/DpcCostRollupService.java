package com.wd.api.service.dpc;

import com.wd.api.dto.dpc.DpcCostRollupDto;
import com.wd.api.dto.dpc.DpcMasterCostSummaryDto;
import com.wd.api.model.BoqCategory;
import com.wd.api.model.BoqDocument;
import com.wd.api.model.BoqItem;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.DpcScopeTemplate;
import com.wd.api.model.enums.ItemKind;
import com.wd.api.repository.BoqDocumentRepository;
import com.wd.api.repository.BoqItemRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.DpcScopeTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Computes the per-scope cost rollup for a DPC document.
 *
 * DPC stores no cost numbers of its own — every figure is derived live from
 * the project's APPROVED BoQ items mapped onto the scope-template library
 * via the template's {@code boqCategoryPatterns} (case-insensitive substring
 * match against {@link BoqCategory#getName()}).
 *
 * Items whose category does not match any template pattern are routed to the
 * ELEVATION scope per spec — ELEVATION is the catch-all bucket so that no
 * BoQ rupee disappears from the customer-facing summary.
 */
@Service
@Transactional(readOnly = true)
public class DpcCostRollupService {

    private static final Logger log = LoggerFactory.getLogger(DpcCostRollupService.class);

    /** Code of the catch-all scope template for items that match no other pattern. */
    static final String ELEVATION_SCOPE_CODE = "ELEVATION";

    private final BoqItemRepository boqItemRepository;
    private final BoqDocumentRepository boqDocumentRepository;
    private final DpcScopeTemplateRepository dpcScopeTemplateRepository;
    private final CustomerProjectRepository customerProjectRepository;

    public DpcCostRollupService(BoqItemRepository boqItemRepository,
                                BoqDocumentRepository boqDocumentRepository,
                                DpcScopeTemplateRepository dpcScopeTemplateRepository,
                                CustomerProjectRepository customerProjectRepository) {
        this.boqItemRepository = boqItemRepository;
        this.boqDocumentRepository = boqDocumentRepository;
        this.dpcScopeTemplateRepository = dpcScopeTemplateRepository;
        this.customerProjectRepository = customerProjectRepository;
    }

    /**
     * Build the master cost summary for one project + BoQ pair.
     *
     * @throws IllegalArgumentException if the project or BoQ does not exist
     * @throws IllegalStateException    if the BoQ is not yet APPROVED
     */
    public DpcMasterCostSummaryDto computeForProject(Long projectId, Long boqDocumentId) {
        BoqDocument boq = boqDocumentRepository.findById(boqDocumentId)
                .orElseThrow(() -> new IllegalArgumentException("BoQ document not found: " + boqDocumentId));

        if (!boq.isApproved()) {
            throw new IllegalStateException("BoQ must be APPROVED before DPC rollup");
        }

        CustomerProject project = customerProjectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        // Fetch all active items for the project. The existing BoQ flow does
        // NOT back-link items to the BoqDocument (submitForApproval only
        // snapshots totals); items live at the project level. So the rollup
        // accepts both: items already linked to the pinned doc, AND items
        // with no doc link at all (legacy / pre-snapshot rows).
        List<BoqItem> allItems = boqItemRepository.findByProjectIdWithAssociations(projectId);
        List<BoqItem> items = new ArrayList<>(allItems.size());
        for (BoqItem item : allItems) {
            if (!Boolean.TRUE.equals(item.getIsActive())) continue;
            BoqDocument linked = item.getBoqDocument();
            if (linked == null || boqDocumentId.equals(linked.getId())) {
                items.add(item);
            }
        }

        List<DpcScopeTemplate> templates = dpcScopeTemplateRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        if (templates.isEmpty()) {
            log.warn("No active DPC scope templates configured — rollup will be empty");
            return new DpcMasterCostSummaryDto(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    null, null, project.getSqfeet(), List.of()
            );
        }

        // Bucket items per scope template.
        Map<Long, List<BoqItem>> bucketByTemplateId = new HashMap<>();
        for (DpcScopeTemplate t : templates) bucketByTemplateId.put(t.getId(), new ArrayList<>());

        DpcScopeTemplate elevation = findElevationFallback(templates);

        for (BoqItem item : items) {
            DpcScopeTemplate matched = findMatchingScope(item.getCategory(), templates).orElse(elevation);
            if (matched == null) {
                // No ELEVATION configured either — drop from rollup; item totals will not appear.
                log.debug("BoQ item {} has no matching scope and no ELEVATION fallback — skipping", item.getId());
                continue;
            }
            bucketByTemplateId.get(matched.getId()).add(item);
        }

        // Compute rollup per scope, in template displayOrder.
        BigDecimal totalOriginal = BigDecimal.ZERO;
        BigDecimal totalCustomized = BigDecimal.ZERO;
        BigDecimal sqfeet = project.getSqfeet();
        boolean hasSqfeet = sqfeet != null && sqfeet.compareTo(BigDecimal.ZERO) > 0;

        List<DpcCostRollupDto> rollups = new ArrayList<>(templates.size());
        for (DpcScopeTemplate template : templates) {
            List<BoqItem> bucket = bucketByTemplateId.get(template.getId());

            BigDecimal originalAmount = BigDecimal.ZERO;
            BigDecimal customizedAmount = BigDecimal.ZERO;
            for (BoqItem item : bucket) {
                BigDecimal amount = item.getTotalAmount() != null ? item.getTotalAmount() : BigDecimal.ZERO;
                ItemKind kind = item.getItemKind();
                if (ItemKind.BASE == kind) {
                    originalAmount = originalAmount.add(amount);
                    customizedAmount = customizedAmount.add(amount);
                } else if (ItemKind.ADDON == kind) {
                    customizedAmount = customizedAmount.add(amount);
                }
                // OPTIONAL / EXCLUSION are not included in either roll-up total.
            }
            BigDecimal variance = customizedAmount.subtract(originalAmount);

            BigDecimal originalPerSqft = hasSqfeet
                    ? originalAmount.divide(sqfeet, 0, RoundingMode.HALF_UP) : null;
            BigDecimal customizedPerSqft = hasSqfeet
                    ? customizedAmount.divide(sqfeet, 0, RoundingMode.HALF_UP) : null;

            rollups.add(new DpcCostRollupDto(
                    template.getCode(),
                    template.getTitle(),
                    originalAmount,
                    customizedAmount,
                    variance,
                    originalPerSqft,
                    customizedPerSqft
            ));

            totalOriginal = totalOriginal.add(originalAmount);
            totalCustomized = totalCustomized.add(customizedAmount);
        }

        BigDecimal totalVariance = totalCustomized.subtract(totalOriginal);
        BigDecimal totalOriginalPerSqft = hasSqfeet
                ? totalOriginal.divide(sqfeet, 0, RoundingMode.HALF_UP) : null;
        BigDecimal totalCustomizedPerSqft = hasSqfeet
                ? totalCustomized.divide(sqfeet, 0, RoundingMode.HALF_UP) : null;

        return new DpcMasterCostSummaryDto(
                totalOriginal,
                totalCustomized,
                totalVariance,
                totalOriginalPerSqft,
                totalCustomizedPerSqft,
                sqfeet,
                rollups
        );
    }

    /**
     * Find the first scope template whose {@code boqCategoryPatterns} contains
     * a pattern that is a case-insensitive substring of the category name.
     *
     * Templates should be supplied in the desired priority order
     * (ascending displayOrder), since the first match wins.  Returns empty
     * when the category is null or no template matches.
     *
     * Package-private so unit tests can exercise the matching logic directly.
     */
    Optional<DpcScopeTemplate> findMatchingScope(BoqCategory category, List<DpcScopeTemplate> templates) {
        if (category == null || category.getName() == null || templates == null || templates.isEmpty()) {
            return Optional.empty();
        }
        String haystack = category.getName().toLowerCase(Locale.ROOT);
        for (DpcScopeTemplate template : templates) {
            List<String> patterns = template.getBoqCategoryPatterns();
            if (patterns == null) continue;
            for (String pattern : patterns) {
                if (pattern == null || pattern.isBlank()) continue;
                if (haystack.contains(pattern.toLowerCase(Locale.ROOT))) {
                    return Optional.of(template);
                }
            }
        }
        return Optional.empty();
    }

    private DpcScopeTemplate findElevationFallback(List<DpcScopeTemplate> templates) {
        for (DpcScopeTemplate t : templates) {
            if (ELEVATION_SCOPE_CODE.equalsIgnoreCase(t.getCode())) return t;
        }
        return null;
    }
}
