package com.wd.api.module;

import com.wd.api.config.TestDataSeeder;
import com.wd.api.support.AuthTestHelper;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProjectModuleTest extends TestcontainersPostgresBase {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TestDataSeeder seeder;

    AuthTestHelper auth;

    /** Shared project ID across ordered tests. */
    private static Long createdProjectId;

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

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    void should_createProject() {
        HttpHeaders headers = adminHeaders();

        Map<String, Object> projectBody = new LinkedHashMap<>();
        projectBody.put("name", "Integration Test Villa");
        projectBody.put("location", "Bangalore");
        projectBody.put("project_type", "RESIDENTIAL");
        projectBody.put("state", "Karnataka");
        projectBody.put("district", "Bangalore Urban");
        projectBody.put("sqfeet", 2500);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(projectBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/customer-projects"), HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        assertThat(data.get("name")).isEqualTo("Integration Test Villa");

        // Store ID for subsequent tests
        createdProjectId = ((Number) data.get("id")).longValue();
        assertThat(createdProjectId).isPositive();
    }

    @Test
    @Order(2)
    void should_listProjects() {
        HttpHeaders headers = adminHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/customer-projects"), HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        // Paginated response has "content" with the list
        assertThat(data.get("content")).isNotNull();
    }

    @Test
    @Order(3)
    void should_getProjectById() {
        assertThat(createdProjectId).as("Project must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/customer-projects/" + createdProjectId),
                HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        assertThat(data.get("name")).isEqualTo("Integration Test Villa");
        assertThat(((Number) data.get("id")).longValue()).isEqualTo(createdProjectId);
    }

    @Test
    @Order(4)
    void should_updateProject() {
        assertThat(createdProjectId).as("Project must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        Map<String, Object> updateBody = new LinkedHashMap<>();
        updateBody.put("name", "Updated Villa Name");
        updateBody.put("location", "Mumbai");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updateBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/customer-projects/" + createdProjectId),
                HttpMethod.PUT, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);

        Map<String, Object> data = extractData(response.getBody());
        assertThat(data).isNotNull();
        assertThat(data.get("name")).isEqualTo("Updated Villa Name");
    }

    @Test
    @Order(5)
    void should_searchProjects() {
        HttpHeaders headers = adminHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/customer-projects/search?search=Villa"),
                HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @Order(6)
    void should_assignProjectMember() {
        assertThat(createdProjectId).as("Project must be created first").isNotNull();

        HttpHeaders headers = adminHeaders();

        // Assign customerA as a project member
        Long customerUserId = seeder.getCustomerA().getId();

        Map<String, Object> memberBody = new LinkedHashMap<>();
        memberBody.put("customer_user_id", customerUserId);
        memberBody.put("role_in_project", "OWNER");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(memberBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/customer-projects/" + createdProjectId + "/members"),
                HttpMethod.POST, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    @Test
    @Order(7)
    void should_deleteProject() {
        // Create a separate project for deletion so we don't affect other tests
        HttpHeaders headers = adminHeaders();

        Map<String, Object> projectBody = new LinkedHashMap<>();
        projectBody.put("name", "Project To Delete");
        projectBody.put("location", "Test Location");
        projectBody.put("project_type", "COMMERCIAL");

        HttpEntity<Map<String, Object>> createEntity = new HttpEntity<>(projectBody, headers);
        ResponseEntity<Map> createResponse = restTemplate.exchange(
                baseUrl("/customer-projects"), HttpMethod.POST, createEntity, Map.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> data = extractData(createResponse.getBody());
        Long deleteProjectId = ((Number) data.get("id")).longValue();

        // Now delete it
        HttpEntity<Void> deleteEntity = new HttpEntity<>(headers);
        ResponseEntity<Map> deleteResponse = restTemplate.exchange(
                baseUrl("/customer-projects/" + deleteProjectId),
                HttpMethod.DELETE, deleteEntity, Map.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deleteResponse.getBody()).isNotNull();
        assertThat(deleteResponse.getBody().get("success")).isEqualTo(true);
    }
}
