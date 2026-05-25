package com.wd.api.module;

import com.wd.api.config.TestDataSeeder;
import com.wd.api.support.AuthTestHelper;
import com.wd.api.support.BoqApprovalSupport;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REGRESSION-LOCK — audit Card 4.4 / Method-2 core guarantee.
 *
 * <p>Once a project's BOQ document reaches APPROVED status, the system MUST block
 * creation of a second BOQ document. All scope changes after that point must travel
 * through the Change Order workflow.
 *
 * <p>Invariant: {@code BoqDocumentService.createDocument} throws
 * {@link IllegalStateException} with message starting
 * {@code "Project <id> already has an approved BOQ."} when
 * {@code boqDocumentRepository.existsByProjectIdAndStatus(projectId, APPROVED)} is
 * {@code true}.
 *
 * <p>This test characterises the current production behaviour; it must NEVER be
 * weakened to accommodate a regression in the guard.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BoqMethod2GuaranteeTest extends TestcontainersPostgresBase {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TestDataSeeder seeder;

    AuthTestHelper auth;

    @BeforeEach
    void setUp() {
        seeder.seed();
        auth = new AuthTestHelper(restTemplate, port);
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders adminHeaders() {
        return auth.authHeaders(auth.loginAsAdmin());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractData(Map<String, Object> body) {
        return (Map<String, Object>) body.get("data");
    }

    /**
     * LOCK: a second {@code POST /api/boq-documents} on a project that already owns
     * an APPROVED BOQ document must be rejected.
     *
     * <p>Setup path (cheapest available):
     * <ol>
     *   <li>Create a fresh project with admin team.</li>
     *   <li>POST /api/boq-documents  → DRAFT document.</li>
     *   <li>POST /api/boq            → one BOQ item.</li>
     *   <li>PATCH /api/boq/{id}/approve → approve the item (required before submit).</li>
     *   <li>PATCH /api/boq-documents/{id}/submit → PENDING_INTERNAL_APPROVAL.</li>
     *   <li>PATCH /api/boq-documents/{id}/approve-internal → PENDING_CUSTOMER_APPROVAL.</li>
     *   <li>PATCH /api/boq-documents/{id}/customer-approve → APPROVED.</li>
     *   <li>POST /api/boq-documents again → must be blocked.</li>
     * </ol>
     */
    @Test
    void createBoqDocument_whenProjectAlreadyHasApprovedBoq_isBlocked() {
        HttpHeaders headers = adminHeaders();

        // 1. Fresh isolated project
        Long projectId = seeder.createFreshProjectWithTeam("RENOVATION", seeder.getCustomerC()).getId();

        // 2. Create first BOQ document
        Map<String, Object> docBody = new LinkedHashMap<>();
        docBody.put("projectId", projectId);
        docBody.put("gstRate", new BigDecimal("0.00"));

        ResponseEntity<Map> createDoc = restTemplate.exchange(
                baseUrl("/api/boq-documents"), HttpMethod.POST,
                new HttpEntity<>(docBody, headers), Map.class);
        assertThat(createDoc.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long boqDocumentId = ((Number) extractData(createDoc.getBody()).get("id")).longValue();

        // 3. Create one BOQ item
        Map<String, Object> itemBody = new LinkedHashMap<>();
        itemBody.put("projectId", projectId);
        itemBody.put("hsnSacCode", "995411");
        itemBody.put("description", "Lock-test item");
        itemBody.put("unit", "nos");
        itemBody.put("quantity", new BigDecimal("10"));
        itemBody.put("unitRate", new BigDecimal("100.00"));
        itemBody.put("itemKind", "BASE");

        ResponseEntity<Map> createItem = restTemplate.exchange(
                baseUrl("/api/boq"), HttpMethod.POST,
                new HttpEntity<>(itemBody, headers), Map.class);
        assertThat(createItem.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 4. Approve all DRAFT items (required by submit guard)
        BoqApprovalSupport.approveAllItems(restTemplate, "http://localhost:" + port, headers, projectId);

        // 5. Submit for approval
        ResponseEntity<Map> submit = restTemplate.exchange(
                baseUrl("/api/boq-documents/" + boqDocumentId + "/submit"),
                HttpMethod.PATCH, new HttpEntity<>(headers), Map.class);
        assertThat(submit.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 6. Internal approval
        ResponseEntity<Map> approveInternal = restTemplate.exchange(
                baseUrl("/api/boq-documents/" + boqDocumentId + "/approve-internal"),
                HttpMethod.PATCH, new HttpEntity<>(headers), Map.class);
        assertThat(approveInternal.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 7. Customer approval (portal side)
        Long customerUserId = seeder.getCustomerC().getId();
        Map<String, Object> stage = new LinkedHashMap<>();
        stage.put("name", "Completion");
        stage.put("percentage", new BigDecimal("1.00"));
        Map<String, Object> approveBody = new LinkedHashMap<>();
        approveBody.put("customerSignedById", customerUserId);
        approveBody.put("stages", java.util.List.of(stage));

        ResponseEntity<Map> customerApprove = restTemplate.exchange(
                baseUrl("/api/boq-documents/" + boqDocumentId + "/customer-approve"),
                HttpMethod.PATCH, new HttpEntity<>(approveBody, headers), Map.class);
        assertThat(customerApprove.getStatusCode()).isEqualTo(HttpStatus.OK);

        // --- LOCK ASSERTION ---
        // 8. Attempt to create a SECOND BOQ document on the same project.
        //    BoqDocumentService.createDocument throws IllegalStateException:
        //    "Project <id> already has an approved BOQ. All scope changes must be submitted as Change Orders."
        //    The controller must propagate this as a non-2xx error response.
        Map<String, Object> doc2Body = new LinkedHashMap<>();
        doc2Body.put("projectId", projectId);
        doc2Body.put("gstRate", new BigDecimal("0.00"));

        ResponseEntity<Map> blocked = restTemplate.exchange(
                baseUrl("/api/boq-documents"), HttpMethod.POST,
                new HttpEntity<>(doc2Body, headers), Map.class);

        // The guard in BoqDocumentService throws IllegalStateException; the controller
        // must not return 200 or 201. Any 4xx/5xx is evidence the block is active.
        assertThat(blocked.getStatusCode().is2xxSuccessful())
                .as("Second BOQ document creation must be BLOCKED once project has an APPROVED BOQ. "
                        + "Expected non-2xx but got: " + blocked.getStatusCode()
                        + " body: " + blocked.getBody())
                .isFalse();

        // Additionally verify the response body carries the failure signal.
        Map<String, Object> blockedBody = blocked.getBody();
        if (blockedBody != null && blockedBody.containsKey("success")) {
            assertThat(blockedBody.get("success"))
                    .as("Response 'success' field must be false for blocked second BOQ")
                    .isEqualTo(false);
        }

        // Verify the message fragment that BoqDocumentService embeds.
        // The service message is:
        //   "Project <id> already has an approved BOQ. All scope changes must be submitted as Change Orders."
        if (blockedBody != null && blockedBody.containsKey("message")) {
            String msg = String.valueOf(blockedBody.get("message"));
            assertThat(msg)
                    .as("Error message must mention the approved-BOQ guard")
                    .containsIgnoringCase("already has an approved BOQ");
        }
    }
}
