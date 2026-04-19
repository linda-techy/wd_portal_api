package com.wd.api.module;

import com.wd.api.config.TestDataSeeder;
import com.wd.api.support.AuthTestHelper;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the BOQ module covering document lifecycle,
 * item CRUD, workflow transitions, financial summary, and Excel export.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BoqModuleTest extends TestcontainersPostgresBase {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TestDataSeeder seeder;

    AuthTestHelper auth;

    /** Shared state across ordered tests. */
    private static Long projectId;
    private static Long boqDocumentId;
    private static Long boqItemId;

    @BeforeEach
    void setUp() {
        seeder.seed();
        auth = new AuthTestHelper(restTemplate, port);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders adminHeaders() {
        String token = auth.loginAsAdmin();
        return auth.authHeaders(token);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractData(Map<String, Object> body) {
        return (Map<String, Object>) body.get("data");
    }

    /**
     * Creates a test project and stores its ID in the static field.
     * Called by the first test to set up the shared context.
     */
    private void ensureProjectExists() {
        if (projectId != null) return;

        HttpHeaders headers = adminHeaders();
        Map<String, Object> projectBody = new LinkedHashMap<>();
        projectBody.put("name", "BOQ Test Project");
        projectBody.put("location", "Bangalore");
        projectBody.put("project_type", "RESIDENTIAL");
        projectBody.put("state", "Karnataka");
        projectBody.put("district", "Bangalore Urban");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(projectBody, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/customer-projects"), HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> data = extractData(response.getBody());
        projectId = ((Number) data.get("id")).longValue();
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    void should_createBoqDocument() {
        ensureProjectExists();
        HttpHeaders headers = adminHeaders();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("projectId", projectId);
        body.put("gstRate", new BigDecimal("0.18"));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/boq-documents"), HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        boqDocumentId = ((Number) data.get("id")).longValue();
        assertThat(boqDocumentId).isPositive();
        assertThat(data.get("status")).isEqualTo("DRAFT");
    }

    @Test
    @Order(2)
    void should_addBoqItem() {
        ensureProjectExists();
        assertThat(boqDocumentId).as("BOQ document must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("projectId", projectId);
        body.put("description", "Concrete Foundation Work");
        body.put("unit", "CuM");
        body.put("quantity", new BigDecimal("50.00"));
        body.put("unitRate", new BigDecimal("5000.00"));
        body.put("itemKind", "BASE");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/boq"), HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        boqItemId = ((Number) data.get("id")).longValue();
        assertThat(boqItemId).isPositive();
        assertThat(data.get("description")).isEqualTo("Concrete Foundation Work");
    }

    @Test
    @Order(3)
    void should_listBoqItems() {
        ensureProjectExists();
        HttpHeaders headers = adminHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/boq/project/" + projectId),
                HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        // Paginated response has "content"
        assertThat(data.get("content")).isNotNull();
    }

    @Test
    @Order(4)
    void should_updateBoqItem() {
        assertThat(boqItemId).as("BOQ item must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "Updated Concrete Foundation");
        body.put("unitRate", new BigDecimal("5500.00"));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/boq/" + boqItemId),
                HttpMethod.PUT, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        assertThat(data.get("description")).isEqualTo("Updated Concrete Foundation");
    }

    @Test
    @Order(5)
    void should_submitBoqForApproval() {
        assertThat(boqDocumentId).as("BOQ document must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/boq-documents/" + boqDocumentId + "/submit"),
                HttpMethod.PATCH, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        assertThat(data.get("status")).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    @Order(6)
    void should_approveBoqInternally() {
        assertThat(boqDocumentId).as("BOQ document must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/boq-documents/" + boqDocumentId + "/approve-internal"),
                HttpMethod.PATCH, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        // After internal approval, status stays PENDING_APPROVAL until customer approves
        assertThat(data.get("status")).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    @Order(7)
    void should_customerApproveBoq() {
        assertThat(boqDocumentId).as("BOQ document must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        Long customerUserId = seeder.getCustomerA().getId();

        // Add customerA as project member so customer-approve's membership check passes
        Map<String, Object> memberBody = new LinkedHashMap<>();
        memberBody.put("customerUserId", customerUserId);
        memberBody.put("role", "CUSTOMER");
        restTemplate.exchange(
                baseUrl("/customer-projects/" + projectId + "/members"),
                HttpMethod.POST, new HttpEntity<>(memberBody, headers), Map.class);

        // Payment stage configuration (must total 100%)
        Map<String, Object> stage1 = new LinkedHashMap<>();
        stage1.put("name", "Foundation");
        stage1.put("percentage", new BigDecimal("0.40"));

        Map<String, Object> stage2 = new LinkedHashMap<>();
        stage2.put("name", "Structure");
        stage2.put("percentage", new BigDecimal("0.35"));

        Map<String, Object> stage3 = new LinkedHashMap<>();
        stage3.put("name", "Finishing");
        stage3.put("percentage", new BigDecimal("0.25"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customerSignedById", customerUserId);
        body.put("stages", List.of(stage1, stage2, stage3));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/boq-documents/" + boqDocumentId + "/customer-approve"),
                HttpMethod.PATCH, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        assertThat(data.get("status")).isEqualTo("APPROVED");
    }

    @Test
    @Order(8)
    void should_rejectModificationOfApprovedBoq() {
        // After customer approval, BOQ items linked to the approved document
        // should not be editable (they are LOCKED).
        // The item created in test 2 is a standalone item (not linked to the document),
        // so we test the document-level lock by checking status.
        assertThat(boqDocumentId).as("BOQ document must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        // Verify the document is now APPROVED (locked)
        HttpEntity<Void> getEntity = new HttpEntity<>(headers);
        ResponseEntity<Map> getResponse = restTemplate.exchange(
                baseUrl("/api/boq-documents/" + boqDocumentId),
                HttpMethod.GET, getEntity, Map.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> docData = extractData(getResponse.getBody());
        assertThat(docData.get("status")).isEqualTo("APPROVED");

        // Attempt to submit again (should fail since it's already approved)
        HttpEntity<Void> submitEntity = new HttpEntity<>(headers);
        ResponseEntity<Map> submitResponse = restTemplate.exchange(
                baseUrl("/api/boq-documents/" + boqDocumentId + "/submit"),
                HttpMethod.PATCH, submitEntity, Map.class);

        // Should be rejected -- either 400 (bad request due to wrong status) or 403
        assertThat(submitResponse.getStatusCode().value()).isIn(400, 403);
    }

    @Test
    @Order(9)
    void should_getFinancialSummary() {
        ensureProjectExists();
        HttpHeaders headers = adminHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/boq/project/" + projectId + "/financial-summary"),
                HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        assertThat(data.get("projectId")).isNotNull();
    }

    @Test
    @Order(10)
    void should_exportBoqToExcel() {
        ensureProjectExists();
        HttpHeaders headers = adminHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                baseUrl("/api/boq/project/" + projectId + "/export"),
                HttpMethod.GET, entity, byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isPositive();
        // XLSX files start with the PK magic bytes (0x50, 0x4B)
        assertThat(response.getBody()[0]).isEqualTo((byte) 0x50);
        assertThat(response.getBody()[1]).isEqualTo((byte) 0x4B);
    }
}
