package com.wd.api.estimation.service;

import com.wd.api.estimation.domain.Addon;
import com.wd.api.estimation.domain.CustomisationOption;
import com.wd.api.estimation.domain.GovtFee;
import com.wd.api.estimation.domain.MarketIndexSnapshot;
import com.wd.api.estimation.domain.PackageRateVersion;
import com.wd.api.estimation.domain.SiteFee;
import com.wd.api.estimation.domain.EstimationPackage;
import com.wd.api.estimation.dto.CalculatePreviewRequest;
import com.wd.api.estimation.dto.CalculatePreviewResponse;
import com.wd.api.estimation.dto.CustomisationChoiceDto;
import com.wd.api.estimation.dto.LineItemDto;
import com.wd.api.estimation.repository.AddonRepository;
import com.wd.api.estimation.repository.CustomisationOptionRepository;
import com.wd.api.estimation.repository.EstimationPackageRepository;
import com.wd.api.estimation.repository.GovtFeeRepository;
import com.wd.api.estimation.repository.MarketIndexSnapshotRepository;
import com.wd.api.estimation.repository.PackageRateVersionRepository;
import com.wd.api.estimation.repository.SiteFeeRepository;
import com.wd.api.estimation.service.calc.EstimationBreakdown;
import com.wd.api.estimation.service.calc.EstimationCalculator;
import com.wd.api.estimation.service.calc.EstimationContext;
import com.wd.api.estimation.service.calc.LineItem;
import com.wd.api.estimation.service.calc.mapping.AddonMapper;
import com.wd.api.estimation.service.calc.mapping.GovtFeeMapper;
import com.wd.api.estimation.service.calc.mapping.MarketIndexMapper;
import com.wd.api.estimation.service.calc.mapping.PackageMapper;
import com.wd.api.estimation.service.calc.mapping.RateVersionMapper;
import com.wd.api.estimation.service.calc.mapping.SiteFeeMapper;
import com.wd.api.estimation.service.calc.view.AddOnApplied;
import com.wd.api.estimation.service.calc.view.CalculationConstants;
import com.wd.api.estimation.service.calc.view.CustomisationChoice;
import com.wd.api.estimation.service.calc.view.Dimensions;
import com.wd.api.estimation.service.calc.view.Floor;
import com.wd.api.estimation.service.calc.view.GovtFeeApplied;
import com.wd.api.estimation.service.calc.view.SiteFeeApplied;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class EstimationPreviewService {

    private static final CalculationConstants DEFAULTS = new CalculationConstants(
            new BigDecimal("0.50"),  // semiCoveredFactor
            new BigDecimal("0.25"),  // openTerraceFactor
            new BigDecimal("0.10")); // customisationDriftThreshold

    private final EstimationPackageRepository packageRepo;
    private final PackageRateVersionRepository rateVersionRepo;
    private final MarketIndexSnapshotRepository marketIndexRepo;
    private final CustomisationOptionRepository optionRepo;
    private final SiteFeeRepository siteFeeRepo;
    private final AddonRepository addonRepo;
    private final GovtFeeRepository govtFeeRepo;
    private final EstimationCalculator calculator = new EstimationCalculator();

    public EstimationPreviewService(
            EstimationPackageRepository packageRepo,
            PackageRateVersionRepository rateVersionRepo,
            MarketIndexSnapshotRepository marketIndexRepo,
            CustomisationOptionRepository optionRepo,
            SiteFeeRepository siteFeeRepo,
            AddonRepository addonRepo,
            GovtFeeRepository govtFeeRepo) {
        this.packageRepo = packageRepo;
        this.rateVersionRepo = rateVersionRepo;
        this.marketIndexRepo = marketIndexRepo;
        this.optionRepo = optionRepo;
        this.siteFeeRepo = siteFeeRepo;
        this.addonRepo = addonRepo;
        this.govtFeeRepo = govtFeeRepo;
    }

    @Transactional(readOnly = true)
    public CalculatePreviewResponse preview(CalculatePreviewRequest req) {
        EstimationPackage pkg = packageRepo.findById(req.packageId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown package id: " + req.packageId()));

        PackageRateVersion rv = req.rateVersionIdOverride() != null
                ? rateVersionRepo.findById(req.rateVersionIdOverride())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown rate version id: " + req.rateVersionIdOverride()))
                : rateVersionRepo.findActive(pkg.getId(), req.projectType(), LocalDate.now())
                        .orElseThrow(() -> new IllegalStateException("No active rate version for package " + pkg.getId() + " and project type " + req.projectType()));

        MarketIndexSnapshot mi = req.marketIndexIdOverride() != null
                ? marketIndexRepo.findById(req.marketIndexIdOverride())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown market index id: " + req.marketIndexIdOverride()))
                : marketIndexRepo.findActive()
                        .orElseThrow(() -> new IllegalStateException("No active market index snapshot"));

        EstimationContext ctx = new EstimationContext(
                req.projectType(),
                PackageMapper.toView(pkg),
                RateVersionMapper.toView(rv),
                MarketIndexMapper.toView(mi),
                toViewDimensions(req),
                toViewCustomisations(req.customisations(), req.dimensions()),
                toViewSiteFees(req.siteFees()),
                toViewAddOns(req.addOns()),
                toViewGovtFees(req.govtFees()),
                req.discountPercent(),
                req.gstRate(),
                DEFAULTS);

        EstimationBreakdown breakdown = calculator.calculate(ctx);
        return toResponse(breakdown);
    }

    private Dimensions toViewDimensions(CalculatePreviewRequest req) {
        List<Floor> floors = req.dimensions().floors() == null ? List.of() :
                req.dimensions().floors().stream()
                        .map(f -> new Floor(f.floorName(), f.length(), f.width()))
                        .toList();
        return new Dimensions(floors, req.dimensions().semiCoveredArea(), req.dimensions().openTerraceArea());
    }

    private List<CustomisationChoice> toViewCustomisations(List<CustomisationChoiceDto> choices,
                                                             com.wd.api.estimation.dto.DimensionsDto dims) {
        if (choices == null || choices.isEmpty()) return List.of();
        // Approx chargeable area for delta×area (calculator recomputes its own value; this is what gets passed in)
        BigDecimal chargeable = dims.floors() == null ? BigDecimal.ZERO :
                dims.floors().stream()
                        .map(f -> f.length().multiply(f.width()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        return choices.stream().map(c -> {
            CustomisationOption opt = optionRepo.findById(c.optionId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown customisation option id: " + c.optionId()));
            // Simple delta: option rate - 0 (full PR-3 semantics; future PR computes vs package_default_customisation)
            return new CustomisationChoice(
                    c.categoryId(), c.optionId(), null,
                    opt.getName(),
                    opt.getCategoryId() != null ? null /* category lookup deferred */ : null,
                    opt.getRate(),
                    chargeable);
        }).toList();
    }

    private List<SiteFeeApplied> toViewSiteFees(List<com.wd.api.estimation.dto.SiteFeeRefDto> refs) {
        if (refs == null || refs.isEmpty()) return List.of();
        return refs.stream().map(ref -> {
            SiteFee f = siteFeeRepo.findById(ref.id())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown site fee id: " + ref.id()));
            return SiteFeeMapper.toView(f);
        }).toList();
    }

    private List<AddOnApplied> toViewAddOns(List<com.wd.api.estimation.dto.AddOnRefDto> refs) {
        if (refs == null || refs.isEmpty()) return List.of();
        return refs.stream().map(ref -> {
            Addon a = addonRepo.findById(ref.id())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown addon id: " + ref.id()));
            return AddonMapper.toView(a);
        }).toList();
    }

    private List<GovtFeeApplied> toViewGovtFees(List<com.wd.api.estimation.dto.GovtFeeRefDto> refs) {
        if (refs == null || refs.isEmpty()) return List.of();
        return refs.stream().map(ref -> {
            GovtFee f = govtFeeRepo.findById(ref.id())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown govt fee id: " + ref.id()));
            return GovtFeeMapper.toView(f);
        }).toList();
    }

    private CalculatePreviewResponse toResponse(EstimationBreakdown b) {
        List<LineItemDto> lines = b.lineItems().stream()
                .map(this::toLineItemDto)
                .toList();
        return new CalculatePreviewResponse(
                b.chargeableArea(), b.baseCost(), b.customisationCost(),
                b.siteCost(), b.addOnCost(), b.fluctuationAdjustment(),
                b.subtotal(), b.govtFees(), b.discount(), b.taxable(),
                b.gst(), b.grandTotal(),
                lines, b.warnings());
    }

    private LineItemDto toLineItemDto(LineItem li) {
        return new LineItemDto(
                li.lineType(), li.description(), li.sourceRefId(),
                li.quantity(), li.unit(), li.unitRate(), li.amount(),
                li.displayOrder());
    }
}
