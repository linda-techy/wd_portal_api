package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.dpc.CreateScopeOptionRequest;
import com.wd.api.dto.dpc.DpcScopeOptionDto;
import com.wd.api.dto.dpc.DpcScopeTemplateDto;
import com.wd.api.dto.dpc.ReorderScopeOptionsRequest;
import com.wd.api.dto.dpc.UpdateScopeOptionRequest;
import com.wd.api.dto.dpc.UpdateScopeTemplateRequest;
import com.wd.api.service.dpc.DpcScopeTemplateService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for the company-level DPC scope-template library.
 *
 * <p>Read endpoints expose the catalog used by the DPC builder (one row per
 * scope topic, each with its "options considered" cards). Write endpoints
 * are gated by {@code DPC_TEMPLATE_MANAGE} so that only template-admins can
 * patch the narrative content; everyone else can read.
 *
 * <p>Exception mapping mirrors {@link DpcDocumentController}:
 * {@code IllegalStateException} -> 422, {@code IllegalArgumentException}
 * -> 400, anything else -> 500.
 */
@RestController
@RequestMapping("/api/dpc-scope-templates")
@PreAuthorize("isAuthenticated()")
public class DpcScopeTemplateController {

    private static final Logger logger = LoggerFactory.getLogger(DpcScopeTemplateController.class);

    private final DpcScopeTemplateService dpcScopeTemplateService;

    public DpcScopeTemplateController(DpcScopeTemplateService dpcScopeTemplateService) {
        this.dpcScopeTemplateService = dpcScopeTemplateService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DPC_VIEW')")
    public ResponseEntity<ApiResponse<List<DpcScopeTemplateDto>>> listAll() {
        try {
            List<DpcScopeTemplateDto> dtos = dpcScopeTemplateService.listAll();
            return ResponseEntity.ok(ApiResponse.success("DPC scope templates retrieved", dtos));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to list DPC scope templates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while listing scope templates"));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DPC_VIEW')")
    public ResponseEntity<ApiResponse<DpcScopeTemplateDto>> getById(@PathVariable Long id) {
        try {
            DpcScopeTemplateDto dto = dpcScopeTemplateService.getById(id);
            return ResponseEntity.ok(ApiResponse.success("DPC scope template retrieved", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch DPC scope template {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while fetching the scope template"));
        }
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('DPC_TEMPLATE_MANAGE')")
    public ResponseEntity<ApiResponse<DpcScopeTemplateDto>> update(
            @PathVariable Long id,
            @RequestBody UpdateScopeTemplateRequest request) {
        try {
            DpcScopeTemplateDto dto = dpcScopeTemplateService.update(id, request);
            return ResponseEntity.ok(ApiResponse.success("DPC scope template updated", dto));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update DPC scope template {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while updating the scope template"));
        }
    }

    // ---- Option CRUD --------------------------------------------------------

    @PostMapping("/{templateId}/options")
    @PreAuthorize("hasAuthority('DPC_TEMPLATE_MANAGE')")
    public ResponseEntity<ApiResponse<DpcScopeOptionDto>> addOption(
            @PathVariable Long templateId,
            @Valid @RequestBody CreateScopeOptionRequest request) {
        try {
            DpcScopeOptionDto dto = dpcScopeTemplateService.addOption(templateId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Scope option added", dto));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to add scope option to template {}", templateId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while adding the option"));
        }
    }

    @PatchMapping("/options/{optionId}")
    @PreAuthorize("hasAuthority('DPC_TEMPLATE_MANAGE')")
    public ResponseEntity<ApiResponse<DpcScopeOptionDto>> updateOption(
            @PathVariable Long optionId,
            @Valid @RequestBody UpdateScopeOptionRequest request) {
        try {
            DpcScopeOptionDto dto = dpcScopeTemplateService.updateOption(optionId, request);
            return ResponseEntity.ok(ApiResponse.success("Scope option updated", dto));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update scope option {}", optionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while updating the option"));
        }
    }

    @DeleteMapping("/options/{optionId}")
    @PreAuthorize("hasAuthority('DPC_TEMPLATE_MANAGE')")
    public ResponseEntity<ApiResponse<String>> deleteOption(@PathVariable Long optionId) {
        try {
            dpcScopeTemplateService.softDeleteOption(optionId);
            return ResponseEntity.ok(ApiResponse.success("Scope option deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to delete scope option {}", optionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while deleting the option"));
        }
    }

    @PatchMapping("/{templateId}/options/reorder")
    @PreAuthorize("hasAuthority('DPC_TEMPLATE_MANAGE')")
    public ResponseEntity<ApiResponse<List<DpcScopeOptionDto>>> reorderOptions(
            @PathVariable Long templateId,
            @Valid @RequestBody ReorderScopeOptionsRequest request) {
        try {
            List<DpcScopeOptionDto> dtos =
                    dpcScopeTemplateService.reorderOptions(templateId, request.orderedOptionIds());
            return ResponseEntity.ok(ApiResponse.success("Scope options reordered", dtos));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to reorder options for template {}", templateId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while reordering options"));
        }
    }
}
