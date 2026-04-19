package com.wd.api.support;

import com.wd.api.dto.LoginRequest;
import com.wd.api.dto.LoginResponse;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Helper for integration tests that need JWT authentication tokens.
 * <p>
 * Typical usage inside a {@code @SpringBootTest(webEnvironment = RANDOM_PORT)} test:
 * <pre>
 *   &#64;LocalServerPort int port;
 *   &#64;Autowired TestRestTemplate restTemplate;
 *
 *   AuthTestHelper auth = new AuthTestHelper(restTemplate, port);
 *   String token = auth.loginAsAdmin();
 *   HttpHeaders headers = auth.authHeaders(token);
 * </pre>
 */
public class AuthTestHelper {

    private static final String DEFAULT_PASSWORD = "password123";

    private final TestRestTemplate restTemplate;
    private final int port;

    public AuthTestHelper(TestRestTemplate restTemplate, int port) {
        this.restTemplate = restTemplate;
        this.port = port;
    }

    /**
     * Logs in as admin@test.com and returns the access token.
     */
    public String loginAsAdmin() {
        return login("admin@test.com", DEFAULT_PASSWORD);
    }

    /**
     * Logs in as pm@test.com (Project Manager) and returns the access token.
     */
    public String loginAsPM() {
        return login("pm@test.com", DEFAULT_PASSWORD);
    }

    /**
     * Logs in as accounts@test.com and returns the access token.
     */
    public String loginAsAccounts() {
        return login("accounts@test.com", DEFAULT_PASSWORD);
    }

    /**
     * Logs in as engineer@test.com (Site Engineer) and returns the access token.
     */
    public String loginAsEngineer() {
        return login("engineer@test.com", DEFAULT_PASSWORD);
    }

    /**
     * Posts credentials to {@code /auth/login} and returns the access token.
     *
     * @param email    user email
     * @param password plaintext password
     * @return JWT access token string
     * @throws AssertionError if the login request fails
     */
    public String login(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                loginUrl(), request, LoginResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Login failed for " + email + ": " + response.getStatusCode());
        assertNotNull(response.getBody(), "Login response body is null for " + email);
        assertNotNull(response.getBody().getAccessToken(),
                "Access token is null for " + email);

        return response.getBody().getAccessToken();
    }

    /**
     * Builds {@link HttpHeaders} with a Bearer authorization token and JSON content type.
     *
     * @param token JWT access token
     * @return ready-to-use headers for authenticated REST calls
     */
    public HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // ------------------------------------------------------------------
    // Private
    // ------------------------------------------------------------------

    private String loginUrl() {
        return "http://localhost:" + port + "/auth/login";
    }
}
