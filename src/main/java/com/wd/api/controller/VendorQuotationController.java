package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.VendorQuotationSearchFilter;
import com.wd.api.model.VendorQuotation;
import com.wd.api.service.VendorQuotationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procurement/quotations")
@RequiredArgsConstructor
@Slf4j
public class VendorQuotationController {

    private final VendorQuotationService quotationService;

    @GetMapping("/search")
    public ResponseEntity<Page<VendorQuotation>> searchVendorQuotations(@ModelAttribute VendorQuotationSearchFilter filter) {
        try {
            Page<VendorQuotation> quotations = quotationService.searchVendorQuotations(filter);
            return ResponseEntity.ok(quotations);
        } catch (Exception e) {
            log.error("Error searching vendor quotations", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/indent/{indentId}/vendor/{vendorId}")
    public ResponseEntity<ApiResponse<VendorQuotation>> createQuotation(
            @PathVariable Long indentId,
            @PathVariable Long vendorId,
            @RequestBody VendorQuotation quotation) {
        try {
            VendorQuotation created = quotationService.createQuotation(indentId, vendorId, quotation);
            return ResponseEntity.ok(ApiResponse.success("Quotation added successfully", created));
        } catch (Exception e) {
            log.error("Error creating quotation", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/indent/{indentId}")
    public ResponseEntity<ApiResponse<List<VendorQuotation>>> getQuotations(@PathVariable Long indentId) {
        try {
            List<VendorQuotation> list = quotationService.getQuotationsForIndent(indentId);
            return ResponseEntity.ok(ApiResponse.success("Quotations fetched", list));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/select")
    public ResponseEntity<ApiResponse<VendorQuotation>> selectQuotation(@PathVariable Long id) {
        try {
            VendorQuotation approved = quotationService.approveQuotation(id);
            return ResponseEntity.ok(ApiResponse.success("Quotation selected", approved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
