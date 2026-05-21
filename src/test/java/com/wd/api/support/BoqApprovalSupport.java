package com.wd.api.support;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;

/**
 * Test helper for the BOQ submit workflow.
 * <p>
 * As of the task-quality-gates change, {@code BoqDocumentService.submit} rejects a
 * document while any project BOQ item is still {@code DRAFT}. Integration tests that
 * create items via {@code POST /api/boq} and then submit the document must first
 * approve every item. This helper lists the project's items and approves any that
 * are still DRAFT, so callers can submit immediately afterwards.
 */
public final class BoqApprovalSupport {

    private BoqApprovalSupport() {}

    /**
     * Approves every still-DRAFT BOQ item in the given project.
     *
     * @param restTemplate the test REST client
     * @param urlPrefix    scheme+host+port, e.g. {@code "http://localhost:" + port}
     * @param headers      auth headers for a user holding {@code BOQ_APPROVE}
     * @param projectId    the project whose items should be approved
     */
    @SuppressWarnings("unchecked")
    public static void approveAllItems(TestRestTemplate restTemplate, String urlPrefix,
                                       HttpHeaders headers, Long projectId) {
        Map<String, Object> body = restTemplate.exchange(
                urlPrefix + "/api/boq/project/" + projectId + "?page=0&size=500",
                HttpMethod.GET, new HttpEntity<>(headers), Map.class).getBody();
        if (body == null) {
            return;
        }
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
        for (Map<String, Object> item : content) {
            Object status = item.get("status");
            if (status != null && !"DRAFT".equals(status)) {
                continue; // already APPROVED/LOCKED — leave as-is
            }
            Long id = ((Number) item.get("id")).longValue();
            restTemplate.exchange(urlPrefix + "/api/boq/" + id + "/approve",
                    HttpMethod.PATCH, new HttpEntity<>(headers), Map.class);
        }
    }
}
