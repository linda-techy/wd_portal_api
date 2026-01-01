package com.wd.api.service;

import com.wd.api.dto.MaterialDTO;
import com.wd.api.dto.InventoryStockDTO;
import com.wd.api.model.Material;
import com.wd.api.model.InventoryStock;
import com.wd.api.model.CustomerProject;
import com.wd.api.repository.MaterialRepository;
import com.wd.api.repository.InventoryStockRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.dto.MaterialConsumptionDTO;
import com.wd.api.repository.PurchaseOrderItemRepository;
import com.wd.api.repository.StockAdjustmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryService {

        private final CustomerProjectRepository projectRepository;
        private final MaterialRepository materialRepository;
        private final InventoryStockRepository stockRepository;
        private final PurchaseOrderItemRepository poItemRepository;
        private final StockAdjustmentRepository adjustmentRepository;

        public InventoryService(CustomerProjectRepository projectRepository,
                        MaterialRepository materialRepository,
                        InventoryStockRepository stockRepository,
                        PurchaseOrderItemRepository poItemRepository,
                        StockAdjustmentRepository adjustmentRepository) {
                this.projectRepository = projectRepository;
                this.materialRepository = materialRepository;
                this.stockRepository = stockRepository;
                this.poItemRepository = poItemRepository;
                this.adjustmentRepository = adjustmentRepository;
        }

        public List<MaterialConsumptionDTO> getConsumptionReport(Long projectId) {
                // 1. Get all materials associated with this project (via Stock or POs)
                // For simplicity, we fetch all materials and filter those with activity
                List<MaterialDTO> allMaterials = getAllMaterials();
                List<InventoryStockDTO> projectStock = getStockByProject(projectId);

                // 2. Fetch Aggregates
                // This is a naive implementation. In production, use a custom JPQL query for
                // performance.

                return allMaterials.stream().map(material -> {
                        Long materialId = material.getId();

                        // Current Stock
                        BigDecimal currentStock = projectStock.stream()
                                        .filter(s -> s.getMaterialId().equals(materialId))
                                        .findFirst()
                                        .map(InventoryStockDTO::getCurrentQuantity)
                                        .orElse(BigDecimal.ZERO);

                        // Total Purchased (From PO Items linked to this project)
                        // Note: This assumes PO Items are the source of truth for "Inward".
                        // A better way would be summing GRN items if available.
                        BigDecimal totalPurchased = poItemRepository
                                        .sumQuantityByProjectAndMaterial(projectId, materialId)
                                        .orElse(BigDecimal.ZERO);

                        // Adjustments
                        List<com.wd.api.model.StockAdjustment> adjustments = adjustmentRepository
                                        .findByProjectIdAndMaterialId(projectId, materialId);

                        BigDecimal wastage = adjustments.stream()
                                        .filter(a -> "WASTAGE".equals(a.getAdjustmentType()))
                                        .map(com.wd.api.model.StockAdjustment::getQuantity)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                        BigDecimal theft = adjustments.stream()
                                        .filter(a -> "THEFT".equals(a.getAdjustmentType()))
                                        .map(com.wd.api.model.StockAdjustment::getQuantity)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                        BigDecimal damage = adjustments.stream()
                                        .filter(a -> "DAMAGE".equals(a.getAdjustmentType()))
                                        .map(com.wd.api.model.StockAdjustment::getQuantity)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                        // Other adjustments (Correction, Transfer Out) could affect calc, but sticking
                        // to basics:
                        BigDecimal totalAdjustments = wastage.add(theft).add(damage);

                        // Implied Consumption = Purchased - Stock - (Negative Adjustments)
                        // Note: Adjustments are usually negative quantities in the stock ledger?
                        // In our model, we stored them as absolute quantities with a type.
                        // If we have 100 Purchased, 50 Stock, 10 Wastage -> Consumption = 40.

                        BigDecimal impliedConsumption = totalPurchased.subtract(currentStock)
                                        .subtract(totalAdjustments);
                        if (impliedConsumption.compareTo(BigDecimal.ZERO) < 0)
                                impliedConsumption = BigDecimal.ZERO;

                        if (totalPurchased.compareTo(BigDecimal.ZERO) == 0
                                        && currentStock.compareTo(BigDecimal.ZERO) == 0) {
                                return null; // Skip unused materials
                        }

                        return MaterialConsumptionDTO.builder()
                                        .materialId(materialId)
                                        .materialName(material.getName())
                                        .unit(material.getUnit())
                                        .totalPurchased(totalPurchased)
                                        .currentStock(currentStock)
                                        .totalWastage(wastage)
                                        .totalTheft(theft)
                                        .totalDamage(damage)
                                        .impliedConsumption(impliedConsumption)
                                        .build();
                })
                                .filter(dto -> dto != null)
                                .collect(Collectors.toList());
        }

        public MaterialDTO createMaterial(MaterialDTO dto) {
                Material material = Material.builder()
                                .name(dto.getName())
                                .unit(dto.getUnit())
                                .category(dto.getCategory())
                                .active(true)
                                .build();
                material = materialRepository.save(material);
                return mapToMaterialDTO(material);
        }

        public List<MaterialDTO> getAllMaterials() {
                return materialRepository.findAll().stream()
                                .map(this::mapToMaterialDTO)
                                .collect(Collectors.toList());
        }

        @Transactional
        public void updateStock(Long projectId, Long materialId, BigDecimal quantityChange) {
                CustomerProject project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new RuntimeException("Project not found"));
                Material material = materialRepository.findById(materialId)
                                .orElseThrow(() -> new RuntimeException("Material not found"));

                InventoryStock stock = stockRepository.findByProjectIdAndMaterialId(projectId, materialId)
                                .orElse(InventoryStock.builder()
                                                .project(project)
                                                .material(material)
                                                .currentQuantity(BigDecimal.ZERO)
                                                .build());

                stock.setCurrentQuantity(stock.getCurrentQuantity().add(quantityChange));
                stockRepository.save(stock);
        }

        public List<InventoryStockDTO> getStockByProject(Long projectId) {
                return stockRepository.findByProjectId(projectId).stream()
                                .map(this::mapToStockDTO)
                                .collect(Collectors.toList());
        }

        private MaterialDTO mapToMaterialDTO(Material m) {
                return MaterialDTO.builder()
                                .id(m.getId())
                                .name(m.getName())
                                .unit(m.getUnit())
                                .category(m.getCategory())
                                .active(m.isActive())
                                .build();
        }

        private InventoryStockDTO mapToStockDTO(InventoryStock s) {
                return InventoryStockDTO.builder()
                                .id(s.getId())
                                .projectId(s.getProject().getId())
                                .projectName(s.getProject().getName())
                                .materialId(s.getMaterial().getId())
                                .materialName(s.getMaterial().getName())
                                .unit(s.getMaterial().getUnit())
                                .currentQuantity(s.getCurrentQuantity())
                                .lastUpdated(s.getLastUpdated())
                                .build();
        }
}
