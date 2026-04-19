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
 * Change-order-heavy renovation project scenario test.
 * <p>
 * Focuses on the change order workflow: creating multiple COs with different types,
 * submitting, approving, and sending to customer. Also covers delay logs and observations
 * that typically arise during renovation discovery work.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RenovationScenarioTest extends TestcontainersPostgresBase {

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
    private Long changeOrder1Id;
    private Long changeOrder2Id;
    private Long changeOrder3Id;

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

    private Map<String, Object> lineItem(String description, String unit,
                                          BigDecimal newQty, BigDecimal unitRate, BigDecimal lineAmount) {
        Map<String, Object> li = new LinkedHashMap<>();
        li.put("description", description);
        li.put("unit", unit);
        li.put("originalQuantity", BigDecimal.ZERO);
        li.put("newQuantity", newQty);
        li.put("originalRate", BigDecimal.ZERO);
        li.put("newRate", unitRate);
        li.put("unitRate", unitRate);
        li.put("lineAmountExGst", lineAmount);
        return li;
    }

    // ------------------------------------------------------------------
    // Step 1: Create RENOVATION project
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    void step01_createRenovationProject() {
        // Create fresh renovation project with full team — isolated from
        // module tests running in the same JVM/DB.
        projectId = seeder.createFreshProjectWithTeam("RENOVATION", seeder.getCustomerC()).getId();
        assertThat(projectId).isPositive();
    }

    // ------------------------------------------------------------------
    // Step 2: Create initial BOQ (5 items)
    // ------------------------------------------------------------------

    @Test
    @Order(2)
    void step02_createInitialBoq() {
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

        // Add 5 renovation-specific BOQ items
        Object[][] items = {
                {"Demolition & Debris Removal", "LS", "1.00", "350000.00"},
                {"Structural Reinforcement", "CuM", "45.00", "12000.00"},
                {"Rewiring & Electrical Upgrade", "LS", "1.00", "480000.00"},
                {"Plumbing Overhaul", "LS", "1.00", "320000.00"},
                {"Flooring Replacement (Italian Marble)", "SqM", "280.00", "2800.00"}
        };

        for (Object[] item : items) {
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
        }
    }

    // ------------------------------------------------------------------
    // Step 3: Submit and approve BOQ
    // ------------------------------------------------------------------

    @Test
    @Order(3)
    void step03_submitAndApproveBoq() {
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

        // Customer approval with stages.
        // customerC is already attached to PRJ-REN by the seeder.
        Long customerUserId = seeder.getCustomerC().getId();

        Map<String, Object> stage1 = new LinkedHashMap<>();
        stage1.put("name", "Demolition Phase");
        stage1.put("percentage", new BigDecimal("0.30"));

        Map<String, Object> stage2 = new LinkedHashMap<>();
        stage2.put("name", "Reconstruction");
        stage2.put("percentage", new BigDecimal("0.45"));

        Map<String, Object> stage3 = new LinkedHashMap<>();
        stage3.put("name", "Final Fit-out");
        stage3.put("percentage", new BigDecimal("0.25"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customerSignedById", customerUserId);
        body.put("stages", List.of(stage1, stage2, stage3));

        ResponseEntity<Map> customerResponse = restTemplate.exchange(
                url("/api/boq-documents/" + boqDocumentId + "/customer-approve"),
                HttpMethod.PATCH, new HttpEntity<>(body, adminHeaders), Map.class);
        assertThat(customerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(extractData(customerResponse.getBody()).get("status")).isEqualTo("APPROVED");
    }

    // ------------------------------------------------------------------
    // Step 4: Create Change Order 1 (add rooftop garden, 2 items)
    // ------------------------------------------------------------------

    @Test
    @Order(4)
    void step04_createChangeOrder1_rooftopGarden() {
        assertThat(projectId).as("Project must exist").isNotNull();

        HttpHeaders headers = auth.authHeaders(auth.loginAsPM());

        Map<String, Object> co1Body = new LinkedHashMap<>();
        co1Body.put("projectId", projectId);
        co1Body.put("coType", "SCOPE_ADDITION");
        co1Body.put("title", "Add Rooftop Garden");
        co1Body.put("description", "Customer requested a landscaped rooftop garden with seating area");
        co1Body.put("justification", "Customer enhancement request during demolition phase");

        List<Map<String, Object>> lineItems = new ArrayList<>();
        lineItems.add(lineItem(
                "Rooftop waterproofing & drainage", "SqM",
                new BigDecimal("120.00"), new BigDecimal("1800.00"),
                new BigDecimal("216000.00")));
        lineItems.add(lineItem(
                "Landscaping, planters & seating", "LS",
                new BigDecimal("1.00"), new BigDecimal("450000.00"),
                new BigDecimal("450000.00")));
        co1Body.put("lineItems", lineItems);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/change-orders"), HttpMethod.POST,
                new HttpEntity<>(co1Body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> data = extractData(response.getBody());
        changeOrder1Id = ((Number) data.get("id")).longValue();
        assertThat(changeOrder1Id).isPositive();
        assertThat(data.get("status")).isEqualTo("DRAFT");
    }

    // ------------------------------------------------------------------
    // Step 5: Submit and internally approve CO1
    // ------------------------------------------------------------------

    @Test
    @Order(5)
    void step05_submitAndApproveChangeOrder1() {
        assertThat(changeOrder1Id).as("CO1 must exist").isNotNull();

        HttpHeaders pmHeaders = auth.authHeaders(auth.loginAsPM());
        HttpHeaders adminHeaders = auth.authHeaders(auth.loginAsAdmin());

        // Submit
        ResponseEntity<Map> submitResponse = restTemplate.exchange(
                url("/api/change-orders/" + changeOrder1Id + "/submit"),
                HttpMethod.PATCH, new HttpEntity<>(pmHeaders), Map.class);
        assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> submitData = extractData(submitResponse.getBody());
        // ChangeOrder.submit transitions to SUBMITTED (ChangeOrderStatus enum)
        assertThat(submitData.get("status")).isEqualTo("SUBMITTED");

        // Internal approval
        ResponseEntity<Map> approveResponse = restTemplate.exchange(
                url("/api/change-orders/" + changeOrder1Id + "/approve-internal"),
                HttpMethod.PATCH, new HttpEntity<>(adminHeaders), Map.class);
        assertThat(approveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> approveData = extractData(approveResponse.getBody());
        assertThat(approveData.get("status")).isEqualTo("INTERNALLY_APPROVED");
    }

    // ------------------------------------------------------------------
    // Step 6: Send CO1 to customer (status: CUSTOMER_REVIEW)
    // ------------------------------------------------------------------

    @Test
    @Order(6)
    void step06_sendCO1ToCustomer() {
        assertThat(changeOrder1Id).as("CO1 must exist").isNotNull();

        HttpHeaders headers = auth.authHeaders(auth.loginAsAdmin());

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/change-orders/" + changeOrder1Id + "/send-to-customer"),
                HttpMethod.PATCH, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = extractData(response.getBody());
        assertThat(data.get("status")).isEqualTo("CUSTOMER_REVIEW");
    }

    // ------------------------------------------------------------------
    // Step 7: Create Change Order 2 (structural fix, 1 item)
    // ------------------------------------------------------------------

    @Test
    @Order(7)
    void step07_createChangeOrder2_structuralFix() {
        assertThat(projectId).as("Project must exist").isNotNull();

        HttpHeaders headers = auth.authHeaders(auth.loginAsPM());

        Map<String, Object> co2Body = new LinkedHashMap<>();
        co2Body.put("projectId", projectId);
        co2Body.put("coType", "UNFORESEEN_VARIATION");
        co2Body.put("title", "Emergency Structural Fix - Load Bearing Wall");
        co2Body.put("description", "Hidden termite damage found in load-bearing wall during demolition");
        co2Body.put("justification", "Structural integrity compromise discovered during demolition phase");

        List<Map<String, Object>> lineItems = new ArrayList<>();
        lineItems.add(lineItem(
                "Load bearing wall structural repair & reinforcement", "LS",
                new BigDecimal("1.00"), new BigDecimal("680000.00"),
                new BigDecimal("680000.00")));
        co2Body.put("lineItems", lineItems);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/change-orders"), HttpMethod.POST,
                new HttpEntity<>(co2Body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> data = extractData(response.getBody());
        changeOrder2Id = ((Number) data.get("id")).longValue();
        assertThat(changeOrder2Id).isPositive();
    }

    // ------------------------------------------------------------------
    // Step 8: Submit and approve CO2 internally
    // ------------------------------------------------------------------

    @Test
    @Order(8)
    void step08_submitAndApproveChangeOrder2() {
        assertThat(changeOrder2Id).as("CO2 must exist").isNotNull();

        HttpHeaders pmHeaders = auth.authHeaders(auth.loginAsPM());
        HttpHeaders adminHeaders = auth.authHeaders(auth.loginAsAdmin());

        // Submit
        ResponseEntity<Map> submitResponse = restTemplate.exchange(
                url("/api/change-orders/" + changeOrder2Id + "/submit"),
                HttpMethod.PATCH, new HttpEntity<>(pmHeaders), Map.class);
        assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(extractData(submitResponse.getBody()).get("status")).isEqualTo("SUBMITTED");

        // Internal approval
        ResponseEntity<Map> approveResponse = restTemplate.exchange(
                url("/api/change-orders/" + changeOrder2Id + "/approve-internal"),
                HttpMethod.PATCH, new HttpEntity<>(adminHeaders), Map.class);
        assertThat(approveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(extractData(approveResponse.getBody()).get("status")).isEqualTo("INTERNALLY_APPROVED");
    }

    // ------------------------------------------------------------------
    // Step 9: Create Change Order 3 (downgrade fixtures - scope reduction)
    // ------------------------------------------------------------------

    @Test
    @Order(9)
    void step09_createChangeOrder3_downgradeFixtures() {
        assertThat(projectId).as("Project must exist").isNotNull();

        HttpHeaders headers = auth.authHeaders(auth.loginAsPM());

        Map<String, Object> co3Body = new LinkedHashMap<>();
        co3Body.put("projectId", projectId);
        co3Body.put("coType", "SCOPE_REDUCTION");
        co3Body.put("title", "Downgrade Bathroom Fixtures");
        co3Body.put("description", "Customer opted for standard fixtures instead of premium imported ones");
        co3Body.put("justification", "Budget reallocation to rooftop garden; customer accepted standard grade");

        List<Map<String, Object>> lineItems = new ArrayList<>();
        lineItems.add(lineItem(
                "Fixture downgrade credit (4 bathrooms)", "LS",
                new BigDecimal("1.00"), new BigDecimal("-180000.00"),
                new BigDecimal("-180000.00")));
        co3Body.put("lineItems", lineItems);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/change-orders"), HttpMethod.POST,
                new HttpEntity<>(co3Body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> data = extractData(response.getBody());
        changeOrder3Id = ((Number) data.get("id")).longValue();
        assertThat(changeOrder3Id).isPositive();
    }

    // ------------------------------------------------------------------
    // Step 10: Submit CO3
    // ------------------------------------------------------------------

    @Test
    @Order(10)
    void step10_submitChangeOrder3() {
        assertThat(changeOrder3Id).as("CO3 must exist").isNotNull();

        HttpHeaders headers = auth.authHeaders(auth.loginAsPM());

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/change-orders/" + changeOrder3Id + "/submit"),
                HttpMethod.PATCH, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = extractData(response.getBody());
        assertThat(data.get("status")).isEqualTo("SUBMITTED");
    }

    // ------------------------------------------------------------------
    // Step 11: Create delay log for structural discovery
    // ------------------------------------------------------------------

    @Test
    @Order(11)
    void step11_createDelayLogForStructuralDiscovery() {
        assertThat(projectId).as("Project must exist").isNotNull();

        HttpHeaders headers = auth.authHeaders(auth.loginAsPM());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("delayType", "OTHER");
        body.put("fromDate", LocalDate.now().minusDays(10).toString());
        body.put("toDate", LocalDate.now().minusDays(3).toString());
        body.put("reasonText", "Work stopped after discovering hidden termite damage in load-bearing wall; "
                + "structural engineer assessment required");
        body.put("reasonCategory", "DESIGN_CHANGES");
        body.put("responsibleParty", "Not attributable - hidden condition");
        body.put("durationDays", 7);
        body.put("impactDescription", "7-day delay while structural assessment and CO2 were prepared; "
                + "cascading impact on reconstruction schedule");

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/projects/" + projectId + "/delays"), HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(((Number) response.getBody().get("id")).longValue()).isPositive();
    }

    // ------------------------------------------------------------------
    // Step 12: Create observations/snags
    // ------------------------------------------------------------------

    @Test
    @Order(12)
    void step12_createObservationsAndSnags() {
        assertThat(projectId).as("Project must exist").isNotNull();

        HttpHeaders headers = auth.authHeaders(auth.loginAsEngineer());

        String[][] observations = {
                {"Termite trail in west wall", "Active termite trail visible behind plaster on west-facing wall",
                        "West Wall, Ground Floor", "HIGH", "CRITICAL"},
                {"Hairline crack in ceiling beam", "Existing hairline crack in RCC beam near staircase",
                        "Staircase Landing, 1st Floor", "MEDIUM", "MINOR"},
                {"Damp patch near bathroom", "Rising damp observed near ground floor bathroom external wall",
                        "Ground Floor Bathroom", "HIGH", "MAJOR"}
        };

        for (String[] obs : observations) {
            HttpHeaders multipartHeaders = auth.authHeaders(auth.loginAsEngineer());
            multipartHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

            org.springframework.util.LinkedMultiValueMap<String, String> formData =
                    new org.springframework.util.LinkedMultiValueMap<>();
            formData.add("title", obs[0]);
            formData.add("description", obs[1]);
            formData.add("location", obs[2]);
            formData.add("priority", obs[3]);
            formData.add("severity", obs[4]);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url("/api/observations/project/" + projectId), HttpMethod.POST,
                    new HttpEntity<>(formData, multipartHeaders), Map.class);

            assertThat(response.getStatusCode())
                    .as("Failed creating observation: " + obs[0])
                    .isIn(HttpStatus.OK, HttpStatus.CREATED);
            assertThat(response.getBody().get("success")).isEqualTo(true);
        }
    }

    // ------------------------------------------------------------------
    // Step 13: Verify BOQ document is still in original approved state
    // ------------------------------------------------------------------

    @Test
    @Order(13)
    void step13_verifyBoqDocumentStillApproved() {
        assertThat(boqDocumentId).as("BOQ document must exist").isNotNull();

        HttpHeaders headers = auth.authHeaders(auth.loginAsAdmin());

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/boq-documents/" + boqDocumentId),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();

        // BOQ document should remain APPROVED regardless of change orders
        assertThat(data.get("status")).isEqualTo("APPROVED");

        // Verify BOQ items still exist
        ResponseEntity<Map> itemsResponse = restTemplate.exchange(
                url("/api/boq/project/" + projectId),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(itemsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> itemsData = extractData(itemsResponse.getBody());
        assertThat(itemsData).isNotNull();
        // Paginated response should have content
        assertThat(itemsData.get("content")).isNotNull();
    }

    // ------------------------------------------------------------------
    // Step 14: Verify 3 change orders exist with correct statuses
    // ------------------------------------------------------------------

    @Test
    @Order(14)
    void step14_verifyChangeOrderStatuses() {
        assertThat(projectId).as("Project must exist").isNotNull();

        HttpHeaders headers = auth.authHeaders(auth.loginAsAdmin());

        // Get all change orders for the project
        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/change-orders/project/" + projectId),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> changeOrders = extractDataList(response.getBody());
        assertThat(changeOrders).hasSize(3);

        // Build a map of CO ID to status for verification
        Map<Long, String> coStatuses = new LinkedHashMap<>();
        for (Map<String, Object> co : changeOrders) {
            Long coId = ((Number) co.get("id")).longValue();
            String status = (String) co.get("status");
            coStatuses.put(coId, status);
        }

        // CO1: sent to customer -> CUSTOMER_REVIEW
        assertThat(coStatuses.get(changeOrder1Id))
                .as("CO1 should be in CUSTOMER_REVIEW")
                .isEqualTo("CUSTOMER_REVIEW");

        // CO2: internally approved -> INTERNALLY_APPROVED
        assertThat(coStatuses.get(changeOrder2Id))
                .as("CO2 should be INTERNALLY_APPROVED")
                .isEqualTo("INTERNALLY_APPROVED");

        // CO3: submitted -> SUBMITTED (ChangeOrderStatus enum)
        assertThat(coStatuses.get(changeOrder3Id))
                .as("CO3 should be SUBMITTED")
                .isEqualTo("SUBMITTED");

        // Verify individual COs have correct types
        ResponseEntity<Map> co1Response = restTemplate.exchange(
                url("/api/change-orders/" + changeOrder1Id),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(co1Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> co1Data = extractData(co1Response.getBody());
        assertThat(co1Data.get("coType")).isEqualTo("SCOPE_ADDITION");
        assertThat(co1Data.get("title")).isEqualTo("Add Rooftop Garden");

        ResponseEntity<Map> co2Response = restTemplate.exchange(
                url("/api/change-orders/" + changeOrder2Id),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(co2Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> co2Data = extractData(co2Response.getBody());
        assertThat(co2Data.get("coType")).isEqualTo("UNFORESEEN_VARIATION");

        ResponseEntity<Map> co3Response = restTemplate.exchange(
                url("/api/change-orders/" + changeOrder3Id),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(co3Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> co3Data = extractData(co3Response.getBody());
        assertThat(co3Data.get("coType")).isEqualTo("SCOPE_REDUCTION");

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
        assertThat(observations).hasSizeGreaterThanOrEqualTo(3);
    }
}
