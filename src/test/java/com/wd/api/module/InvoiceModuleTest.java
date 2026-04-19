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
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Invoice module.
 * Covers the full invoice lifecycle: raise from payment stage, list, send,
 * confirm payment, and dispute.
 * <p>
 * Requires a fully approved BOQ with payment stages, so the ordered tests
 * first set up a project, BOQ document, items, and run through the approval
 * workflow before raising invoices.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InvoiceModuleTest extends TestcontainersPostgresBase {

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
    private static Long paymentStageId;
    private static Long invoiceId;

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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractDataList(Map<String, Object> body) {
        return (List<Map<String, Object>>) body.get("data");
    }

    // ------------------------------------------------------------------
    // Setup: project + BOQ + approval (must run before invoice tests)
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    void setup_createProjectAndApprovedBoq() {
        HttpHeaders headers = adminHeaders();

        // 1. Create project
        Map<String, Object> projectBody = new LinkedHashMap<>();
        projectBody.put("name", "Invoice Test Project");
        projectBody.put("location", "Chennai");
        projectBody.put("project_type", "COMMERCIAL");
        projectBody.put("state", "Tamil Nadu");
        projectBody.put("district", "Chennai");

        ResponseEntity<Map> projectResponse = restTemplate.exchange(
                baseUrl("/customer-projects"), HttpMethod.POST,
                new HttpEntity<>(projectBody, headers), Map.class);

        assertThat(projectResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        projectId = ((Number) extractData(projectResponse.getBody()).get("id")).longValue();

        // 2. Create BOQ document
        Map<String, Object> docBody = new LinkedHashMap<>();
        docBody.put("projectId", projectId);
        docBody.put("gstRate", new BigDecimal("0.18"));

        ResponseEntity<Map> docResponse = restTemplate.exchange(
                baseUrl("/api/boq-documents"), HttpMethod.POST,
                new HttpEntity<>(docBody, headers), Map.class);

        assertThat(docResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        boqDocumentId = ((Number) extractData(docResponse.getBody()).get("id")).longValue();

        // 3. Create a BOQ item
        Map<String, Object> itemBody = new LinkedHashMap<>();
        itemBody.put("projectId", projectId);
        itemBody.put("description", "Steel Reinforcement");
        itemBody.put("unit", "Kg");
        itemBody.put("quantity", new BigDecimal("1000.00"));
        itemBody.put("unitRate", new BigDecimal("80.00"));
        itemBody.put("itemKind", "BASE");

        ResponseEntity<Map> itemResponse = restTemplate.exchange(
                baseUrl("/api/boq"), HttpMethod.POST,
                new HttpEntity<>(itemBody, headers), Map.class);

        assertThat(itemResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 4. Submit for approval
        ResponseEntity<Map> submitResponse = restTemplate.exchange(
                baseUrl("/api/boq-documents/" + boqDocumentId + "/submit"),
                HttpMethod.PATCH, new HttpEntity<>(headers), Map.class);
        assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 5. Internal approval
        ResponseEntity<Map> approveResponse = restTemplate.exchange(
                baseUrl("/api/boq-documents/" + boqDocumentId + "/approve-internal"),
                HttpMethod.PATCH, new HttpEntity<>(headers), Map.class);
        assertThat(approveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 6. Customer approval with payment stages
        Long customerUserId = seeder.getCustomerA().getId();

        // Add customerA as project member (required for verifyCustomerMembership)
        Map<String, Object> memberBody = new LinkedHashMap<>();
        memberBody.put("customerUserId", customerUserId);
        memberBody.put("role", "CUSTOMER");
        restTemplate.exchange(
                baseUrl("/customer-projects/" + projectId + "/members"),
                HttpMethod.POST, new HttpEntity<>(memberBody, headers), Map.class);

        Map<String, Object> stage1 = new LinkedHashMap<>();
        stage1.put("name", "Advance");
        stage1.put("percentage", new BigDecimal("0.30"));

        Map<String, Object> stage2 = new LinkedHashMap<>();
        stage2.put("name", "Mid-work");
        stage2.put("percentage", new BigDecimal("0.40"));

        Map<String, Object> stage3 = new LinkedHashMap<>();
        stage3.put("name", "Completion");
        stage3.put("percentage", new BigDecimal("0.30"));

        Map<String, Object> approveBody = new LinkedHashMap<>();
        approveBody.put("customerSignedById", customerUserId);
        approveBody.put("stages", List.of(stage1, stage2, stage3));

        ResponseEntity<Map> customerApproveResponse = restTemplate.exchange(
                baseUrl("/api/boq-documents/" + boqDocumentId + "/customer-approve"),
                HttpMethod.PATCH, new HttpEntity<>(approveBody, headers), Map.class);
        assertThat(customerApproveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 7. Retrieve payment stages to get a stage ID
        ResponseEntity<Map> stagesResponse = restTemplate.exchange(
                baseUrl("/api/boq-documents/" + boqDocumentId + "/payment-stages"),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(stagesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<Map<String, Object>> stages = extractDataList(stagesResponse.getBody());
        assertThat(stages).isNotEmpty();
        paymentStageId = ((Number) stages.get(0).get("id")).longValue();
    }

    // ------------------------------------------------------------------
    // Invoice lifecycle tests
    // ------------------------------------------------------------------

    @Test
    @Order(2)
    void should_raiseInvoiceFromPaymentStage() {
        assertThat(paymentStageId).as("Payment stage must exist").isNotNull();

        HttpHeaders headers = adminHeaders();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dueDate", LocalDate.now().plusDays(30).toString());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/boq-invoices/stage/" + paymentStageId + "/raise"),
                HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        invoiceId = ((Number) data.get("id")).longValue();
        assertThat(invoiceId).isPositive();
    }

    @Test
    @Order(3)
    void should_listInvoices() {
        assertThat(projectId).as("Project must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/boq-invoices/project/" + projectId),
                HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        List<Map<String, Object>> invoices = extractDataList(response.getBody());
        assertThat(invoices).isNotEmpty();
    }

    @Test
    @Order(4)
    void should_sendInvoice() {
        assertThat(invoiceId).as("Invoice must be raised first").isNotNull();

        HttpHeaders headers = adminHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/boq-invoices/" + invoiceId + "/send"),
                HttpMethod.PATCH, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    @Test
    @Order(5)
    void should_confirmPayment() {
        assertThat(invoiceId).as("Invoice must be raised first").isNotNull();

        HttpHeaders headers = adminHeaders();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentReference", "TXN-2026-04-001");
        body.put("paymentMethod", "BANK_TRANSFER");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/boq-invoices/" + invoiceId + "/confirm-payment"),
                HttpMethod.PATCH, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    @Test
    @Order(6)
    void should_disputeInvoice() {
        // Raise a second invoice from another stage to test dispute flow
        assertThat(projectId).as("Project must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        // Get remaining payment stages
        HttpEntity<Void> getEntity = new HttpEntity<>(headers);
        ResponseEntity<Map> stagesResponse = restTemplate.exchange(
                baseUrl("/api/boq-documents/" + boqDocumentId + "/payment-stages"),
                HttpMethod.GET, getEntity, Map.class);
        assertThat(stagesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<Map<String, Object>> stages = extractDataList(stagesResponse.getBody());
        // Find a stage that hasn't been invoiced yet (second stage)
        Long secondStageId = null;
        for (Map<String, Object> stage : stages) {
            Long stageIdCandidate = ((Number) stage.get("id")).longValue();
            if (!stageIdCandidate.equals(paymentStageId)) {
                secondStageId = stageIdCandidate;
                break;
            }
        }
        assertThat(secondStageId).as("Need a second payment stage for dispute test").isNotNull();

        // Raise invoice on second stage
        Map<String, Object> raiseBody = new LinkedHashMap<>();
        raiseBody.put("dueDate", LocalDate.now().plusDays(60).toString());

        ResponseEntity<Map> raiseResponse = restTemplate.exchange(
                baseUrl("/api/boq-invoices/stage/" + secondStageId + "/raise"),
                HttpMethod.POST, new HttpEntity<>(raiseBody, headers), Map.class);
        assertThat(raiseResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Long secondInvoiceId = ((Number) extractData(raiseResponse.getBody()).get("id")).longValue();

        // Send the invoice first (dispute requires SENT status)
        restTemplate.exchange(
                baseUrl("/api/boq-invoices/" + secondInvoiceId + "/send"),
                HttpMethod.PATCH, new HttpEntity<>(headers), Map.class);

        // Dispute it
        Map<String, Object> disputeBody = new LinkedHashMap<>();
        disputeBody.put("reason", "Incorrect quantities billed");

        HttpEntity<Map<String, Object>> disputeEntity = new HttpEntity<>(disputeBody, headers);

        ResponseEntity<Map> disputeResponse = restTemplate.exchange(
                baseUrl("/api/boq-invoices/" + secondInvoiceId + "/dispute"),
                HttpMethod.PATCH, disputeEntity, Map.class);

        assertThat(disputeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(disputeResponse.getBody()).isNotNull();
        assertThat(disputeResponse.getBody().get("success")).isEqualTo(true);
    }
}
