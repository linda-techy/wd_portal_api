package com.wd.api.module;

import com.wd.api.config.TestDataSeeder;
import com.wd.api.dto.LoginRequest;
import com.wd.api.dto.LoginResponse;
import com.wd.api.dto.RefreshTokenRequest;
import com.wd.api.dto.RefreshTokenResponse;
import com.wd.api.support.AuthTestHelper;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthModuleTest extends TestcontainersPostgresBase {

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

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private LoginResponse loginAdmin() {
        LoginRequest request = new LoginRequest("admin@test.com", "password123");
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                baseUrl("/auth/login"), request, LoginResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    void should_loginWithValidCredentials() {
        LoginRequest request = new LoginRequest("admin@test.com", "password123");

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                baseUrl("/auth/login"), request, LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isNotBlank();
        assertThat(response.getBody().getRefreshToken()).isNotBlank();
        assertThat(response.getBody().getTokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().getExpiresIn()).isPositive();
        assertThat(response.getBody().getUser()).isNotNull();
        assertThat(response.getBody().getUser().getEmail()).isEqualTo("admin@test.com");
        assertThat(response.getBody().getPermissions()).isNotEmpty();
    }

    @Test
    @Order(2)
    void should_rejectInvalidPassword() {
        LoginRequest request = new LoginRequest("admin@test.com", "wrongpassword");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl("/auth/login"), request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(3)
    void should_rejectEmptyEmail() {
        LoginRequest request = new LoginRequest("", "password123");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl("/auth/login"), request, Map.class);

        // Blank email triggers either @NotBlank validation (400) or @Email validation (400)
        // or the controller's IllegalArgumentException handler (400)
        assertThat(response.getStatusCode().value()).isIn(400, 401);
    }

    @Test
    @Order(4)
    void should_refreshToken() {
        // First, log in to get a refresh token
        LoginResponse loginResponse = loginAdmin();
        assertThat(loginResponse.getRefreshToken()).isNotBlank();

        // Now use the refresh token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(loginResponse.getRefreshToken());
        ResponseEntity<RefreshTokenResponse> response = restTemplate.postForEntity(
                baseUrl("/auth/refresh-token"), refreshRequest, RefreshTokenResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isNotBlank();
        assertThat(response.getBody().getRefreshToken()).isNotBlank();
        assertThat(response.getBody().getTokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().getExpiresIn()).isPositive();
    }

    @Test
    @Order(5)
    void should_getCurrentUser() {
        String token = auth.loginAsAdmin();

        HttpHeaders headers = auth.authHeaders(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/auth/me"), HttpMethod.GET, entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("email")).isEqualTo("admin@test.com");
        assertThat(response.getBody().get("firstName")).isEqualTo("Admin");
        assertThat(response.getBody().get("lastName")).isEqualTo("User");
    }

    @Test
    @Order(6)
    void should_rejectUnauthenticatedAccess() {
        // Try to access a protected endpoint without a token
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl("/customer-projects"), Map.class);

        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }

    @Test
    @Order(7)
    void should_enforceRolePermissions() {
        // Login as SITE_ENGINEER who does NOT have PROJECT_CREATE permission
        String token = auth.loginAsEngineer();

        HttpHeaders headers = auth.authHeaders(token);

        // Build a project creation request body
        Map<String, Object> projectBody = Map.of(
                "name", "Unauthorized Project",
                "location", "Test Location",
                "project_type", "RESIDENTIAL"
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(projectBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl("/customer-projects"), HttpMethod.POST, entity, Map.class);

        // SITE_ENGINEER lacks PROJECT_CREATE — expect 403 Forbidden
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
