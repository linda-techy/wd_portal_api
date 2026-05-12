package com.wd.api.ai;

import com.wd.api.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI-assisted BOQ item suggester. Takes a free-text description from the
 * site engineer or estimator and returns a normalised BOQ item draft —
 * description, unit, HSN/SAC code (the field the backend now requires per
 * G-21), and ItemKind.
 *
 * <p>Permission: {@code BOQ_CREATE} — same right as actually adding the item.
 */
@RestController
@RequestMapping("/api/ai/boq")
@PreAuthorize("hasAuthority('BOQ_CREATE')")
public class BoqSuggesterController {

    private final BoqSuggesterService service;

    public BoqSuggesterController(BoqSuggesterService service) {
        this.service = service;
    }

    @PostMapping("/suggest")
    public ResponseEntity<ApiResponse<BoqSuggesterResponse>> suggest(
            @Valid @RequestBody BoqSuggesterRequest request) {
        BoqSuggesterResponse response = service.suggest(request);
        return ResponseEntity.ok(ApiResponse.success("BOQ suggestion generated", response));
    }
}
