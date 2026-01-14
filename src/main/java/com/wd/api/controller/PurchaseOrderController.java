package com.wd.api.controller;

import com.wd.api.model.PurchaseOrder;
import com.wd.api.dto.PurchaseOrderSearchFilter;
import com.wd.api.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService poService;

    /**
     * NEW: Standardized search endpoint with pagination and filters
     */
    @GetMapping("/search")
    public ResponseEntity<Page<PurchaseOrder>> searchPurchaseOrders(
            @ModelAttribute PurchaseOrderSearchFilter filter) {
        return ResponseEntity.ok(poService.search(filter));
    }

    @PostMapping("/project/{projectId}")
    public ResponseEntity<PurchaseOrder> createPurchaseOrder(@PathVariable Long projectId,
            @RequestParam Long vendorId,
            @RequestBody PurchaseOrder po) {
        return ResponseEntity.ok(poService.createPurchaseOrder(projectId, vendorId, po));
    }

    /**
     * DEPRECATED: Use /search endpoint instead
     */
    @GetMapping("/project/{projectId}")
    @Deprecated
    public ResponseEntity<List<PurchaseOrder>> getProjectPurchaseOrders(@PathVariable Long projectId) {
        return ResponseEntity.ok(poService.getProjectPurchaseOrders(projectId));
    }
}
