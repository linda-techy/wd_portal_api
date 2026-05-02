package com.wd.api.estimation.service;

import com.wd.api.estimation.dto.EstimationOptionsResponse;
import com.wd.api.estimation.dto.EstimationOptionsResponse.AddonDto;
import com.wd.api.estimation.dto.EstimationOptionsResponse.CustomisationCategoryDto;
import com.wd.api.estimation.dto.EstimationOptionsResponse.CustomisationOptionDto;
import com.wd.api.estimation.dto.EstimationOptionsResponse.GovtFeeDto;
import com.wd.api.estimation.dto.EstimationOptionsResponse.SiteFeeDto;
import com.wd.api.estimation.repository.AddonRepository;
import com.wd.api.estimation.repository.CustomisationCategoryRepository;
import com.wd.api.estimation.repository.CustomisationOptionRepository;
import com.wd.api.estimation.repository.GovtFeeRepository;
import com.wd.api.estimation.repository.SiteFeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EstimationOptionsService {

    private final CustomisationCategoryRepository categoryRepo;
    private final CustomisationOptionRepository optionRepo;
    private final AddonRepository addonRepo;
    private final SiteFeeRepository siteFeeRepo;
    private final GovtFeeRepository govtFeeRepo;

    public EstimationOptionsService(
            CustomisationCategoryRepository categoryRepo,
            CustomisationOptionRepository optionRepo,
            AddonRepository addonRepo,
            SiteFeeRepository siteFeeRepo,
            GovtFeeRepository govtFeeRepo) {
        this.categoryRepo = categoryRepo;
        this.optionRepo = optionRepo;
        this.addonRepo = addonRepo;
        this.siteFeeRepo = siteFeeRepo;
        this.govtFeeRepo = govtFeeRepo;
    }

    @Transactional(readOnly = true)
    public EstimationOptionsResponse listOptions(UUID packageId) {
        // TODO: per-package filtering via package_default_customisation when needed.
        // For now all options are returned regardless of package.

        var categories = categoryRepo.findAllByOrderByDisplayOrderAsc().stream()
                .map(c -> {
                    var opts = optionRepo.findByCategoryIdOrderByDisplayOrderAsc(c.getId()).stream()
                            .map(o -> new CustomisationOptionDto(
                                    o.getId(), o.getCategoryId(), o.getName(), o.getRate(), o.getDisplayOrder()))
                            .toList();
                    return new CustomisationCategoryDto(
                            c.getId(), c.getName(), c.getPricingMode().name(), c.getDisplayOrder(), opts);
                })
                .toList();

        var addons = addonRepo.findByActiveTrueOrderByName().stream()
                .map(a -> new AddonDto(a.getId(), a.getName(), a.getDescription(), a.getLumpAmount()))
                .toList();

        var siteFees = siteFeeRepo.findByActiveTrueOrderByName().stream()
                .map(f -> new SiteFeeDto(
                        f.getId(), f.getName(), f.getMode().name(), f.getLumpAmount(), f.getPerSqftRate()))
                .toList();

        var govtFees = govtFeeRepo.findByActiveTrueOrderByName().stream()
                .map(g -> new GovtFeeDto(g.getId(), g.getName(), g.getLumpAmount()))
                .toList();

        return new EstimationOptionsResponse(categories, addons, siteFees, govtFees);
    }
}
