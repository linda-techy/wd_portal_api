package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.GRNDTO;
import com.wd.api.dto.PurchaseOrderDTO;
import com.wd.api.dto.VendorDTO;
import com.wd.api.service.ProcurementService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/vendors")
    public ResponseEntity<ApiResponse<List<VendorDTO>>> getAllVendors() {
        try {
            List<VendorDTO> vendors = procurementService.getAllVendors();
            return ResponseEntity.ok(ApiResponse.success("Vendors retrieved successfully", vendors));
        } catch (Exception e) {
            logger.error("Error fetching vendors", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @PostMapping("/purchase-orders")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> createPurchaseOrder(@RequestBody PurchaseOrderDTO dto) {
        try {
            PurchaseOrderDTO created = procurementService.createPurchaseOrder(dto);
            return ResponseEntity.ok(ApiResponse.success("Purchase order created successfully", created));
        } catch (Exception e) {
            logger.error("Error creating purchase order", e);
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/grn")
    public ResponseEntity<ApiResponse<GRNDTO>> recordGRN(@RequestBody GRNDTO dto) {
        try {
            GRNDTO recorded = procurementService.recordGRN(dto);
            return ResponseEntity.ok(ApiResponse.success("GRN recorded successfully", recorded));
        } catch (Exception e) {
            logger.error("Error recording GRN", e);
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }
}
