package com.wd.api.module;

import com.wd.api.config.TestDataSeeder;
import com.wd.api.support.AuthTestHelper;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Site Management module.
 * <p>
 * Covers site reports, site visits (check-in/check-out), quality checks,
 * observations, and delay logs.
 * <p>
 * Tests first create a project, then exercise each sub-module's create and
 * list endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SiteManagementModuleTest extends TestcontainersPostgresBase {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TestDataSeeder seeder;

    AuthTestHelper auth;

    /** Shared state across ordered tests. */
    private static Long projectId;
    private static Long siteVisitId;

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

    private HttpHeaders engineerHeaders() {
        return auth.authHeaders(auth.loginAsEngineer());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractData(Map<String, Object> body) {
        return (Map<String, Object>) body.get("data");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractDataList(Map<String, Object> body) {
        return (List<Map<String, Object>>) body.get("data");
    }

    /**
     * Creates a minimal 1x1 white PNG image as a byte array for multipart uploads.
     */
    private byte[] createDummyImage() {
        // Minimal valid PNG: 1x1 white pixel
        return new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, // IHDR chunk
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, // 1x1
            0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53,
            (byte) 0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, // IDAT chunk
            0x54, 0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF,
            (byte) 0xC0, 0x00, 0x00, 0x00, 0x02, 0x00, 0x01,
            (byte) 0xE2, 0x21, (byte) 0xBC, 0x33, 0x00, 0x00, 0x00,
            0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82 // IEND
        };
    }

    // ------------------------------------------------------------------
    // Setup: create a project
    // ------------------------------------------------------------------

    @Test
    @Order(0)
    void setup_createProject() {
        HttpHeaders headers = adminHeaders();

        Map<String, Object> projectBody = new LinkedHashMap<>();
        projectBody.put("name", "Site Management Test Project");
        projectBody.put("location", "Hubli");
        projectBody.put("project_type", "RESIDENTIAL");
        projectBody.put("state", "Karnataka");
        projectBody.put("district", "Dharwad");

        ResponseEntity<Map> projectResponse = restTemplate.exchange(
                baseUrl("/customer-projects"), HttpMethod.POST,
                new HttpEntity<>(projectBody, headers), Map.class);

        assertThat(projectResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        projectId = ((Number) extractData(projectResponse.getBody()).get("id")).longValue();
        assertThat(projectId).isPositive();
    }

    // ------------------------------------------------------------------
    // Site Report tests
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    void should_createSiteReport() {
        assertThat(projectId).as("Project must be created first").isNotNull();

        String token = auth.loginAsAdmin();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Site report creation requires multipart/form-data with photos
        String reportJson = String.format(
                "{\"title\":\"Foundation Inspection Report\",\"description\":\"Inspected foundation work\","
                + "\"projectId\":%d,\"reportType\":\"DAILY\",\"weather\":\"Clear\","
                + "\"manpowerDeployed\":15,\"workProgress\":\"Foundation 80%% complete\"}",
                projectId);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("report", reportJson);

        // Add a dummy photo (required by the controller)
        ByteArrayResource photo = new ByteArrayResource(createDummyImage()) {
            @Override
            public String getFilename() {
                return "test-photo.png";
            }
        };
        body.add("photos", photo);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/site-reports"), HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        assertThat(data.get("title")).isEqualTo("Foundation Inspection Report");
    }

    @Test
    @Order(2)
    void should_listSiteReports() {
        assertThat(projectId).as("Project must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/site-reports/project/" + projectId),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        List<Map<String, Object>> reports = extractDataList(response.getBody());
        assertThat(reports).isNotEmpty();
    }

    // ------------------------------------------------------------------
    // Site Visit tests (check-in/check-out)
    // ------------------------------------------------------------------

    @Test
    @Order(3)
    void should_createSiteVisit_viaCheckIn() {
        assertThat(projectId).as("Project must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("projectId", projectId);
        body.put("latitude", 15.3647);
        body.put("longitude", 75.1240);
        body.put("visitType", "SITE_ENGINEER");
        body.put("purpose", "Daily inspection");
        body.put("notes", "Checking concrete curing progress");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/site-visits/check-in"), HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        siteVisitId = ((Number) data.get("id")).longValue();
        assertThat(siteVisitId).isPositive();
    }

    @Test
    @Order(4)
    void should_listSiteVisits() {
        assertThat(projectId).as("Project must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/site-visits/project/" + projectId),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        List<Map<String, Object>> visits = extractDataList(response.getBody());
        assertThat(visits).isNotEmpty();
    }

    @Test
    @Order(5)
    void should_getSiteVisitById() {
        assertThat(siteVisitId).as("Site visit must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/site-visits/" + siteVisitId),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    // ------------------------------------------------------------------
    // Quality Check tests
    // ------------------------------------------------------------------

    @Test
    @Order(6)
    void should_createQualityCheck() {
        assertThat(projectId).as("Project must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("projectId", projectId);
        body.put("title", "Concrete Strength Test");
        body.put("description", "Testing concrete cube strength after 28 days");
        body.put("status", "PENDING");
        body.put("result", "AWAITING");
        body.put("remarks", "Samples collected from foundation pour");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/quality-checks"), HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
    }

    @Test
    @Order(7)
    void should_listQualityChecks() {
        assertThat(projectId).as("Project must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/quality-checks/project/" + projectId),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        List<Map<String, Object>> checks = extractDataList(response.getBody());
        assertThat(checks).isNotEmpty();
    }

    // ------------------------------------------------------------------
    // Observation tests
    // ------------------------------------------------------------------

    @Test
    @Order(8)
    void should_createObservation() {
        assertThat(projectId).as("Project must be created first").isNotNull();

        String token = auth.loginAsAdmin();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Observation creation uses multipart/form-data with @RequestParam fields
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("title", "Crack in foundation wall");
        body.add("description", "Hairline crack observed in north wall of foundation");
        body.add("location", "Block A, North Wall");
        body.add("priority", "HIGH");
        body.add("severity", "MAJOR");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/observations/project/" + projectId), HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        assertThat(data.get("title")).isEqualTo("Crack in foundation wall");
    }

    @Test
    @Order(9)
    void should_listObservations() {
        assertThat(projectId).as("Project must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/observations/project/" + projectId),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        List<Map<String, Object>> observations = extractDataList(response.getBody());
        assertThat(observations).isNotEmpty();
    }

    // ------------------------------------------------------------------
    // Delay Log tests
    // ------------------------------------------------------------------

    @Test
    @Order(10)
    void should_createDelayLog() {
        assertThat(projectId).as("Project must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reason", "Heavy rainfall");
        body.put("delayDays", 3);
        body.put("description", "Work halted due to continuous heavy rainfall for 3 days");
        body.put("startDate", "2026-04-15");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/api/projects/" + projectId + "/delays"), HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        // DelayLogController returns ResponseEntity<DelayLog> directly (no ApiResponse wrapper)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @Order(11)
    void should_listDelayLogs() {
        assertThat(projectId).as("Project must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        ResponseEntity<List> response = restTemplate.exchange(
                baseUrl("/api/projects/" + projectId + "/delays"),
                HttpMethod.GET, new HttpEntity<>(headers), List.class);

        // DelayLogController returns ResponseEntity<List<DelayLog>> directly
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isNotEmpty();
    }
}
