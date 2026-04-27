package com.wd.api.service.dpc;

import com.wd.api.dto.dpc.CreateDpcCustomizationCatalogItemRequest;
import com.wd.api.dto.dpc.DpcCustomizationCatalogItemDto;
import com.wd.api.dto.dpc.DpcCustomizationCatalogSearchFilter;
import com.wd.api.dto.dpc.UpdateDpcCustomizationCatalogItemRequest;
import com.wd.api.model.DpcCustomizationCatalogItem;
import com.wd.api.repository.DpcCustomizationCatalogRepository;
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
 * Service for the DPC-customization-catalog (admin-managed master library).
 *
 * Read endpoints power the picker UI used while building a DPC; write
 * endpoints (create/update/soft-delete) are gated behind the
 * {@code DPC_CUSTOMIZATION_CATALOG_MANAGE} authority by the controller layer.
 *
 * Mirrors {@link com.wd.api.service.QuotationCatalogService} almost
 * exactly — the only modeling difference is the lump-sum {@code defaultAmount}
 * field replacing the quotation catalog's {@code defaultUnitPrice}.
 */
@Service
@Transactional
public class DpcCustomizationCatalogService {

    private static final Logger logger = LoggerFactory.getLogger(DpcCustomizationCatalogService.class);

    private final DpcCustomizationCatalogRepository repository;

    public DpcCustomizationCatalogService(DpcCustomizationCatalogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<DpcCustomizationCatalogItemDto> search(DpcCustomizationCatalogSearchFilter filter) {
        Specification<DpcCustomizationCatalogItem> spec = buildSpecification(filter);

        Sort.Direction direction = "asc".equalsIgnoreCase(filter.sortDirectionOrDefault())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(
                filter.pageOrDefault(),
                filter.sizeOrDefault(),
                Sort.by(direction, filter.sortByOrDefault()));

        return repository.findAll(spec, pageable).map(DpcCustomizationCatalogItemDto::from);
    }

    private Specification<DpcCustomizationCatalogItem> buildSpecification(DpcCustomizationCatalogSearchFilter filter) {
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
    public DpcCustomizationCatalogItemDto getById(Long id) {
        DpcCustomizationCatalogItem entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DPC customization catalog item not found: " + id));
        return DpcCustomizationCatalogItemDto.from(entity);
    }

    public DpcCustomizationCatalogItemDto create(CreateDpcCustomizationCatalogItemRequest req) {
        if (req.code() != null && repository.existsByCodeIgnoreCase(req.code())) {
            throw new IllegalStateException("Catalog item with code '" + req.code() + "' already exists");
        }

        DpcCustomizationCatalogItem entity = new DpcCustomizationCatalogItem();
        entity.setCode(req.code());
        entity.setName(req.name());
        entity.setDescription(req.description());
        entity.setCategory(req.category());
        entity.setUnit(req.unit());
        entity.setDefaultAmount(req.defaultAmount());
        entity.setTimesUsed(0);
        entity.setIsActive(true);

        DpcCustomizationCatalogItem saved = repository.save(entity);
        logger.info("Created DPC customization catalog item id={} code={}", saved.getId(), saved.getCode());
        return DpcCustomizationCatalogItemDto.from(saved);
    }

    public DpcCustomizationCatalogItemDto update(Long id, UpdateDpcCustomizationCatalogItemRequest req) {
        DpcCustomizationCatalogItem entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DPC customization catalog item not found: " + id));

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
        if (req.defaultAmount() != null) entity.setDefaultAmount(req.defaultAmount());
        if (req.isActive() != null) entity.setIsActive(req.isActive());

        DpcCustomizationCatalogItem saved = repository.save(entity);
        return DpcCustomizationCatalogItemDto.from(saved);
    }

    public void softDelete(Long id) {
        DpcCustomizationCatalogItem entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DPC customization catalog item not found: " + id));

        // @SQLDelete on the entity converts this into UPDATE dpc_customization_catalog
        // SET deleted_at = NOW() ... and @SQLRestriction("deleted_at IS NULL")
        // hides the row from all subsequent queries.
        repository.delete(entity);
        logger.info("Soft-deleted DPC customization catalog item id={} code={}", id, entity.getCode());
    }

    public void incrementUsageCount(Long id) {
        repository.incrementTimesUsed(id);
    }
}
