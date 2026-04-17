package com.wd.api.service;

import com.wd.api.security.JwtConstants;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class JwtAudienceTest extends TestcontainersPostgresBase {

    @Autowired
    private JwtService jwtService;

    @Value("${jwt.secret}")
    private String secret;

    private UserDetails user() {
        return new User("testuser@t.com", "x", Collections.emptyList());
    }

    @Test
    void case1_newlyIssuedTokenCarriesAudClaim() {
        String token = jwtService.generateAccessToken(user());
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Set<String> aud = claims.getAudience();
        assertThat(aud).contains(JwtConstants.AUDIENCE_PORTAL);
    }

    @Test
    void case2_correctAudEnforceFalseValidates(CapturedOutput output) {
        ReflectionTestUtils.setField(jwtService, "audEnforce", false);
        String token = jwtService.generateAccessToken(user());
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(output.getAll()).doesNotContain("JWT missing or mismatched aud claim");
    }

    @Test
    void case3_missingAudEnforceFalseValidatesAndLogsWarn(CapturedOutput output) {
        ReflectionTestUtils.setField(jwtService, "audEnforce", false);
        String token = tokenWithoutAud();
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(output.getAll()).contains("JWT missing or mismatched aud claim");
    }

    @Test
    void case4_wrongAudEnforceFalseValidatesAndLogsWarn(CapturedOutput output) {
        ReflectionTestUtils.setField(jwtService, "audEnforce", false);
        String token = tokenWithAud(JwtConstants.AUDIENCE_CUSTOMER);
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(output.getAll()).contains("JWT missing or mismatched aud claim");
    }

    @Test
    void case5_correctAudEnforceTrueValidates() {
        ReflectionTestUtils.setField(jwtService, "audEnforce", true);
        try {
            String token = jwtService.generateAccessToken(user());
            assertThat(jwtService.validateToken(token)).isTrue();
        } finally {
            ReflectionTestUtils.setField(jwtService, "audEnforce", false);
        }
    }

    @Test
    void case6_missingAudEnforceTrueRejects() {
        ReflectionTestUtils.setField(jwtService, "audEnforce", true);
        try {
            String token = tokenWithoutAud();
            assertThat(jwtService.validateToken(token)).isFalse();
        } finally {
            ReflectionTestUtils.setField(jwtService, "audEnforce", false);
        }
    }

    @Test
    void preChangeTokenStillWorksWhenEnforceFalse() {
        ReflectionTestUtils.setField(jwtService, "audEnforce", false);
        String legacyToken = tokenWithoutAud();
        assertThat(jwtService.validateToken(legacyToken)).isTrue();
    }

    private String tokenWithoutAud() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        HashMap<String, Object> claims = new HashMap<>();
        claims.put(JwtConstants.CLAIM_TOKEN_TYPE, JwtConstants.TOKEN_TYPE_PORTAL);
        return Jwts.builder().claims(claims).subject("testuser@t.com")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    private String tokenWithAud(String audValue) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        HashMap<String, Object> claims = new HashMap<>();
        claims.put(JwtConstants.CLAIM_TOKEN_TYPE, JwtConstants.TOKEN_TYPE_PORTAL);
        return Jwts.builder().claims(claims).subject("testuser@t.com")
                .audience().add(audValue).and()
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key, Jwts.SIG.HS256).compact();
    }
}
