package com.wd.api.controller;

import com.wd.api.dto.ChallanDtos;
import com.wd.api.model.PaymentChallan;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.service.ChallanService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/challans")
@PreAuthorize("isAuthenticated()")
public class ChallanController {

    private final ChallanService challanService;

    private final PortalUserRepository portalUserRepository;

    public ChallanController(ChallanService challanService,
            PortalUserRepository portalUserRepository) {
        this.challanService = challanService;
        this.portalUserRepository = portalUserRepository;
    }

    @PostMapping("/generate/{transactionId}")
    @PreAuthorize("hasAuthority('CHALLAN_CREATE')")
    public ResponseEntity<ChallanDtos.ChallanResponse> generateChallan(
            @PathVariable Long transactionId,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        PaymentChallan challan = challanService.generateChallan(transactionId, userId);
        ChallanDtos.ChallanFilterRequest filter = new ChallanDtos.ChallanFilterRequest();
        filter.setIds(List.of(challan.getId()));
        return ResponseEntity.ok(challanService.searchChallans(filter).get(0));
    }

    @PostMapping("/search")
    @PreAuthorize("hasAuthority('CHALLAN_VIEW')")
    public ResponseEntity<List<ChallanDtos.ChallanResponse>> searchChallans(
            @RequestBody ChallanDtos.ChallanFilterRequest filter) {
        return ResponseEntity.ok(challanService.searchChallans(filter));
    }

    @GetMapping("/download/{id}")
    @PreAuthorize("hasAuthority('CHALLAN_DOWNLOAD')")
    public ResponseEntity<byte[]> downloadChallan(@PathVariable Long id) {
        byte[] pdf = challanService.generateChallanPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("Challan_" + id + ".pdf").build());

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @PostMapping("/bulk-download")
    @PreAuthorize("hasAuthority('CHALLAN_DOWNLOAD')")
    public ResponseEntity<byte[]> bulkDownload(@RequestBody List<Long> ids) {
        byte[] zip = challanService.generateBulkZip(ids);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/zip"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("Walldot_Challans_Bulk.zip").build());

        return new ResponseEntity<>(zip, headers, HttpStatus.OK);
    }

    private Long getCurrentUserId(Authentication auth) {
        String email = auth.getName();
        PortalUser user = portalUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }
}
