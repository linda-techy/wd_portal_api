package com.wd.api.estimation.service.admin;

import com.wd.api.estimation.domain.MarketIndexSnapshot;
import com.wd.api.estimation.dto.admin.MarketIndexCreateRequest;
import com.wd.api.estimation.dto.admin.MarketIndexResponse;
import com.wd.api.estimation.repository.MarketIndexSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class MarketIndexAdminService {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final BigDecimal MIN_WEIGHT_SUM = new BigDecimal("0.99");
    private static final BigDecimal MAX_WEIGHT_SUM = new BigDecimal("1.01");

    private final MarketIndexSnapshotRepository repo;

    public MarketIndexAdminService(MarketIndexSnapshotRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public MarketIndexResponse createSnapshot(MarketIndexCreateRequest req) {
        validateWeights(req.weights());

        LocalDate snapshotDate = req.snapshotDate() != null ? req.snapshotDate() : LocalDate.now();

        // Compute composite_index. Baseline = previous active snapshot's rates.
        // First snapshot ever has no baseline → composite = 1.0000 by definition.
        Optional<MarketIndexSnapshot> previous = repo.findActive();
        BigDecimal composite = previous.isPresent()
                ? computeComposite(req, previous.get())
                : new BigDecimal("1.0000");

        // Deactivate the previous active row. saveAndFlush is required (not just save) — the
        // partial unique index uq_estimation_market_index_active rejects two rows with
        // is_active=true. Without an immediate flush, Hibernate's action queue inserts the new
        // row before updating the old one, triggering a constraint violation at commit.
        previous.ifPresent(p -> {
            p.setActive(false);
            repo.saveAndFlush(p);
        });

        MarketIndexSnapshot snap = new MarketIndexSnapshot();
        snap.setSnapshotDate(snapshotDate);
        snap.setSteelRate(req.steelRate());
        snap.setCementRate(req.cementRate());
        snap.setSandRate(req.sandRate());
        snap.setAggregateRate(req.aggregateRate());
        snap.setTilesRate(req.tilesRate());
        snap.setElectricalRate(req.electricalRate());
        snap.setPaintsRate(req.paintsRate());
        snap.setWeightsJson(toObjectMap(req.weights()));
        snap.setCompositeIndex(composite.setScale(4, RoundingMode.HALF_UP));
        snap.setActive(true);

        return MarketIndexResponse.fromEntity(repo.save(snap));
    }

    @Transactional(readOnly = true)
    public MarketIndexResponse get(UUID id) {
        MarketIndexSnapshot snap = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Market index snapshot not found: " + id));
        return MarketIndexResponse.fromEntity(snap);
    }

    @Transactional(readOnly = true)
    public List<MarketIndexResponse> list() {
        // DB-side sort (append-only table grows over time, avoid eager-load + Java comparator).
        return repo.findAllByOrderBySnapshotDateDesc().stream()
                .map(MarketIndexResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<MarketIndexResponse> getActive() {
        return repo.findActive().map(MarketIndexResponse::fromEntity);
    }

    // ---- internals ----

    private void validateWeights(Map<String, String> weights) {
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("weights map is required and must not be empty");
        }
        BigDecimal sum = weights.values().stream()
                .map(BigDecimal::new)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b, MC));
        if (sum.compareTo(MIN_WEIGHT_SUM) < 0 || sum.compareTo(MAX_WEIGHT_SUM) > 0) {
            throw new IllegalArgumentException(
                    "weights must sum to between 0.99 and 1.01 (was " + sum.toPlainString() + ")");
        }
    }

    private BigDecimal computeComposite(MarketIndexCreateRequest req, MarketIndexSnapshot baseline) {
        // composite = Σ((currentRate[i] / baselineRate[i]) × weight[i])
        Map<String, BigDecimal> currentByCommodity = Map.of(
                "steel", req.steelRate(),
                "cement", req.cementRate(),
                "sand", req.sandRate(),
                "aggregate", req.aggregateRate(),
                "tiles", req.tilesRate(),
                "electrical", req.electricalRate(),
                "paints", req.paintsRate());
        Map<String, BigDecimal> baselineByCommodity = Map.of(
                "steel", baseline.getSteelRate(),
                "cement", baseline.getCementRate(),
                "sand", baseline.getSandRate(),
                "aggregate", baseline.getAggregateRate(),
                "tiles", baseline.getTilesRate(),
                "electrical", baseline.getElectricalRate(),
                "paints", baseline.getPaintsRate());

        BigDecimal sum = BigDecimal.ZERO;
        for (Map.Entry<String, String> e : req.weights().entrySet()) {
            String commodity = e.getKey();
            BigDecimal weight = new BigDecimal(e.getValue());
            BigDecimal current = currentByCommodity.get(commodity);
            BigDecimal base = baselineByCommodity.get(commodity);
            if (current == null || base == null) {
                throw new IllegalArgumentException("Unknown commodity in weights: " + commodity);
            }
            BigDecimal ratio = current.divide(base, 10, RoundingMode.HALF_UP);
            sum = sum.add(ratio.multiply(weight, MC), MC);
        }
        return sum;
    }

    private Map<String, Object> toObjectMap(Map<String, String> in) {
        Map<String, Object> out = new HashMap<>();
        in.forEach(out::put);
        return out;
    }
}
