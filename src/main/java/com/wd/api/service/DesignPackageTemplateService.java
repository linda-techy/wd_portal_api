package com.wd.api.service;

import com.wd.api.model.DesignPackageTemplate;
import com.wd.api.repository.DesignPackageTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DesignPackageTemplateService {

    private final DesignPackageTemplateRepository repository;

    public DesignPackageTemplateService(DesignPackageTemplateRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<DesignPackageTemplate> listAll() {
        return repository.findAllByOrderByDisplayOrderAscIdAsc();
    }

    @Transactional(readOnly = true)
    public List<DesignPackageTemplate> listActive() {
        return repository.findByIsActiveTrueOrderByDisplayOrderAscIdAsc();
    }

    @Transactional(readOnly = true)
    public DesignPackageTemplate getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Design package template not found: " + id));
    }

    @Transactional
    public DesignPackageTemplate create(DesignPackageTemplate t, Long actorUserId) {
        normaliseCode(t);
        repository.findByCodeIgnoreCase(t.getCode()).ifPresent(existing -> {
            throw new IllegalArgumentException(
                    "Design package code already exists: " + existing.getCode());
        });
        t.setCreatedByUserId(actorUserId);
        t.setUpdatedByUserId(actorUserId);
        return repository.save(t);
    }

    @Transactional
    public DesignPackageTemplate update(Long id, DesignPackageTemplate patch, Long actorUserId) {
        DesignPackageTemplate existing = getById(id);
        // Code is intentionally read-only after creation — it's a stable
        // machine identifier used by the customer-app cutover and by future
        // foreign-key references from design_package_payments.packageName.
        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getTagline() != null) existing.setTagline(patch.getTagline());
        if (patch.getDescription() != null) existing.setDescription(patch.getDescription());
        if (patch.getRatePerSqft() != null) existing.setRatePerSqft(patch.getRatePerSqft());
        if (patch.getFullPaymentDiscountPct() != null) existing.setFullPaymentDiscountPct(patch.getFullPaymentDiscountPct());
        if (patch.getRevisionsIncluded() != null) existing.setRevisionsIncluded(patch.getRevisionsIncluded());
        if (patch.getFeatures() != null) existing.setFeatures(patch.getFeatures());
        if (patch.getDisplayOrder() != null) existing.setDisplayOrder(patch.getDisplayOrder());
        if (patch.getIsActive() != null) existing.setIsActive(patch.getIsActive());
        existing.setUpdatedByUserId(actorUserId);
        return repository.save(existing);
    }

    /** Soft-archive (preferred) — flips is_active off so historical
     *  design_package_payments rows referencing this code keep displaying
     *  correctly. Use {@link #delete} only for typos / never-used rows. */
    @Transactional
    public DesignPackageTemplate setActive(Long id, boolean active, Long actorUserId) {
        DesignPackageTemplate t = getById(id);
        t.setIsActive(active);
        t.setUpdatedByUserId(actorUserId);
        return repository.save(t);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Design package template not found: " + id);
        }
        repository.deleteById(id);
    }

    private void normaliseCode(DesignPackageTemplate t) {
        if (t.getCode() == null || t.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Code is required");
        }
        t.setCode(t.getCode().trim().toUpperCase().replace(' ', '_'));
    }
}
