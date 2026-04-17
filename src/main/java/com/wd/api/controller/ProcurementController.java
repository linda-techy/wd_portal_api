package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.GRNDTO;
import com.wd.api.dto.PurchaseOrderDTO;
import com.wd.api.dto.VendorDTO;
import com.wd.api.service.ProcurementService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/procurement")
@RequiredArgsConstructor
public class ProcurementController {

    private static final Logger logger = LoggerFactory.getLogger(ProcurementController.class);
    private final ProcurementService procurementService;

    @PostMapping("/vendors")
    public ResponseEntity<ApiResponse<VendorDTO>> createVendor(@RequestBody VendorDTO dto) {
        try {
            VendorDTO created = procurementService.createVendor(dto);
            return ResponseEntity.ok(ApiResponse.success("Vendor created successfully", created));
        } catch (Exception e) {
            logger.error("Error creating vendor", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to create vendor"));
        }
    }

    @GetMapping("/vendors")
    public ResponseEntity<ApiResponse<List<VendorDTO>>> getAllVendors() {
        try {
            List<VendorDTO> vendors = procurementService.getAllVendors();
            return ResponseEntity.ok(ApiResponse.success("Vendors retrieved successfully", vendors));
        } catch (Exception e) {
            logger.error("Error fetching vendors", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to retrieve vendors"));
        }
    }

    @PutMapping("/vendors/{id}")
    public ResponseEntity<ApiResponse<VendorDTO>> updateVendor(@PathVariable Long id, @RequestBody VendorDTO dto) {
        try {
            VendorDTO updated = procurementService.updateVendor(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Vendor updated successfully", updated));
        } catch (Exception e) {
            logger.error("Error updating vendor {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to update vendor"));
        }
    }

    @DeleteMapping("/vendors/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivateVendor(@PathVariable Long id) {
        try {
            procurementService.deactivateVendor(id);
            return ResponseEntity.ok(ApiResponse.success("Vendor deactivated successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(ApiResponse.error(e.getMessage())); // domain msg — safe
        } catch (Exception e) {
            logger.error("Error deactivating vendor {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to deactivate vendor"));
        }
    }

    @PostMapping("/purchase-orders")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> createPurchaseOrder(@RequestBody PurchaseOrderDTO dto) {
        try {
            PurchaseOrderDTO created = procurementService.createPurchaseOrder(dto);
            return ResponseEntity.ok(ApiResponse.success("Purchase order created successfully", created));
        } catch (Exception e) {
            logger.error("Error creating purchase order", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to create purchase order"));
        }
    }

    @GetMapping("/purchase-orders")
    public ResponseEntity<ApiResponse<Page<PurchaseOrderDTO>>> searchPurchaseOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        try {
            Sort.Direction direction = sort.length > 1 && sort[1].equalsIgnoreCase("asc")
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            String sortField = sort.length > 0 ? sort[0] : "createdAt";
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
            Page<PurchaseOrderDTO> orders = procurementService.searchPurchaseOrders(search, status, projectId, pageable);
            return ResponseEntity.ok(ApiResponse.success("Purchase orders retrieved successfully", orders));
        } catch (Exception e) {
            logger.error("Error fetching purchase orders", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to retrieve purchase orders"));
        }
    }

    @DeleteMapping("/purchase-orders/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePurchaseOrder(
            @PathVariable Long id,
            @RequestParam(required = false) Long deletedBy) {
        try {
            Long deletedByUserId = deletedBy != null ? deletedBy : 1L;
            procurementService.softDeletePurchaseOrder(id, deletedByUserId);
            return ResponseEntity.ok(ApiResponse.success("Purchase order deleted successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting purchase order {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to delete purchase order"));
        }
    }

    @PutMapping("/purchase-orders/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> updatePurchaseOrder(
            @PathVariable Long id,
            @RequestBody PurchaseOrderDTO dto) {
        try {
            PurchaseOrderDTO updated = procurementService.updatePurchaseOrder(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Purchase order updated successfully", updated));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating purchase order {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to update purchase order"));
        }
    }

    @PostMapping("/purchase-orders/{id}/cancel")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> cancelPurchaseOrder(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        try {
            String reason = payload.getOrDefault("reason", "No reason provided");
            PurchaseOrderDTO cancelled = procurementService.cancelPurchaseOrder(id, reason);
            return ResponseEntity.ok(ApiResponse.success("Purchase order cancelled successfully", cancelled));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error cancelling purchase order {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to cancel purchase order"));
        }
    }

    @PostMapping("/purchase-orders/{id}/close")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> closePurchaseOrder(@PathVariable Long id) {
        try {
            PurchaseOrderDTO closed = procurementService.closePurchaseOrder(id);
            return ResponseEntity.ok(ApiResponse.success("Purchase order closed successfully", closed));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error closing purchase order {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to close purchase order"));
        }
    }

    @PreAuthorize("hasAuthority('PROCUREMENT_APPROVE')")
    @PostMapping("/grn")
    public ResponseEntity<ApiResponse<GRNDTO>> recordGRN(@RequestBody GRNDTO dto) {
        try {
            GRNDTO recorded = procurementService.recordGRN(dto);
            return ResponseEntity.ok(ApiResponse.success("GRN recorded successfully", recorded));
        } catch (Exception e) {
            logger.error("Error recording GRN", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to record GRN"));
        }
    }

    @GetMapping("/grns")
    public ResponseEntity<ApiResponse<List<GRNDTO>>> getAllGRNs() {
        try {
            List<GRNDTO> grns = procurementService.getAllGRNs();
            return ResponseEntity.ok(ApiResponse.success("GRNs retrieved successfully", grns));
        } catch (Exception e) {
            logger.error("Error fetching GRNs", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to retrieve GRNs"));
        }
    }
}
