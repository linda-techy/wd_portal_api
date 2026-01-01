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
}
