package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.ApprovalRequestDTO;
import com.wd.api.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalController.class);
    private final ApprovalService approvalService;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<ApprovalRequestDTO>> createRequest(@RequestBody ApprovalRequestDTO dto) {
        try {
            ApprovalRequestDTO created = approvalService.createRequest(dto);
            return ResponseEntity.ok(ApiResponse.success("Approval request created successfully", created));
        } catch (Exception e) {
            logger.error("Error creating approval request", e);
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/process/{requestId}")
    public ResponseEntity<ApiResponse<ApprovalRequestDTO>> processRequest(
            @PathVariable Long requestId,
            @RequestParam String status,
            @RequestParam(required = false) String comments,
            @RequestParam Long approverId) {
        try {
            ApprovalRequestDTO processed = approvalService.processRequest(requestId, status, comments, approverId);
            return ResponseEntity.ok(ApiResponse.success("Approval request processed successfully", processed));
        } catch (Exception e) {
            logger.error("Error processing approval request", e);
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/pending/{approverId}")
    public ResponseEntity<ApiResponse<List<ApprovalRequestDTO>>> getPendingApprovals(@PathVariable Long approverId) {
        try {
            List<ApprovalRequestDTO> pending = approvalService.getPendingApprovals(approverId);
            return ResponseEntity.ok(ApiResponse.success("Pending approvals retrieved successfully", pending));
        } catch (Exception e) {
            logger.error("Error fetching pending approvals", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }
}
