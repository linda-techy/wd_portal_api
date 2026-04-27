package com.wd.api.service.dpc;

import com.wd.api.dto.dpc.CreateDpcCustomizationCatalogItemRequest;
import com.wd.api.dto.dpc.DpcCustomizationCatalogItemDto;
import com.wd.api.dto.dpc.PromoteCustomizationToCatalogRequest;
import com.wd.api.model.DpcCustomizationCatalogItem;
import com.wd.api.model.DpcCustomizationLine;
import com.wd.api.repository.DpcCustomizationCatalogRepository;
import com.wd.api.repository.DpcCustomizationLineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Owns the "promote ad-hoc DPC customization line to catalog" workflow
 * because it touches both the catalog repository AND the existing
 * {@link DpcCustomizationLine} row that needs to be linked to the new
 * catalog id.
 *
 * Mirrors {@link com.wd.api.service.QuotationCatalogPromotionService}.
 */
@Service
@Transactional
public class DpcCustomizationCatalogPromotionService {

    private static final Logger logger = LoggerFactory.getLogger(DpcCustomizationCatalogPromotionService.class);

    private final DpcCustomizationCatalogRepository catalogRepository;
    private final DpcCustomizationLineRepository customizationLineRepository;
    private final DpcCustomizationCatalogService catalogService;

    public DpcCustomizationCatalogPromotionService(
            DpcCustomizationCatalogRepository catalogRepository,
            DpcCustomizationLineRepository customizationLineRepository,
            DpcCustomizationCatalogService catalogService) {
        this.catalogRepository = catalogRepository;
        this.customizationLineRepository = customizationLineRepository;
        this.catalogService = catalogService;
    }

    /**
     * Create a catalog row from an ad-hoc {@link DpcCustomizationLine} and
     * link the line back to the new catalog row.
     *
     * @throws IllegalArgumentException if {@code lineId} is not found
     * @throws IllegalStateException if the line is already linked to the catalog
     */
    public DpcCustomizationCatalogItemDto promoteAdHocCustomization(Long lineId, PromoteCustomizationToCatalogRequest req) {
        DpcCustomizationLine line = customizationLineRepository.findById(lineId)
                .orElseThrow(() -> new IllegalArgumentException("DPC customization line not found: " + lineId));

        if (line.getCatalogItem() != null) {
            throw new IllegalStateException(
                    "Customization line is already linked to catalog id " + line.getCatalogItem().getId());
        }

        // Resolve code: explicit -> derived from name.
        String code = (req.code() != null && !req.code().isBlank())
                ? req.code()
                : deriveCodeFromName(req.name());

        // Resolve default amount: explicit -> source line's amount -> ZERO.
        BigDecimal defaultAmount = req.defaultAmount();
        if (defaultAmount == null) {
            defaultAmount = line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO;
        }

        CreateDpcCustomizationCatalogItemRequest createReq = new CreateDpcCustomizationCatalogItemRequest(
                code,
                req.name(),
                line.getDescription(),
                req.category(),
                req.unit(),
                defaultAmount
        );

        DpcCustomizationCatalogItemDto created = catalogService.create(createReq);

        // Re-load the saved entity to set on the line (need a managed reference).
        DpcCustomizationCatalogItem savedEntity = catalogRepository.findById(created.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Newly-created catalog row vanished: id=" + created.id()));

        line.setCatalogItem(savedEntity);
        customizationLineRepository.save(line);

        logger.info("Promoted ad-hoc DPC customization line id={} to catalog id={} code={}",
                lineId, created.id(), created.code());
        return created;
    }

    /**
     * Derive a catalog code from a name: uppercase, non-alphanumeric -> '-',
     * collapsed runs of '-', truncated to 80 chars.
     */
    static String deriveCodeFromName(String name) {
        if (name == null || name.isBlank()) {
            return "ITEM";
        }
        String upper = name.toUpperCase();
        String replaced = upper.replaceAll("[^A-Z0-9]+", "-");
        // Strip leading/trailing dashes from collapsing.
        replaced = replaced.replaceAll("^-+", "").replaceAll("-+$", "");
        if (replaced.isEmpty()) {
            replaced = "ITEM";
        }
        if (replaced.length() > 80) {
            replaced = replaced.substring(0, 80);
            replaced = replaced.replaceAll("-+$", "");
        }
        return replaced;
    }
}
