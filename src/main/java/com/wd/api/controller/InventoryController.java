package com.wd.api.controller;

import com.wd.api.dto.MaterialDTO;
import com.wd.api.dto.InventoryStockDTO;
import com.wd.api.dto.InventorySearchFilter;
import com.wd.api.model.Material;
import com.wd.api.model.InventoryStock;
import com.wd.api.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * NEW: Standardized search for materials with pagination
     */
    @GetMapping("/materials/search")
    public ResponseEntity<Page<Material>> searchMaterials(@ModelAttribute InventorySearchFilter filter) {
        return ResponseEntity.ok(inventoryService.searchMaterials(filter));
    }

    /**
     * NEW: Standardized search for stock with pagination
     */
    @GetMapping("/stock/search")
    public ResponseEntity<Page<InventoryStock>> searchStock(@ModelAttribute InventorySearchFilter filter) {
        return ResponseEntity.ok(inventoryService.searchStock(filter));
    }

    @PostMapping("/materials")
    public ResponseEntity<MaterialDTO> createMaterial(@RequestBody MaterialDTO dto) {
        return ResponseEntity.ok(inventoryService.createMaterial(dto));
    }

    /**
     * Returns all materials without pagination.
     *
     * @deprecated Use {@code /materials/search} ({@link #searchMaterials}) instead,
     *             which supports pagination and filtering.
     */
    @GetMapping("/materials")
    @Deprecated(since = "2026-06")
    public ResponseEntity<List<MaterialDTO>> getAllMaterials() {
        return ResponseEntity.ok(inventoryService.getAllMaterials());
    }

    /**
     * Returns stock for a single project without pagination.
     *
     * @deprecated Use {@code /stock/search} ({@link #searchStock}) with a
     *             {@code projectId} filter instead, which supports pagination.
     */
    @GetMapping("/stock/project/{projectId}")
    @Deprecated(since = "2026-06")
    public ResponseEntity<List<InventoryStockDTO>> getProjectStock(@PathVariable Long projectId) {
        return ResponseEntity.ok(inventoryService.getStockByProject(projectId));
    }

    @GetMapping("/reports/consumption/{projectId}")
    public ResponseEntity<List<com.wd.api.dto.MaterialConsumptionDTO>> getConsumptionReport(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(inventoryService.getConsumptionReport(projectId));
    }

    /**
     * Update Material
     */
    @PutMapping("/materials/{id}")
    public ResponseEntity<MaterialDTO> updateMaterial(
            @PathVariable Long id,
            @RequestBody MaterialDTO dto) {
        return ResponseEntity.ok(inventoryService.updateMaterial(id, dto));
    }

    /**
     * Deactivate Material (soft delete - enterprise pattern)
     */
    @DeleteMapping("/materials/{id}")
    public ResponseEntity<Void> deactivateMaterial(@PathVariable Long id) {
        inventoryService.deactivateMaterial(id);
        return ResponseEntity.ok().build();
    }
}
