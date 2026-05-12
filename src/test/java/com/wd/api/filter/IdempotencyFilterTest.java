package com.wd.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.IdempotencyResponse;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.IdempotencyResponseRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the IdempotencyFilter against the real /api/projects/{id}/delays
 * controller (DelayLogController). Cases:
 *   1. First call persists a cache row.
 *   2. Second call (same key) replays cached body byte-for-byte without re-running the controller.
 *   3. Expired cache rows fall through and are replaced with a fresh response.
 *   4. Requests without the header are pass-through (no cache row created).
 *   5. Same key but different path does NOT deduplicate.
 *   6. Filter scope is limited to the 3 whitelisted path patterns.
 */
@AutoConfigureMockMvc
class IdempotencyFilterTest extends TestcontainersPostgresBase {

    @Autowired MockMvc mvc;
    @Autowired IdempotencyResponseRepository repo;
    @Autowired CustomerProjectRepository projects;
    @Autowired PortalUserRepository users;
    @Autowired ObjectMapper json;

    private Long projectId;

    @BeforeEach
    void clean() {
        repo.deleteAll();
        // Ensure a PortalUser with username "user" exists so the controller's
        // getCurrentUserId() resolves (default @WithMockUser principal).
        if (users.findByEmail("user").isEmpty()) {
            PortalUser u = new PortalUser();
            u.setEmail("user");
            u.setPassword("x");
            u.setFirstName("Test");
            u.setLastName("User");
            u.setEnabled(true);
            users.save(u);
        }
        // Seed a real project so DelayLogService.logDelay can resolve it.
        CustomerProject p = new CustomerProject();
        p.setName("idempotency-test " + UUID.randomUUID());
        p.setLocation("Test");
        p.setProjectUuid(UUID.randomUUID());
        p.setStartDate(LocalDate.of(2026, 6, 1));
        p = projects.save(p);
        this.projectId = p.getId();
    }

    private String validDelayJson() throws Exception {
        return json.writeValueAsString(Map.of(
                "delayType", "WEATHER",
                "fromDate", "2026-05-07",
                "reasonText", "rain"));
    }

    @Test @WithMockUser(authorities = "DELAY_CREATE")
    void firstRequestPersistsCacheRow() throws Exception {
        mvc.perform(post("/api/projects/" + projectId + "/delays")
                        .header("Idempotency-Key", "k-first")
                        .contentType("application/json")
                        .content(validDelayJson()))
                .andExpect(status().is2xxSuccessful());
        assertThat(repo.findById("k-first")).isPresent();
    }

    @Test @WithMockUser(authorities = "DELAY_CREATE")
    void secondRequestReturnsCachedBodyByteForByte() throws Exception {
        // Cache pre-populated for THIS project's path; filter should replay without invoking controller.
        String path = "/api/projects/" + projectId + "/delays";
        IdempotencyResponse cached = IdempotencyResponse.builder()
                .idempotencyKey("k-replay")
                .requestMethod("POST").requestPath(path)
                .responseStatus(201).responseBody("{\"id\":42,\"replay\":true}")
                .responseContentType("application/json")
                .cachedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        repo.save(cached);

        mvc.perform(post(path)
                        .header("Idempotency-Key", "k-replay")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().is(201))
                .andExpect(content().json("{\"id\":42,\"replay\":true}"))
                .andExpect(content().contentType("application/json"));
    }

    @Test @WithMockUser(authorities = "DELAY_CREATE")
    void expiredCacheRowFallsThroughAndReplaces() throws Exception {
        String path = "/api/projects/" + projectId + "/delays";
        repo.save(IdempotencyResponse.builder()
                .idempotencyKey("k-expired")
                .requestMethod("POST").requestPath(path)
                .responseStatus(201).responseBody("{\"id\":1,\"stale\":true}")
                .responseContentType("application/json")
                .cachedAt(LocalDateTime.now().minusHours(48))
                .expiresAt(LocalDateTime.now().minusHours(24))
                .build());

        mvc.perform(post(path)
                        .header("Idempotency-Key", "k-expired")
                        .contentType("application/json")
                        .content(validDelayJson()))
                .andExpect(status().is2xxSuccessful());

        IdempotencyResponse fresh = repo.findById("k-expired").orElseThrow();
        assertThat(fresh.getResponseBody()).doesNotContain("stale");
        assertThat(fresh.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test @WithMockUser(authorities = "DELAY_CREATE")
    void requestWithoutHeaderIsPassThrough() throws Exception {
        mvc.perform(post("/api/projects/" + projectId + "/delays")
                        .contentType("application/json")
                        .content(validDelayJson()))
                .andExpect(status().is2xxSuccessful());
        assertThat(repo.count()).isZero();
    }

    @Test @WithMockUser(authorities = "DELAY_CREATE")
    void differentMethodOrPathDoesNotDeduplicate() throws Exception {
        // Cache row exists for a DIFFERENT path (/api/site-reports). Same key
        // posted to /api/projects/{id}/delays must NOT replay.
        repo.save(IdempotencyResponse.builder()
                .idempotencyKey("k-shared")
                .requestMethod("POST").requestPath("/api/site-reports")
                .responseStatus(201).responseBody("{\"from\":\"site-reports\"}")
                .responseContentType("application/json")
                .cachedAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusHours(24))
                .build());

        mvc.perform(post("/api/projects/" + projectId + "/delays")
                        .header("Idempotency-Key", "k-shared")
                        .contentType("application/json")
                        .content(validDelayJson()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("from\":\"site-reports"))));
    }

    @Test @WithMockUser(authorities = "HOLIDAY_CREATE")
    void filterIsScopedToWhitelistedPathsOnly() throws Exception {
        // /api/admin/holidays is NOT in the filter's path scope. Even with a
        // header present, the filter must NOT persist a cache row.
        mvc.perform(post("/api/admin/holidays")
                        .header("Idempotency-Key", "k-out-of-scope")
                        .contentType("application/json")
                        .content("{}"));

        assertThat(repo.findById("k-out-of-scope")).isEmpty();
    }

    /** G-57: replaying the same key with a different body must surface a 409
     *  with a stable machine-readable code, not silently return the original
     *  cached 2xx. */
    @Test @WithMockUser(authorities = "DELAY_CREATE")
    void sameKeyWithDifferentBodyReturnsConflict() throws Exception {
        String path = "/api/projects/" + projectId + "/delays";
        String originalBody = validDelayJson();

        // Seed a cache row with the hash of `originalBody`.
        repo.save(IdempotencyResponse.builder()
                .idempotencyKey("k-conflict")
                .requestMethod("POST").requestPath(path)
                .responseStatus(201)
                .responseBody("{\"id\":99,\"original\":true}")
                .responseContentType("application/json")
                .requestBodyHash(sha256Hex(originalBody))
                .cachedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build());

        // Replay with a tampered body but the same key.
        String tampered = json.writeValueAsString(Map.of(
                "delayType", "MATERIAL_SHORTAGE",
                "fromDate", "2026-05-08",
                "reasonText", "different reason"));

        mvc.perform(post(path)
                        .header("Idempotency-Key", "k-conflict")
                        .contentType("application/json")
                        .content(tampered))
                .andExpect(status().is(409))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "IDEMPOTENCY_KEY_REPLAYED_WITH_DIFFERENT_BODY")));

        // Original cache row untouched.
        IdempotencyResponse stillThere = repo.findById("k-conflict").orElseThrow();
        assertThat(stillThere.getResponseBody()).contains("original");
    }

    /** G-57: replaying the same key with the same body still replays the
     *  cached response (regression — must not over-correct). */
    @Test @WithMockUser(authorities = "DELAY_CREATE")
    void sameKeyAndSameBodyStillReplays() throws Exception {
        String path = "/api/projects/" + projectId + "/delays";
        String body = validDelayJson();

        repo.save(IdempotencyResponse.builder()
                .idempotencyKey("k-replay-hash")
                .requestMethod("POST").requestPath(path)
                .responseStatus(201)
                .responseBody("{\"id\":7,\"replayed\":true}")
                .responseContentType("application/json")
                .requestBodyHash(sha256Hex(body))
                .cachedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build());

        mvc.perform(post(path)
                        .header("Idempotency-Key", "k-replay-hash")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().is(201))
                .andExpect(content().json("{\"id\":7,\"replayed\":true}"));
    }

    private static String sha256Hex(String s) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(64);
        for (byte b : d) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
