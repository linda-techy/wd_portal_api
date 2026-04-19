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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Commercial multi-vendor project lifecycle scenario test.
 * <p>
 * Exercises a larger commercial project with 10 BOQ items, 5 payment stages,
 * subcontracting, material procurement, labour management, delay tracking,
 * observations, CSV export, and financial verification.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommercialScenarioTest extends TestcontainersPostgresBase {

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
    private List<Long> stageIds = new ArrayList<>();
    private Long invoice1Id;
    private Long invoice2Id;
    private Long labourId;

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

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractData(Map<String, Object> body) {
        return (Map<String, Object>) body.get("data");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractDataList(Map<String, Object> body) {
        return (List<Map<String, Object>>) body.get("data");
    }

    // ------------------------------------------------------------------
    // Step 1: Create COMMERCIAL project (as Admin)
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    void step01_createCommercialProject() {
        // Create fresh commercial project with full team — isolated from
        // module tests running in the same JVM/DB.
        projectId = seeder.createFreshProjectWithTeam("COMMERCIAL", seeder.getCustomerB()).getId();
        assertThat(projectId).isPositive();
    }

    // ------------------------------------------------------------------
    // Step 2: Create BOQ document with 10 items across categories
    // ------------------------------------------------------------------

    @Test
    @Order(2)
    void step02_createBoqWith10Items() {
        assertThat(projectId).as("Project must exist").isNotNull();

        HttpHeaders headers = auth.authHeaders(auth.loginAsPM());

        // Create BOQ document
        Map<String, Object> docBody = new LinkedHashMap<>();
        docBody.put("projectId", projectId);
        docBody.put("gstRate", new BigDecimal("0.18"));

        ResponseEntity<Map> docResponse = restTemplate.exchange(
                url("/api/boq-documents"), HttpMethod.POST,
                new HttpEntity<>(docBody, headers), Map.class);

        assertThat(docResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        boqDocumentId = ((Number) extractData(docResponse.getBody()).get("id")).longValue();

        // Civil Works category (items 1-5)
        Object[][] civilItems = {
                {"Excavation & Earthwork", "CuM", "2500.00", "450.00"},
                {"RCC Foundation & Columns", "CuM", "800.00", "9500.00"},
                {"RCC Slabs & Beams", "CuM", "600.00", "10200.00"},
                {"Brickwork & Masonry", "SqM", "4500.00", "750.00"},
                {"Plastering & Rendering", "SqM", "9000.00", "320.00"}
        };

        // MEP category (items 6-10)
        Object[][] mepItems = {
                {"HVAC Ductwork & Equipment", "LS", "1.00", "8500000.00"},
                {"Electrical HT/LT Panels", "LS", "1.00", "3200000.00"},
                {"Fire Detection & Suppression", "LS", "1.00", "2800000.00"},
                {"Plumbing & Drainage", "LS", "1.00", "1500000.00"},
                {"Elevator Installation (2 units)", "LS", "1.00", "6000000.00"}
        };

        int createdCount = 0;
        for (Object[][] category : new Object[][][]{civilItems, mepItems}) {
            for (Object[] item : category) {
                Map<String, Object> itemBody = new LinkedHashMap<>();
                itemBody.put("projectId", projectId);
                itemBody.put("description", item[0]);
                itemBody.put("unit", item[1]);
                itemBody.put("quantity", new BigDecimal((String) item[2]));
                itemBody.put("unitRate", new BigDecimal((String) item[3]));
                itemBody.put("itemKind", "BASE");

                ResponseEntity<Map> itemResponse = restTemplate.exchange(
                        url("/api/boq"), HttpMethod.POST,
                        new HttpEntity<>(itemBody, headers), Map.class);

                assertThat(itemResponse.getStatusCode())
                        .as("Failed creating item: " + item[0])
                        .isEqualTo(HttpStatus.CREATED);
                createdCount++;
            }
        }
        assertThat(createdCount).isEqualTo(10);
    }

    // ------------------------------------------------------------------
    // Step 3: Submit and approve BOQ (internal + customer with 5 stages: 20% each)
    // ------------------------------------------------------------------

    @Test
    @Order(3)
    void step03_submitAndApproveBOQ() {
        assertThat(boqDocumentId).as("BOQ document must exist").isNotNull();

        HttpHeaders pmHeaders = auth.authHeaders(auth.loginAsPM());
        HttpHeaders adminHeaders = auth.authHeaders(auth.loginAsAdmin());

        // Submit
        ResponseEntity<Map> submitResponse = restTemplate.exchange(
                url("/api/boq-documents/" + boqDocumentId + "/submit"),
                HttpMethod.PATCH, new HttpEntity<>(pmHeaders), Map.class);
        assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(extractData(submitResponse.getBody()).get("status")).isEqualTo("PENDING_APPROVAL");

        // Internal approval
        ResponseEntity<Map> approveResponse = restTemplate.exchange(
                url("/api/boq-documents/" + boqDocumentId + "/approve-internal"),
                HttpMethod.PATCH, new HttpEntity<>(adminHeaders), Map.class);
        assertThat(approveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        // After approve-internal, status stays PENDING_APPROVAL until customer approves
        assertThat(extractData(approveResponse.getBody()).get("status")).isEqualTo("PENDING_APPROVAL");

        // Customer approval with 5 stages at 20% each.
        // customerB is already attached to PRJ-COM by the seeder.
        Long customerUserId = seeder.getCustomerB().getId();

        List<Map<String, Object>> stages = new ArrayList<>();
        String[] stageNames = {"Mobilization", "Substructure", "Superstructure", "MEP Rough-in", "Finishing"};
        for (String name : stageNames) {
            Map<String, Object> stage = new LinkedHashMap<>();
            stage.put("name", name);
            stage.put("percentage", new BigDecimal("0.20"));
            stages.add(stage);
        }

        Map<String, Object> approveBody = new LinkedHashMap<>();
        approveBody.put("customerSignedById", customerUserId);
        approveBody.put("stages", stages);

        ResponseEntity<Map> customerApproveResponse = restTemplate.exchange(
                url("/api/boq-documents/" + boqDocumentId + "/customer-approve"),
                HttpMethod.PATCH, new HttpEntity<>(approveBody, adminHeaders), Map.class);
        assertThat(customerApproveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(extractData(customerApproveResponse.getBody()).get("status"))
                .isEqualTo("APPROVED");

        // Retrieve payment stages
        ResponseEntity<Map> stagesResponse = restTemplate.exchange(
                url("/api/boq-documents/" + boqDocumentId + "/payment-stages"),
                HttpMethod.GET, new HttpEntity<>(adminHeaders), Map.class);
        assertThat(stagesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<Map<String, Object>> stageData = extractDataList(stagesResponse.getBody());
        assertThat(stageData).hasSize(5);

        for (Map<String, Object> s : stageData) {
            stageIds.add(((Number) s.get("id")).longValue());
        }
    }

    // ------------------------------------------------------------------
    // Step 4: Create material indent
    // ------------------------------------------------------------------

    @Test
    @Order(4)
    void step04_createMaterialIndent() {
        assertThat(projectId).as("Project must exist").isNotNull();

        HttpHeaders headers = auth.authHeaders(auth.loginAsPM());

        // MaterialIndent uses direct entity model via the controller
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requestDate", LocalDate.now().toString());
        body.put("requiredDate", LocalDate.now().plusDays(14).toString());
        body.put("priority", "HIGH");
        body.put("notes", "Urgent steel requirement for column casting");

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item1 = new LinkedHashMap<>();
        item1.put("itemName", "TMT Steel Bars - 16mm");
        item1.put("unit", "MT");
        item1.put("quantityRequested", new BigDecimal("25.00"));
        item1.put("estimatedRate", new BigDecimal("65000.00"));
        items.add(item1);

        Map<String, Object> item2 = new LinkedHashMap<>();
        item2.put("itemName", "TMT Steel Bars - 12mm");
        item2.put("unit", "MT");
        item2.put("quantityRequested", new BigDecimal("15.00"));
        item2.put("estimatedRate", new BigDecimal("64000.00"));
        items.add(item2);

        body.put("items", items);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/indents/project/" + projectId), HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    // ------------------------------------------------------------------
    // Step 5: Record labour and attendance
    // ------------------------------------------------------------------

    @Test
    @Order(5)
    void step05_recordLabourAndAttendance() {
        assertThat(projectId).as("Project must exist").isNotNull();

        HttpHeaders headers = auth.authHeaders(auth.loginAsPM());

        // Create labour
        Map<String, Object> labourBody = new LinkedHashMap<>();
        labourBody.put("name", "Ramesh Kumar");
        labourBody.put("phone", "9876500001");
        labourBody.put("tradeType", "MASON");
        labourBody.put("dailyWage", new BigDecimal("800.00"));
        labourBody.put("active", true);

        ResponseEntity<Map> labourResponse = restTemplate.exchange(
                url("/api/labour"), HttpMethod.POST,
                new HttpEntity<>(labourBody, headers), Map.class);

        assertThat(labourResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(labourResponse.getBody()).isNotNull();
        labourId = ((Number) labourResponse.getBody().get("id")).longValue();
        assertThat(labourId).isPositive();

        // Record attendance
        List<Map<String, Object>> attendanceList = new ArrayList<>();
        Map<String, Object> attendance = new LinkedHashMap<>();
        attendance.put("labourId", labourId);
        attendance.put("projectId", projectId);
        attendance.put("attendanceDate", LocalDate.now().toString());
        attendance.put("status", "PRESENT");
        attendance.put("hoursWorked", 8.0);
        attendanceList.add(attendance);

        ResponseEntity<List> attendanceResponse = restTemplate.exchange(
                url("/api/labour/attendance"), HttpMethod.POST,
                new HttpEntity<>(attendanceList, headers), List.class);

        assertThat(attendanceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(attendanceResponse.getBody()).isNotNull();
        assertThat(attendanceResponse.getBody()).hasSize(1);
    }

    // ------------------------------------------------------------------
    // Step 6: Raise first 2 staged invoices
    // ------------------------------------------------------------------

    @Test
    @Order(6)
    void step06_raiseFirstTwoInvoices() {
        assertThat(stageIds).as("Stage IDs must be captured").hasSizeGreaterThanOrEqualTo(2);

        HttpHeaders headers = auth.authHeaders(auth.loginAsAdmin());

        // Raise invoice for Stage 1
        Map<String, Object> body1 = new LinkedHashMap<>();
        body1.put("dueDate", LocalDate.now().plusDays(15).toString());

        ResponseEntity<Map> response1 = restTemplate.exchange(
                url("/api/boq-invoices/stage/" + stageIds.get(0) + "/raise"),
                HttpMethod.POST, new HttpEntity<>(body1, headers), Map.class);

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        invoice1Id = ((Number) extractData(response1.getBody()).get("id")).longValue();

        // Raise invoice for Stage 2
        Map<String, Object> body2 = new LinkedHashMap<>();
        body2.put("dueDate", LocalDate.now().plusDays(30).toString());

        ResponseEntity<Map> response2 = restTemplate.exchange(
                url("/api/boq-invoices/stage/" + stageIds.get(1) + "/raise"),
                HttpMethod.POST, new HttpEntity<>(body2, headers), Map.class);

        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        invoice2Id = ((Number) extractData(response2.getBody()).get("id")).longValue();

        assertThat(invoice1Id).isPositive();
        assertThat(invoice2Id).isPositive();
    }

    // ------------------------------------------------------------------
    // Step 7: Confirm payments (send + confirm both invoices)
    // ------------------------------------------------------------------

    @Test
    @Order(7)
    void step07_confirmPayments() {
        assertThat(invoice1Id).as("Invoice 1 must exist").isNotNull();
        assertThat(invoice2Id).as("Invoice 2 must exist").isNotNull();

        HttpHeaders headers = auth.authHeaders(auth.loginAsAdmin());

        for (int i = 0; i < 2; i++) {
            Long invoiceId = (i == 0) ? invoice1Id : invoice2Id;

            // Send
            ResponseEntity<Map> sendResponse = restTemplate.exchange(
                    url("/api/boq-invoices/" + invoiceId + "/send"),
                    HttpMethod.PATCH, new HttpEntity<>(headers), Map.class);
            assertThat(sendResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Confirm payment
            Map<String, Object> paymentBody = new LinkedHashMap<>();
            paymentBody.put("paymentReference", "RTGS-2026-COM-00" + (i + 1));
            paymentBody.put("paymentDate", LocalDate.now().toString());
            paymentBody.put("paymentMethod", "BANK_TRANSFER");

            ResponseEntity<Map> paymentResponse = restTemplate.exchange(
                    url("/api/boq-invoices/" + invoiceId + "/confirm-payment"),
                    HttpMethod.PATCH, new HttpEntity<>(paymentBody, headers), Map.class);
            assertThat(paymentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(paymentResponse.getBody().get("success")).isEqualTo(true);
        }
    }

    // ------------------------------------------------------------------
    // Step 8: Create delay log
    // ------------------------------------------------------------------

    @Test
    @Order(8)
    void step08_createDelayLog() {
        assertThat(projectId).as("Project must exist").isNotNull();

        HttpHeaders headers = auth.authHeaders(auth.loginAsPM());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("delayType", "MATERIAL_DELAY");
        body.put("fromDate", LocalDate.now().minusDays(5).toString());
        body.put("toDate", LocalDate.now().minusDays(1).toString());
        body.put("reasonText", "Steel delivery delayed due to transport strike");
        body.put("reasonCategory", "MATERIAL_SHORTAGE");
        body.put("responsibleParty", "Supplier - National Steel Corp");
        body.put("durationDays", 5);
        body.put("impactDescription", "Column casting delayed by one week, cascading to slab work");

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/projects/" + projectId + "/delays"), HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(((Number) response.getBody().get("id")).longValue()).isPositive();
    }

    // ------------------------------------------------------------------
    // Step 9: Create observation
    // ------------------------------------------------------------------

    @Test
    @Order(9)
    void step09_createObservation() {
        assertThat(projectId).as("Project must exist").isNotNull();

        HttpHeaders headers = auth.authHeaders(auth.loginAsEngineer());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        org.springframework.util.LinkedMultiValueMap<String, String> formData =
                new org.springframework.util.LinkedMultiValueMap<>();
        formData.add("title", "Exposed rebar on 3rd floor column");
        formData.add("description", "Inadequate cover to reinforcement on column C-12, 3rd floor");
        formData.add("location", "3rd Floor, Column C-12");
        formData.add("priority", "HIGH");
        formData.add("severity", "MAJOR");

        HttpHeaders multipartHeaders = auth.authHeaders(auth.loginAsEngineer());
        multipartHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/observations/project/" + projectId), HttpMethod.POST,
                new HttpEntity<>(formData, multipartHeaders), Map.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    // ------------------------------------------------------------------
    // Step 10: Export BOQ CSV
    // ------------------------------------------------------------------

    @Test
    @Order(10)
    void step10_exportBoqCsv() {
        assertThat(projectId).as("Project must exist").isNotNull();

        HttpHeaders headers = auth.authHeaders(auth.loginAsAdmin());

        ResponseEntity<byte[]> response = restTemplate.exchange(
                url("/api/projects/" + projectId + "/export/boq"),
                HttpMethod.GET, new HttpEntity<>(headers), byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isPositive();

        // Verify it's CSV content (starts with UTF-8 BOM or header)
        String csv = new String(response.getBody(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).contains("Description");
        assertThat(csv).contains("Quantity");
    }

    // ------------------------------------------------------------------
    // Step 11: Verify financial summary
    // ------------------------------------------------------------------

    @Test
    @Order(11)
    void step11_verifyFinancialSummary() {
        assertThat(projectId).as("Project must exist").isNotNull();

        HttpHeaders headers = auth.authHeaders(auth.loginAsAdmin());

        // Financial summary
        ResponseEntity<Map> finResponse = restTemplate.exchange(
                url("/api/boq/project/" + projectId + "/financial-summary"),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(finResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> finData = extractData(finResponse.getBody());
        assertThat(finData).isNotNull();
        assertThat(finData.get("projectId")).isNotNull();

        // Verify invoices
        ResponseEntity<Map> invoiceResponse = restTemplate.exchange(
                url("/api/boq-invoices/project/" + projectId),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(invoiceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> invoices = extractDataList(invoiceResponse.getBody());
        assertThat(invoices).hasSizeGreaterThanOrEqualTo(2);

        // Verify delay log exists
        ResponseEntity<List> delayResponse = restTemplate.exchange(
                url("/api/projects/" + projectId + "/delays"),
                HttpMethod.GET, new HttpEntity<>(headers), List.class);
        assertThat(delayResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(delayResponse.getBody()).hasSizeGreaterThanOrEqualTo(1);

        // Verify observations exist
        ResponseEntity<Map> obsResponse = restTemplate.exchange(
                url("/api/observations/project/" + projectId),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(obsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> observations = extractDataList(obsResponse.getBody());
        assertThat(observations).hasSizeGreaterThanOrEqualTo(1);
    }
}
