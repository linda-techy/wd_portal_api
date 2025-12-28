package com.wd.api.controller;

import com.wd.api.dto.ChallanDtos;
import com.wd.api.model.PaymentChallan;
import com.wd.api.service.ChallanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/challans")
public class ChallanController {

    @Autowired
    private ChallanService challanService;

    @PostMapping("/generate/{transactionId}")
    public ResponseEntity<ChallanDtos.ChallanResponse> generateChallan(
            @PathVariable Long transactionId,
            @RequestAttribute("userId") Long userId) {
        PaymentChallan challan = challanService.generateChallan(transactionId, userId);
        return ResponseEntity.ok(challanService.searchChallans(new ChallanDtos.ChallanFilterRequest() {
            {
                setIds(List.of(challan.getId()));
            }
        }).get(0));
    }

    @PostMapping("/search")
    public ResponseEntity<List<ChallanDtos.ChallanResponse>> searchChallans(
            @RequestBody ChallanDtos.ChallanFilterRequest filter) {
        return ResponseEntity.ok(challanService.searchChallans(filter));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadChallan(@PathVariable Long id) {
        byte[] pdf = challanService.generateChallanPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("Challan_" + id + ".pdf").build());

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @PostMapping("/bulk-download")
    public ResponseEntity<byte[]> bulkDownload(@RequestBody List<Long> ids) {
        byte[] zip = challanService.generateBulkZip(ids);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/zip"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("Walldot_Challans_Bulk.zip").build());

        return new ResponseEntity<>(zip, headers, HttpStatus.OK);
    }
}
