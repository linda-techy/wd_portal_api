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
 * Integration tests for the Payment module.
 * <p>
 * Covers design payment creation, transaction recording, payment schedule
 * retrieval, and stage payment certification.
 * <p>
 * Ordered tests first set up a project with an approved BOQ and payment stages,
 * then exercise the payment lifecycle endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentModuleTest extends TestcontainersPostgresBase {

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
    private static Long designPaymentId;
    private static Long scheduleId;

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
        return auth.authHeaders(auth.loginAsAdmin());
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
    // Setup: project + approved BOQ with payment stages
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    void setup_createProjectAndApprovedBoq() {
        HttpHeaders headers = adminHeaders();

        // 1. Create fresh project (isolated from other tests that approve BOQs).
        projectId = seeder.createFreshProjectWithTeam("COMMERCIAL", seeder.getCustomerB()).getId();

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
        itemBody.put("description", "Concrete Foundation");
        itemBody.put("unit", "CuM");
        itemBody.put("quantity", new BigDecimal("500.00"));
        itemBody.put("unitRate", new BigDecimal("5000.00"));
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

        // 6. Customer approval with payment stages.
        // Commercial project is owned by customerB (already a member via seeder).
        Long customerUserId = seeder.getCustomerB().getId();

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
    // Payment Schedule tests
    // ------------------------------------------------------------------

    @Test
    @Order(2)
    void should_getPaymentSchedule() {
        assertThat(boqDocumentId).as("BOQ document must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/boq-documents/" + boqDocumentId + "/payment-stages"),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        List<Map<String, Object>> stages = extractDataList(response.getBody());
        assertThat(stages).hasSizeGreaterThanOrEqualTo(3);

        // Verify stage structure
        Map<String, Object> firstStage = stages.get(0);
        assertThat(firstStage).containsKey("id");
        assertThat(firstStage).containsKey("stageName");
    }

    @Test
    @Order(3)
    void should_createDesignPayment() {
        assertThat(projectId).as("Project must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("projectId", projectId);
        body.put("packageName", "Premium Design Package");
        body.put("ratePerSqft", new BigDecimal("75.00"));
        body.put("totalSqft", new BigDecimal("2500"));
        body.put("discountPercentage", new BigDecimal("5"));
        body.put("paymentType", "INSTALLMENT");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/payments/design"), HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        designPaymentId = ((Number) data.get("id")).longValue();
        assertThat(designPaymentId).isPositive();

        // Retrieve schedule IDs if present
        if (data.containsKey("schedules") && data.get("schedules") != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> schedules = (List<Map<String, Object>>) data.get("schedules");
            if (!schedules.isEmpty()) {
                scheduleId = ((Number) schedules.get(0).get("id")).longValue();
            }
        }
    }

    @Test
    @Order(4)
    void should_getDesignPaymentByProject() {
        assertThat(projectId).as("Project must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/payments/design/project/" + projectId),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        assertThat(data.get("id")).isNotNull();
    }

    @Test
    @Order(5)
    void should_recordTransaction() {
        assertThat(scheduleId).as("Schedule must exist for transaction recording").isNotNull();

        HttpHeaders headers = adminHeaders();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", new BigDecimal("50000.00"));
        body.put("paymentMethod", "BANK_TRANSFER");
        body.put("referenceNumber", "TXN-PAY-001");
        body.put("notes", "First installment payment");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/payments/schedule/" + scheduleId + "/transactions"),
                HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    @Test
    @Order(6)
    void should_listAllPayments() {
        HttpHeaders headers = adminHeaders();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/payments/all?page=0&size=10"),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    @Test
    @Order(7)
    void should_getTransactionHistory() {
        HttpHeaders headers = adminHeaders();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/payments/history?page=0&size=10"),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }
}
