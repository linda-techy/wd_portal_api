package com.wd.api.controller;

import com.wd.api.model.PurchaseOrder;
import com.wd.api.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService poService;

    @PostMapping("/project/{projectId}")
    public ResponseEntity<PurchaseOrder> createPurchaseOrder(@PathVariable Long projectId,
            @RequestParam Long vendorId,
            @RequestBody PurchaseOrder po) {
        return ResponseEntity.ok(poService.createPurchaseOrder(projectId, vendorId, po));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<PurchaseOrder>> getProjectPurchaseOrders(@PathVariable Long projectId) {
        return ResponseEntity.ok(poService.getProjectPurchaseOrders(projectId));
    }
}
