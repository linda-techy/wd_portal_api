package com.wd.api.service;

import com.wd.api.testsupport.TestcontainersPostgresBase;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-level assertions for {@link JwtService#extractActualSubject(String)}.
 *
 * <p>Uses a real Spring context (via {@link TestcontainersPostgresBase}) so that
 * {@code @Value} and {@code @PostConstruct} on JwtService are wired correctly.
 * Tokens are minted directly with JJWT using the same HMAC secret that
 * TestcontainersPostgresBase registers for the test environment.
 *
 * <p>Key regression covered: emails containing underscores (e.g.
 * {@code task_editor@test.com}) must NOT be truncated. Previously the guard
 * {@code subject.contains("_")} matched any email with an underscore and
 * silently rewrote it (e.g. to {@code editor@test.com}), causing 401s.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JwtServiceExtractSubjectTest extends TestcontainersPostgresBase {

    /** Secret must match the value registered in TestcontainersPostgresBase. */
    private static final String TEST_SECRET =
            "test-secret-do-not-use-in-prod-0123456789abcdef0123456789abcdef";

    @Autowired
    JwtService jwtService;

    private SecretKey key;

    @BeforeEach
    void setUp() {
        key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private String token(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    void plainEmailReturnsEmailUnchanged() {
        assertThat(jwtService.extractActualSubject(token("alice@example.com")))
                .isEqualTo("alice@example.com");
    }

    @Test
    void emailWithUnderscoreIsNotTruncated() {
        // Regression: old code stripped to "editor@test.com". Correct behaviour:
        // leave the underscore alone because '@' is present — it is a real email.
        assertThat(jwtService.extractActualSubject(token("task_editor@test.com")))
                .isEqualTo("task_editor@test.com");
        assertThat(jwtService.extractActualSubject(token("john_smith@company.com")))
                .isEqualTo("john_smith@company.com");
    }

    @Test
    void legacyPrefixIsStillStripped() {
        // Legacy tokens with UPPERCASE_ prefix (no '@') still strip the prefix.
        // Old format stored an opaque ID, not a full email, after the prefix.
        assertThat(jwtService.extractActualSubject(token("PARTNER_abc123")))
                .isEqualTo("abc123");
        assertThat(jwtService.extractActualSubject(token("CUSTOMER_userId99")))
                .isEqualTo("userId99");
    }

    @Test
    void subjectWithoutPrefixAndWithoutAtSignPassesThroughUnchanged() {
        // A subject that has no '@' but also doesn't match ALLCAPS_ should pass through.
        assertThat(jwtService.extractActualSubject(token("someToken123")))
                .isEqualTo("someToken123");
    }
}
