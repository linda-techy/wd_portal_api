package com.wd.api.controller;

import com.wd.api.dto.ApprovalRequestDTO;
import com.wd.api.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @PostMapping("/request")
    public ResponseEntity<ApprovalRequestDTO> createRequest(@RequestBody ApprovalRequestDTO dto) {
        return ResponseEntity.ok(approvalService.createRequest(dto));
    }

    @PostMapping("/process/{requestId}")
    public ResponseEntity<ApprovalRequestDTO> processRequest(
            @PathVariable Long requestId,
            @RequestParam String status,
            @RequestParam(required = false) String comments,
            @RequestParam Long approverId) {
        return ResponseEntity.ok(approvalService.processRequest(requestId, status, comments, approverId));
    }

    @GetMapping("/pending/{approverId}")
    public ResponseEntity<List<ApprovalRequestDTO>> getPendingApprovals(@PathVariable Long approverId) {
        return ResponseEntity.ok(approvalService.getPendingApprovals(approverId));
    }
}
