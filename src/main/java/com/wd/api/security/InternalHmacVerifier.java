package com.wd.api.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Verifies inbound HMAC-SHA256 signatures on /internal/** requests using
 * the shared secret with the customer-API.
 *
 * Header format mirrors the existing customer-API InternalWebhookController:
 *   X-Portal-Signature: sha256=<hex>
 *
 * (Header name is reused for symmetry — there is one shared secret between the
 * two APIs, signed by whichever side is the sender.)
 */
@Component
public class InternalHmacVerifier {

    private static final Logger log = LoggerFactory.getLogger(InternalHmacVerifier.class);
    private static final String ALGORITHM = "HmacSHA256";

    @Value("${customer-api.webhook-secret:}")
    private String sharedSecret;

    public boolean verify(String rawBody, String signatureHeader) {
        if (sharedSecret == null || sharedSecret.isBlank()) {
            log.warn("Internal HMAC secret not configured; rejecting all /internal calls");
            return false;
        }
        if (signatureHeader == null || signatureHeader.isBlank()) return false;
        try {
            String expected = signatureHeader.startsWith("sha256=")
                ? signatureHeader.substring(7) : signatureHeader;
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).equalsIgnoreCase(expected);
        } catch (Exception e) {
            log.error("HMAC verification error: {}", e.getMessage());
            return false;
        }
    }
}
