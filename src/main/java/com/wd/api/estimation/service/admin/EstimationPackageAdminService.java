package com.wd.api.estimation.service.admin;

import com.wd.api.estimation.domain.EstimationPackage;
import com.wd.api.estimation.dto.admin.PackageAdminCreateRequest;
import com.wd.api.estimation.dto.admin.PackageAdminResponse;
import com.wd.api.estimation.dto.admin.PackageAdminUpdateRequest;
import com.wd.api.estimation.repository.EstimationPackageRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EstimationPackageAdminService {

    private final EstimationPackageRepository repo;

    public EstimationPackageAdminService(EstimationPackageRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public PackageAdminResponse create(PackageAdminCreateRequest req) {
        EstimationPackage pkg = new EstimationPackage();
        pkg.setInternalName(req.internalName());
        pkg.setMarketingName(req.marketingName());
        pkg.setTagline(req.tagline());
        pkg.setDescription(req.description());
        pkg.setDisplayOrder(req.displayOrder());
        pkg.setActive(true);
        EstimationPackage saved = repo.save(pkg);
        return PackageAdminResponse.fromEntity(saved);
    }

    @Transactional
    public PackageAdminResponse update(UUID id, PackageAdminUpdateRequest req) {
        EstimationPackage pkg = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estimation package not found: " + id));
        pkg.setMarketingName(req.marketingName());
        pkg.setTagline(req.tagline());
        pkg.setDescription(req.description());
        pkg.setDisplayOrder(req.displayOrder());
        pkg.setActive(req.active());
        return PackageAdminResponse.fromEntity(repo.save(pkg));
    }

    @Transactional(readOnly = true)
    public PackageAdminResponse get(UUID id) {
        EstimationPackage pkg = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estimation package not found: " + id));
        return PackageAdminResponse.fromEntity(pkg);
    }

    @Transactional(readOnly = true)
    public List<PackageAdminResponse> list(boolean includeInactive) {
        // The @Where(clause = "deleted_at IS NULL") on EstimationPackage already excludes
        // soft-deleted rows. includeInactive only affects the is_active flag — but for
        // PR-B.1 we treat inactive == soft-deleted because the toggle is the same UX.
        // Future PR may distinguish them.
        List<EstimationPackage> all = repo.findAll(Sort.by("displayOrder").ascending());
        return all.stream()
                .filter(p -> includeInactive || p.isActive())
                .map(PackageAdminResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void softDelete(UUID id) {
        EstimationPackage pkg = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estimation package not found: " + id));
        repo.delete(pkg);  // @SQLDelete on the entity translates this to UPDATE … SET deleted_at = NOW()
    }
}
