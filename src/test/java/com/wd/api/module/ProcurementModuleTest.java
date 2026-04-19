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
 * Integration tests for the Procurement module.
 * <p>
 * Covers vendors, purchase orders, and GRN (Goods Received Note) workflows.
 * <p>
 * Ordered tests first set up a project and vendor, then exercise purchase
 * order creation and listing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProcurementModuleTest extends TestcontainersPostgresBase {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TestDataSeeder seeder;

    AuthTestHelper auth;

    /** Shared state across ordered tests. */
    private static Long projectId;
    private static Long vendorId;
    private static Long purchaseOrderId;

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
    // Setup: create project and vendor
    // ------------------------------------------------------------------

    @Test
    @Order(0)
    void setup_createProjectAndVendor() {
        HttpHeaders headers = adminHeaders();

        // 1. Create fresh project (isolated from other module tests).
        projectId = seeder.createFreshProjectWithTeam("COMMERCIAL", seeder.getCustomerB()).getId();
        assertThat(projectId).isPositive();

        // 2. Create vendor
        Map<String, Object> vendorBody = new LinkedHashMap<>();
        vendorBody.put("name", "ABC Building Supplies");
        vendorBody.put("contactPerson", "Rajesh Kumar");
        vendorBody.put("phone", "9876543210");
        vendorBody.put("email", "rajesh@abcsupplies.com");
        vendorBody.put("gstin", "29ABCDE1234F1Z5");
        vendorBody.put("address", "123 Industrial Area, Mangalore");
        vendorBody.put("vendorType", "MATERIAL");
        vendorBody.put("bankName", "State Bank of India");
        vendorBody.put("accountNumber", "1234567890");
        vendorBody.put("ifscCode", "SBIN0001234");
        vendorBody.put("active", true);

        ResponseEntity<Map> vendorResponse = restTemplate.exchange(
                baseUrl("/api/procurement/vendors"), HttpMethod.POST,
                new HttpEntity<>(vendorBody, headers), Map.class);

        assertThat(vendorResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(vendorResponse.getBody()).isNotNull();
        assertThat(vendorResponse.getBody().get("success")).isEqualTo(true);

        Map<String, Object> vendorData = extractData(vendorResponse.getBody());
        assertThat(vendorData).isNotNull();
        vendorId = ((Number) vendorData.get("id")).longValue();
        assertThat(vendorId).isPositive();
    }

    // ------------------------------------------------------------------
    // Vendor tests
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    void should_listVendors() {
        HttpHeaders headers = adminHeaders();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/procurement/vendors"),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        List<Map<String, Object>> vendors = extractDataList(response.getBody());
        assertThat(vendors).isNotEmpty();

        // Verify our vendor is in the list
        boolean found = vendors.stream()
                .anyMatch(v -> "ABC Building Supplies".equals(v.get("name")));
        assertThat(found).as("Created vendor should be in the list").isTrue();
    }

    // ------------------------------------------------------------------
    // Purchase Order tests
    // ------------------------------------------------------------------

    @Test
    @Order(2)
    void should_createPurchaseOrder() {
        assertThat(projectId).as("Project must be created first").isNotNull();
        assertThat(vendorId).as("Vendor must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        // Create PO items (matches PurchaseOrderItemDTO schema)
        Map<String, Object> item1 = new LinkedHashMap<>();
        item1.put("description", "TMT Steel Bars 12mm");
        item1.put("unit", "KG");
        item1.put("quantity", new BigDecimal("5000"));
        item1.put("rate", new BigDecimal("65.00"));
        item1.put("gstPercentage", new BigDecimal("18.00"));
        item1.put("amount", new BigDecimal("325000.00"));

        Map<String, Object> item2 = new LinkedHashMap<>();
        item2.put("description", "Portland Cement 53 Grade");
        item2.put("unit", "BAGS");
        item2.put("quantity", new BigDecimal("200"));
        item2.put("rate", new BigDecimal("380.00"));
        item2.put("gstPercentage", new BigDecimal("18.00"));
        item2.put("amount", new BigDecimal("76000.00"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("projectId", projectId);
        body.put("vendorId", vendorId);
        body.put("poDate", LocalDate.now().toString());
        body.put("expectedDeliveryDate", LocalDate.now().plusDays(14).toString());
        body.put("totalAmount", new BigDecimal("401000.00"));
        body.put("gstAmount", new BigDecimal("72180.00"));
        body.put("netAmount", new BigDecimal("473180.00"));
        body.put("status", "DRAFT");
        body.put("notes", "Urgent requirement for foundation phase");
        body.put("items", List.of(item1, item2));

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/procurement/purchase-orders"), HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        purchaseOrderId = ((Number) data.get("id")).longValue();
        assertThat(purchaseOrderId).isPositive();
    }

    @Test
    @Order(3)
    void should_listPurchaseOrders() {
        assertThat(projectId).as("Project must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/procurement/purchase-orders?projectId=" + projectId + "&page=0&size=20"),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        // The response wraps a Page object
        if (data.containsKey("content")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
            assertThat(content).isNotEmpty();
        }
    }

    @Test
    @Order(4)
    void should_updatePurchaseOrder() {
        assertThat(purchaseOrderId).as("Purchase order must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", purchaseOrderId);
        body.put("projectId", projectId);
        body.put("vendorId", vendorId);
        body.put("notes", "Updated: delivery confirmed for next week");
        body.put("status", "DRAFT");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/procurement/purchase-orders/" + purchaseOrderId),
                HttpMethod.PUT, new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }
}
