package com.wd.api.controller;

import com.wd.api.dto.MaterialDTO;
import com.wd.api.dto.InventoryStockDTO;
import com.wd.api.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/materials")
    public ResponseEntity<MaterialDTO> createMaterial(@RequestBody MaterialDTO dto) {
        return ResponseEntity.ok(inventoryService.createMaterial(dto));
    }

    @GetMapping("/materials")
    public ResponseEntity<List<MaterialDTO>> getAllMaterials() {
        return ResponseEntity.ok(inventoryService.getAllMaterials());
    }

    @GetMapping("/stock/project/{projectId}")
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
