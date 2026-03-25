package com.wd.api.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility for hashing refresh tokens before database storage.
 *
 * <p>Why: Storing raw JWT refresh tokens means a database breach directly yields
 * usable tokens. By storing only the SHA-256 hash, a breach exposes the hash but
 * not the raw token — an attacker cannot reconstruct the original JWT.
 *
 * <p>Usage:
 * <ul>
 *   <li>On save:   store {@code hash(rawToken)} in the DB column.
 *   <li>On lookup: query by {@code hash(rawToken)}, the client still sends the raw JWT.
 *   <li>On verify: hash the incoming token and compare against the stored hash.
 * </ul>
 *
 * <p>SHA-256 is appropriate here because:
 * <ul>
 *   <li>Refresh tokens are already high-entropy (JWT with 256-bit secret).
 *   <li>No need for slow KDF (bcrypt/argon2) — the token itself is already random.
 *   <li>Fast lookup is required; slow hashing would degrade every API request.
 * </ul>
 */
public final class TokenHashUtil {

    private TokenHashUtil() {}

    /**
     * Returns the SHA-256 hex hash of the given token string.
     * The result is a 64-character lowercase hex string.
     *
     * @param rawToken the plaintext JWT refresh token
     * @return 64-char lowercase hex SHA-256 digest
     * @throws IllegalStateException if SHA-256 is unavailable (should never happen on JDK 8+)
     */
    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by Java SE spec — this can never happen
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
