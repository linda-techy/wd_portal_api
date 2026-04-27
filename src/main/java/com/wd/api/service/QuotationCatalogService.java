package com.wd.api.service;

import com.wd.api.dto.quotation.CreateQuotationCatalogItemRequest;
import com.wd.api.dto.quotation.QuotationCatalogItemDto;
import com.wd.api.dto.quotation.QuotationCatalogSearchFilter;
import com.wd.api.dto.quotation.UpdateQuotationCatalogItemRequest;
import com.wd.api.model.QuotationCatalogItem;
import com.wd.api.repository.QuotationCatalogItemRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for the quotation-item catalog (admin-managed master library).
 *
 * Read endpoints power the picker UI used while building a quotation;
 * write endpoints (create/update/soft-delete) are gated behind the
 * {@code QUOTATION_CATALOG_MANAGE} authority by the controller layer.
 */
@Service
@Transactional
public class QuotationCatalogService {

    private static final Logger logger = LoggerFactory.getLogger(QuotationCatalogService.class);

    private final QuotationCatalogItemRepository repository;

    public QuotationCatalogService(QuotationCatalogItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<QuotationCatalogItemDto> search(QuotationCatalogSearchFilter filter) {
        Specification<QuotationCatalogItem> spec = buildSpecification(filter);

        Sort.Direction direction = "asc".equalsIgnoreCase(filter.sortDirectionOrDefault())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(
                filter.pageOrDefault(),
                filter.sizeOrDefault(),
                Sort.by(direction, filter.sortByOrDefault()));

        return repository.findAll(spec, pageable).map(QuotationCatalogItemDto::from);
    }

    private Specification<QuotationCatalogItem> buildSpecification(QuotationCatalogSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.isActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), filter.isActive()));
            }

            if (filter.category() != null && !filter.category().isBlank()) {
                predicates.add(cb.equal(root.get("category"), filter.category()));
            }

            if (filter.search() != null && !filter.search().isBlank()) {
                String pattern = "%" + filter.search().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public QuotationCatalogItemDto getById(Long id) {
        QuotationCatalogItem entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quotation catalog item not found: " + id));
        return QuotationCatalogItemDto.from(entity);
    }

    public QuotationCatalogItemDto create(CreateQuotationCatalogItemRequest req) {
        if (req.code() != null && repository.existsByCodeIgnoreCase(req.code())) {
            throw new IllegalStateException("Catalog item with code '" + req.code() + "' already exists");
        }

        QuotationCatalogItem entity = new QuotationCatalogItem();
        entity.setCode(req.code());
        entity.setName(req.name());
        entity.setDescription(req.description());
        entity.setCategory(req.category());
        entity.setUnit(req.unit());
        entity.setDefaultUnitPrice(req.defaultUnitPrice());
        entity.setTimesUsed(0);
        entity.setIsActive(true);

        QuotationCatalogItem saved = repository.save(entity);
        logger.info("Created quotation catalog item id={} code={}", saved.getId(), saved.getCode());
        return QuotationCatalogItemDto.from(saved);
    }

    public QuotationCatalogItemDto update(Long id, UpdateQuotationCatalogItemRequest req) {
        QuotationCatalogItem entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quotation catalog item not found: " + id));

        // Patch semantics — only non-null fields are applied.
        if (req.code() != null) {
            // Reject duplicates only when changing to a different code already in use.
            if (!req.code().equalsIgnoreCase(entity.getCode())
                    && repository.existsByCodeIgnoreCase(req.code())) {
                throw new IllegalStateException("Catalog item with code '" + req.code() + "' already exists");
            }
            entity.setCode(req.code());
        }
        if (req.name() != null) entity.setName(req.name());
        if (req.description() != null) entity.setDescription(req.description());
        if (req.category() != null) entity.setCategory(req.category());
        if (req.unit() != null) entity.setUnit(req.unit());
        if (req.defaultUnitPrice() != null) entity.setDefaultUnitPrice(req.defaultUnitPrice());
        if (req.isActive() != null) entity.setIsActive(req.isActive());

        QuotationCatalogItem saved = repository.save(entity);
        return QuotationCatalogItemDto.from(saved);
    }

    public void softDelete(Long id) {
        QuotationCatalogItem entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quotation catalog item not found: " + id));

        // @SQLDelete on the entity converts this into UPDATE quotation_item_catalog
        // SET deleted_at = NOW() ... and @SQLRestriction("deleted_at IS NULL")
        // hides the row from all subsequent queries. Setting is_active alongside
        // would be redundant — the row is gone from the JPA layer either way.
        repository.delete(entity);
        logger.info("Soft-deleted quotation catalog item id={} code={}", id, entity.getCode());
    }

    public void incrementUsageCount(Long id) {
        repository.incrementTimesUsed(id);
    }
}
