package com.wd.api.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Verifies inbound HMAC-SHA256 signatures on /internal/** requests using
 * the shared secret with the customer-API.
 *
 * Header format mirrors the existing customer-API InternalWebhookController:
 *   X-Portal-Signature: sha256=&lt;hex&gt;
 *
 * (Header name is reused for symmetry — there is one shared secret between the
 * two APIs, signed by whichever side is the sender.)
 *
 * <p>Comparison is constant-time via {@link MessageDigest#isEqual(byte[], byte[])}
 * to avoid leaking match-prefix information to timing attackers.
 */
@Component
public class InternalHmacVerifier {

    private static final Logger log = LoggerFactory.getLogger(InternalHmacVerifier.class);
    private static final String ALGORITHM = "HmacSHA256";
    private static final String SIG_PREFIX = "sha256=";

    @Value("${customer-api.webhook-secret:}")
    private String sharedSecret;

    public boolean verify(String rawBody, String signatureHeader) {
        if (sharedSecret == null || sharedSecret.isBlank()) {
            log.warn("Internal HMAC secret not configured; rejecting all /internal calls");
            return false;
        }
        if (signatureHeader == null || signatureHeader.isBlank()) return false;
        if (!signatureHeader.startsWith(SIG_PREFIX)) return false;

        String expectedHex = signatureHeader.substring(SIG_PREFIX.length()).trim();
        byte[] expectedBytes;
        try {
            expectedBytes = HexFormat.of().parseHex(expectedHex);
        } catch (IllegalArgumentException e) {
            // Malformed hex (odd length / non-hex character) — reject without
            // doing the HMAC computation. Returning false here is safe: a
            // valid sender always emits even-length lowercase hex.
            return false;
        }

        byte[] computedBytes = computeHmacBytes(rawBody);
        // Constant-time comparison: leaks neither match-prefix length nor the
        // HMAC output through timing side channels.
        return MessageDigest.isEqual(computedBytes, expectedBytes);
    }

    private byte[] computeHmacBytes(String body) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // NoSuchAlgorithmException / InvalidKeyException — both are config
            // errors that should not be silently swallowed: fail loudly.
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }
}
