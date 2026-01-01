package com.wd.api.controller;

import com.wd.api.dto.GRNDTO;
import com.wd.api.dto.PurchaseOrderDTO;
import com.wd.api.dto.VendorDTO;
import com.wd.api.service.ProcurementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/procurement")
@RequiredArgsConstructor
public class ProcurementController {

    private final ProcurementService procurementService;

    @PostMapping("/vendors")
    public ResponseEntity<VendorDTO> createVendor(@RequestBody VendorDTO dto) {
        return ResponseEntity.ok(procurementService.createVendor(dto));
    }

    @GetMapping("/vendors")
    public ResponseEntity<List<VendorDTO>> getAllVendors() {
        return ResponseEntity.ok(procurementService.getAllVendors());
    }

    @PostMapping("/purchase-orders")
    public ResponseEntity<PurchaseOrderDTO> createPurchaseOrder(@RequestBody PurchaseOrderDTO dto) {
        return ResponseEntity.ok(procurementService.createPurchaseOrder(dto));
    }

    @PostMapping("/grn")
    public ResponseEntity<GRNDTO> recordGRN(@RequestBody GRNDTO dto) {
        return ResponseEntity.ok(procurementService.recordGRN(dto));
    }
}
