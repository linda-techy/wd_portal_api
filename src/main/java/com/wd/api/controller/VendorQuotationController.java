package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.model.VendorQuotation;
import com.wd.api.service.VendorQuotationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procurement/quotations")
@RequiredArgsConstructor
@Slf4j
public class VendorQuotationController {

    private final VendorQuotationService quotationService;

    @PostMapping("/indent/{indentId}/vendor/{vendorId}")
    public ResponseEntity<ApiResponse<VendorQuotation>> createQuotation(
            @PathVariable Long indentId,
            @PathVariable Long vendorId,
            @RequestBody VendorQuotation quotation) {
        try {
            VendorQuotation created = quotationService.createQuotation(indentId, vendorId, quotation);
            return ResponseEntity.ok(new ApiResponse<>(true, "Quotation added successfully", created));
        } catch (Exception e) {
            log.error("Error creating quotation", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/indent/{indentId}")
    public ResponseEntity<ApiResponse<List<VendorQuotation>>> getQuotations(@PathVariable Long indentId) {
        try {
            List<VendorQuotation> list = quotationService.getQuotationsForIndent(indentId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Quotations fetched", list));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/{id}/select")
    public ResponseEntity<ApiResponse<VendorQuotation>> selectQuotation(@PathVariable Long id) {
        try {
            VendorQuotation approved = quotationService.approveQuotation(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "Quotation selected", approved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
