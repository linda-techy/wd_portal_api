package com.wd.api.scenario;

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
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full residential project lifecycle scenario test.
 * <p>
 * Exercises the complete happy-path from project creation through BOQ approval,
 * staged invoicing with payment confirmation, site reporting, and quality checks.
 * Uses multiple roles (Admin, PM, Accounts, Engineer) to verify role-based access.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResidentialScenarioTest extends TestcontainersPostgresBase {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TestDataSeeder seeder;

    AuthTestHelper auth;

    // Shared state across ordered test methods
    private Long projectId;
    private Long boqDocumentId;
    private Long stage1Id;
    private Long stage2Id;
    private Long stage3Id;
    private Long invoice1Id;
    private Long invoice2Id;

    @BeforeAll
    void setUpOnce() {
        seeder.seed();
        AuthTestHelper.clearTokenCache();
    }

    @BeforeEach
    void setUp() {
        auth = new AuthTestHelper(restTemplate, port);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders headersFor(String token) {
        return auth.authHeaders(token);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractData(Map<String, Object> body) {
        return (Map<String, Object>) body.get("data");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractDataList(Map<String, Object> body) {
        return (List<Map<String, Object>>) body.get("data");
    }

    // ------------------------------------------------------------------
    // Step 1: Create RESIDENTIAL project (as Admin)
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    void step01_createResidentialProject() {
        HttpHeaders headers = headersFor(auth.loginAsAdmin());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Lakeside Villa - 3BHK");
        body.put("location", "Whitefield, Bangalore");
        body.put("project_type", "RESIDENTIAL");
        body.put("startDate", LocalDate.now().toString());
        body.put("endDate", LocalDate.now().plusMonths(12).toString());
        body.put("state", "Karnataka");
        body.put("district", "Bangalore Urban");
        body.put("budget", new BigDecimal("5000000.00"));
        body.put("sqfeet", 2400);
        body.put("contractType", "FIXED_PRICE");

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/customer-projects"), HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        projectId = ((Number) data.get("id")).longValue();
        assertThat(projectId).isPositive();
        assertThat(data.get("name")).isEqualTo("Lakeside Villa - 3BHK");
    }

    // ------------------------------------------------------------------
    // Step 2: Create BOQ document (as PM)
    // ------------------------------------------------------------------

    @Test
    @Order(2)
    void step02_createBoqDocument() {
        assertThat(projectId).as("Project must be created first").isNotNull();

        HttpHeaders headers = headersFor(auth.loginAsPM());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("projectId", projectId);
        body.put("gstRate", new BigDecimal("0.18"));

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/boq-documents"), HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> data = extractData(response.getBody());
        boqDocumentId = ((Number) data.get("id")).longValue();
        assertThat(boqDocumentId).isPositive();
        assertThat(data.get("status")).isEqualTo("DRAFT");
    }

    // ------------------------------------------------------------------
    // Step 3: Add 5 BOQ items (structure, electrical, plumbing, painting, finishing)
    // ------------------------------------------------------------------

    @Test
    @Order(3)
    void step03_addBoqItems() {
        assertThat(projectId).as("Project must be created first").isNotNull();

        HttpHeaders headers = headersFor(auth.loginAsPM());

        Object[][] items = {
                {"RCC Structure Work", "CuM", "120.00", "8500.00"},
                {"Electrical Wiring & Fittings", "LS", "1.00", "350000.00"},
                {"Plumbing & Sanitary Work", "LS", "1.00", "280000.00"},
                {"Interior & Exterior Painting", "SqM", "450.00", "180.00"},
                {"Finishing & Fixtures", "LS", "1.00", "420000.00"}
        };

        for (Object[] item : items) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("projectId", projectId);
            body.put("description", item[0]);
            body.put("unit", item[1]);
            body.put("quantity", new BigDecimal((String) item[2]));
            body.put("unitRate", new BigDecimal((String) item[3]));
            body.put("itemKind", "BASE");

            ResponseEntity<Map> response = restTemplate.exchange(
                    url("/api/boq"), HttpMethod.POST,
                    new HttpEntity<>(body, headers), Map.class);

            assertThat(response.getStatusCode())
                    .as("Failed to create BOQ item: " + item[0])
                    .isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().get("success")).isEqualTo(true);

            Map<String, Object> data = extractData(response.getBody());
            assertThat(((Number) data.get("id")).longValue()).isPositive();
        }
    }

    // ------------------------------------------------------------------
    // Step 4: Submit BOQ for approval
    // ------------------------------------------------------------------

    @Test
    @Order(4)
    void step04_submitBoq() {
        assertThat(boqDocumentId).as("BOQ document must exist").isNotNull();

        HttpHeaders headers = headersFor(auth.loginAsPM());

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/boq-documents/" + boqDocumentId + "/submit"),
                HttpMethod.PATCH, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = extractData(response.getBody());
        assertThat(data.get("status")).isEqualTo("PENDING_APPROVAL");
    }

    // ------------------------------------------------------------------
    // Step 5: Internal approval (as Admin)
    // ------------------------------------------------------------------

    @Test
    @Order(5)
    void step05_internalApproval() {
        assertThat(boqDocumentId).as("BOQ document must exist").isNotNull();

        HttpHeaders headers = headersFor(auth.loginAsAdmin());

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/boq-documents/" + boqDocumentId + "/approve-internal"),
                HttpMethod.PATCH, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = extractData(response.getBody());
        // After approve-internal, BOQ status stays PENDING_APPROVAL until customer approves
        assertThat(data.get("status")).isEqualTo("PENDING_APPROVAL");
    }

    // ------------------------------------------------------------------
    // Step 6: Customer approve BOQ with 3 payment stages (40%/35%/25%)
    // ------------------------------------------------------------------

    @Test
    @Order(6)
    void step06_customerApproveWithStages() {
        assertThat(boqDocumentId).as("BOQ document must exist").isNotNull();

        HttpHeaders headers = headersFor(auth.loginAsAdmin());
        Long customerUserId = seeder.getCustomerA().getId();

        // Add customerA as project member (required for verifyCustomerMembership)
        Map<String, Object> memberBody = new LinkedHashMap<>();
        memberBody.put("customerUserId", customerUserId);
        memberBody.put("role", "CUSTOMER");
        restTemplate.exchange(
                url("/customer-projects/" + projectId + "/members"),
                HttpMethod.POST, new HttpEntity<>(memberBody, headers), Map.class);

        Map<String, Object> stage1 = new LinkedHashMap<>();
        stage1.put("name", "Foundation & Structure");
        stage1.put("percentage", new BigDecimal("0.40"));

        Map<String, Object> stage2 = new LinkedHashMap<>();
        stage2.put("name", "MEP & Interior");
        stage2.put("percentage", new BigDecimal("0.35"));

        Map<String, Object> stage3 = new LinkedHashMap<>();
        stage3.put("name", "Finishing & Handover");
        stage3.put("percentage", new BigDecimal("0.25"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customerSignedById", customerUserId);
        body.put("stages", List.of(stage1, stage2, stage3));

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/boq-documents/" + boqDocumentId + "/customer-approve"),
                HttpMethod.PATCH, new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = extractData(response.getBody());
        assertThat(data.get("status")).isEqualTo("APPROVED");
    }

    // ------------------------------------------------------------------
    // Step 7: Get payment stages, extract stage IDs
    // ------------------------------------------------------------------

    @Test
    @Order(7)
    void step07_getPaymentStages() {
        assertThat(boqDocumentId).as("BOQ document must exist").isNotNull();

        HttpHeaders headers = headersFor(auth.loginAsAdmin());

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/boq-documents/" + boqDocumentId + "/payment-stages"),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<Map<String, Object>> stages = extractDataList(response.getBody());
        assertThat(stages).hasSize(3);

        stage1Id = ((Number) stages.get(0).get("id")).longValue();
        stage2Id = ((Number) stages.get(1).get("id")).longValue();
        stage3Id = ((Number) stages.get(2).get("id")).longValue();

        assertThat(stage1Id).isPositive();
        assertThat(stage2Id).isPositive();
        assertThat(stage3Id).isPositive();
    }

    // ------------------------------------------------------------------
    // Step 8: Raise invoice for Stage 1 (as Accounts)
    // ------------------------------------------------------------------

    @Test
    @Order(8)
    void step08_raiseInvoiceStage1() {
        assertThat(stage1Id).as("Stage 1 ID must be captured").isNotNull();

        HttpHeaders headers = headersFor(auth.loginAsAccounts());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dueDate", LocalDate.now().plusDays(15).toString());

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/boq-invoices/stage/" + stage1Id + "/raise"),
                HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> data = extractData(response.getBody());
        invoice1Id = ((Number) data.get("id")).longValue();
        assertThat(invoice1Id).isPositive();
    }

    // ------------------------------------------------------------------
    // Step 9: Send invoice
    // ------------------------------------------------------------------

    @Test
    @Order(9)
    void step09_sendInvoice1() {
        assertThat(invoice1Id).as("Invoice 1 must be raised").isNotNull();

        HttpHeaders headers = headersFor(auth.loginAsAccounts());

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/boq-invoices/" + invoice1Id + "/send"),
                HttpMethod.PATCH, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    // ------------------------------------------------------------------
    // Step 10: Confirm payment for invoice
    // ------------------------------------------------------------------

    @Test
    @Order(10)
    void step10_confirmPaymentInvoice1() {
        assertThat(invoice1Id).as("Invoice 1 must be raised").isNotNull();

        HttpHeaders headers = headersFor(auth.loginAsAccounts());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentReference", "NEFT-2026-RES-001");
        body.put("paymentDate", LocalDate.now().toString());
        body.put("paymentMethod", "BANK_TRANSFER");

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/boq-invoices/" + invoice1Id + "/confirm-payment"),
                HttpMethod.PATCH, new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    // ------------------------------------------------------------------
    // Step 11: Create site report (as Engineer)
    // ------------------------------------------------------------------

    @Test
    @Order(11)
    void step11_createSiteReport() {
        assertThat(projectId).as("Project must exist").isNotNull();

        HttpHeaders headers = headersFor(auth.loginAsEngineer());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Site report creation requires multipart/form-data with a photo.
        // We send a minimal 1x1 PNG as the required photo attachment.
        org.springframework.util.LinkedMultiValueMap<String, Object> formData =
                new org.springframework.util.LinkedMultiValueMap<>();

        String reportJson = String.format(
                "{\"projectId\":%d,\"title\":\"Foundation inspection\","
                        + "\"description\":\"Foundation work completed for Block A\","
                        + "\"weather\":\"Clear\",\"manpowerDeployed\":12}",
                projectId);
        formData.add("report", new org.springframework.http.HttpEntity<>(
                reportJson, createJsonPartHeaders()));

        // Minimal valid PNG (1x1 pixel, transparent)
        byte[] minimalPng = {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,         // IHDR chunk
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,         // 1x1
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, // RGBA, filters
                (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, // IDAT chunk
                0x54, 0x78, (byte) 0x9C, 0x62, 0x00, 0x00, 0x00, 0x02,
                0x00, 0x01, (byte) 0xE5, 0x27, (byte) 0xDE, (byte) 0xFC,
                0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,         // IEND chunk
                (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };

        org.springframework.core.io.ByteArrayResource photoResource =
                new org.springframework.core.io.ByteArrayResource(minimalPng) {
                    @Override
                    public String getFilename() {
                        return "site-photo.png";
                    }
                };

        HttpHeaders photoPartHeaders = new HttpHeaders();
        photoPartHeaders.setContentType(MediaType.IMAGE_PNG);
        formData.add("photos", new org.springframework.http.HttpEntity<>(photoResource, photoPartHeaders));

        HttpHeaders multipartHeaders = headersFor(auth.loginAsEngineer());
        multipartHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/site-reports"), HttpMethod.POST,
                new HttpEntity<>(formData, multipartHeaders), Map.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    private HttpHeaders createJsonPartHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // ------------------------------------------------------------------
    // Step 12: Create quality check
    // ------------------------------------------------------------------

    @Test
    @Order(12)
    void step12_createQualityCheck() {
        assertThat(projectId).as("Project must exist").isNotNull();

        HttpHeaders headers = headersFor(auth.loginAsEngineer());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("projectId", projectId);
        body.put("title", "Foundation concrete cube test");
        body.put("description", "Testing 7-day cube strength for M25 grade concrete");
        body.put("status", "COMPLETED");
        body.put("result", "PASS");
        body.put("remarks", "Achieved 28.5 N/mm2, above minimum 25 N/mm2");

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/quality-checks"), HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        assertThat(((Number) data.get("id")).longValue()).isPositive();
    }

    // ------------------------------------------------------------------
    // Step 13: Raise invoice for Stage 2
    // ------------------------------------------------------------------

    @Test
    @Order(13)
    void step13_raiseInvoiceStage2() {
        assertThat(stage2Id).as("Stage 2 ID must be captured").isNotNull();

        HttpHeaders headers = headersFor(auth.loginAsAccounts());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dueDate", LocalDate.now().plusDays(30).toString());

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/boq-invoices/stage/" + stage2Id + "/raise"),
                HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> data = extractData(response.getBody());
        invoice2Id = ((Number) data.get("id")).longValue();
        assertThat(invoice2Id).isPositive();
    }

    // ------------------------------------------------------------------
    // Step 14: Send and confirm payment for Stage 2
    // ------------------------------------------------------------------

    @Test
    @Order(14)
    void step14_sendAndConfirmPaymentStage2() {
        assertThat(invoice2Id).as("Invoice 2 must be raised").isNotNull();

        HttpHeaders headers = headersFor(auth.loginAsAccounts());

        // Send
        ResponseEntity<Map> sendResponse = restTemplate.exchange(
                url("/api/boq-invoices/" + invoice2Id + "/send"),
                HttpMethod.PATCH, new HttpEntity<>(headers), Map.class);
        assertThat(sendResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Confirm payment
        Map<String, Object> paymentBody = new LinkedHashMap<>();
        paymentBody.put("paymentReference", "NEFT-2026-RES-002");
        paymentBody.put("paymentDate", LocalDate.now().toString());
        paymentBody.put("paymentMethod", "BANK_TRANSFER");

        ResponseEntity<Map> paymentResponse = restTemplate.exchange(
                url("/api/boq-invoices/" + invoice2Id + "/confirm-payment"),
                HttpMethod.PATCH, new HttpEntity<>(paymentBody, headers), Map.class);
        assertThat(paymentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(paymentResponse.getBody().get("success")).isEqualTo(true);
    }

    // ------------------------------------------------------------------
    // Step 15: Create more quality checks
    // ------------------------------------------------------------------

    @Test
    @Order(15)
    void step15_createAdditionalQualityChecks() {
        assertThat(projectId).as("Project must exist").isNotNull();

        HttpHeaders headers = headersFor(auth.loginAsEngineer());

        Object[][] checks = {
                {"Electrical wiring insulation test", "Insulation resistance check on all circuits", "COMPLETED", "PASS"},
                {"Plumbing pressure test", "Hydrostatic pressure test at 6 bar for 30 minutes", "COMPLETED", "PASS"},
                {"Waterproofing membrane check", "Terrace waterproofing adhesion and overlap inspection", "IN_PROGRESS", null}
        };

        for (Object[] check : checks) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("projectId", projectId);
            body.put("title", check[0]);
            body.put("description", check[1]);
            body.put("status", check[2]);
            if (check[3] != null) {
                body.put("result", check[3]);
            }

            ResponseEntity<Map> response = restTemplate.exchange(
                    url("/api/quality-checks"), HttpMethod.POST,
                    new HttpEntity<>(body, headers), Map.class);

            assertThat(response.getStatusCode())
                    .as("Failed to create quality check: " + check[0])
                    .isEqualTo(HttpStatus.OK);
        }
    }

    // ------------------------------------------------------------------
    // Step 16: Verify project has correct financial data
    // ------------------------------------------------------------------

    @Test
    @Order(16)
    void step16_verifyFinancialSummary() {
        assertThat(projectId).as("Project must exist").isNotNull();

        HttpHeaders headers = headersFor(auth.loginAsAdmin());

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/boq/project/" + projectId + "/financial-summary"),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        assertThat(data.get("projectId")).isNotNull();

        // Verify quality checks were created
        ResponseEntity<Map> qcResponse = restTemplate.exchange(
                url("/api/quality-checks/project/" + projectId),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(qcResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> qcData = extractDataList(qcResponse.getBody());
        assertThat(qcData).hasSizeGreaterThanOrEqualTo(4);

        // Verify invoices exist
        ResponseEntity<Map> invoiceResponse = restTemplate.exchange(
                url("/api/boq-invoices/project/" + projectId),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(invoiceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> invoices = extractDataList(invoiceResponse.getBody());
        assertThat(invoices).hasSizeGreaterThanOrEqualTo(2);
    }
}
