package com.wd.api.service;

import com.wd.api.dto.quotation.CreateQuotationCatalogItemRequest;
import com.wd.api.dto.quotation.PromoteItemToCatalogRequest;
import com.wd.api.dto.quotation.QuotationCatalogItemDto;
import com.wd.api.model.LeadQuotationItem;
import com.wd.api.model.QuotationCatalogItem;
import com.wd.api.repository.LeadQuotationItemRepository;
import com.wd.api.repository.QuotationCatalogItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Owns the "promote ad-hoc item to catalog" workflow because it touches
 * both the catalog repository AND the existing {@link LeadQuotationItem}
 * row that needs to be linked to the new catalog id.
 */
@Service
@Transactional
public class QuotationCatalogPromotionService {

    private static final Logger logger = LoggerFactory.getLogger(QuotationCatalogPromotionService.class);

    private final QuotationCatalogItemRepository catalogRepository;
    private final LeadQuotationItemRepository quotationItemRepository;
    private final QuotationCatalogService catalogService;

    public QuotationCatalogPromotionService(
            QuotationCatalogItemRepository catalogRepository,
            LeadQuotationItemRepository quotationItemRepository,
            QuotationCatalogService catalogService) {
        this.catalogRepository = catalogRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.catalogService = catalogService;
    }

    /**
     * Create a catalog row from an ad-hoc {@link LeadQuotationItem} and link
     * the line item back to the new catalog row.
     *
     * @throws IllegalArgumentException if {@code itemId} is not found
     * @throws IllegalStateException if the line item is already linked to the catalog
     */
    public QuotationCatalogItemDto promoteAdHocItem(Long itemId, PromoteItemToCatalogRequest req) {
        LeadQuotationItem item = quotationItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Quotation item not found: " + itemId));

        if (item.getCatalogItem() != null) {
            throw new IllegalStateException(
                    "Item is already linked to catalog id " + item.getCatalogItem().getId());
        }

        // Resolve code: explicit -> derived from name.
        String code = (req.code() != null && !req.code().isBlank())
                ? req.code()
                : deriveCodeFromName(req.name());

        // Resolve default unit price: explicit -> source line's unit price -> ZERO.
        BigDecimal defaultUnitPrice = req.defaultUnitPrice();
        if (defaultUnitPrice == null) {
            defaultUnitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        }

        CreateQuotationCatalogItemRequest createReq = new CreateQuotationCatalogItemRequest(
                code,
                req.name(),
                item.getDescription(),
                req.category(),
                req.unit(),
                defaultUnitPrice
        );

        QuotationCatalogItemDto created = catalogService.create(createReq);

        // Re-load the saved entity to set on the line item (need a managed reference).
        QuotationCatalogItem savedEntity = catalogRepository.findById(created.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Newly-created catalog row vanished: id=" + created.id()));

        item.setCatalogItem(savedEntity);
        quotationItemRepository.save(item);

        logger.info("Promoted ad-hoc item id={} to catalog id={} code={}",
                itemId, created.id(), created.code());
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
