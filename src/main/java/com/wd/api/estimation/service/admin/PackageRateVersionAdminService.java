package com.wd.api.estimation.service.admin;

import com.wd.api.estimation.domain.PackageRateVersion;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.estimation.dto.admin.RateVersionCreateRequest;
import com.wd.api.estimation.dto.admin.RateVersionResponse;
import com.wd.api.estimation.repository.EstimationPackageRepository;
import com.wd.api.estimation.repository.PackageRateVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PackageRateVersionAdminService {

    private final EstimationPackageRepository packageRepo;
    private final PackageRateVersionRepository repo;

    public PackageRateVersionAdminService(
            EstimationPackageRepository packageRepo,
            PackageRateVersionRepository repo) {
        this.packageRepo = packageRepo;
        this.repo = repo;
    }

    /**
     * Atomic window-close + new insert. Both writes happen in this @Transactional
     * boundary; partial failure rolls back both, leaving the previous active version
     * intact.
     */
    @Transactional
    public RateVersionResponse createNewVersion(RateVersionCreateRequest req) {
        if (!packageRepo.existsById(req.packageId())) {
            throw new IllegalArgumentException("Estimation package not found: " + req.packageId());
        }

        LocalDate effectiveFrom = req.effectiveFrom() != null ? req.effectiveFrom() : LocalDate.now();

        // Close the previous active version's window (if any)
        Optional<PackageRateVersion> prev = repo.findActive(req.packageId(), req.projectType(), effectiveFrom);
        prev.ifPresent(p -> {
            p.setEffectiveTo(effectiveFrom.minusDays(1));
            repo.save(p);
        });

        PackageRateVersion next = new PackageRateVersion();
        next.setPackageId(req.packageId());
        next.setProjectType(req.projectType());
        next.setMaterialRate(req.materialRate());
        next.setLabourRate(req.labourRate());
        next.setOverheadRate(req.overheadRate());
        next.setEffectiveFrom(effectiveFrom);
        next.setEffectiveTo(null);

        return RateVersionResponse.fromEntity(repo.save(next));
    }

    @Transactional(readOnly = true)
    public RateVersionResponse get(UUID id) {
        PackageRateVersion rv = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rate version not found: " + id));
        return RateVersionResponse.fromEntity(rv);
    }

    @Transactional(readOnly = true)
    public List<RateVersionResponse> list(UUID packageId, ProjectType projectType) {
        // List all versions for the package + project type
        return repo.findAll().stream()
                .filter(rv -> rv.getPackageId().equals(packageId))
                .filter(rv -> rv.getProjectType() == projectType)
                .map(RateVersionResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<RateVersionResponse> getActive(UUID packageId, ProjectType projectType) {
        return repo.findActive(packageId, projectType, LocalDate.now())
                .map(RateVersionResponse::fromEntity);
    }
}
