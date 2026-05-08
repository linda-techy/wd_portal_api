package com.wd.api.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class InternalHmacVerifierTest {

    @Test
    void acceptsValidSignature() throws Exception {
        InternalHmacVerifier v = new InternalHmacVerifier();
        ReflectionTestUtils.setField(v, "sharedSecret", "shh");
        String body = "{\"crId\":42}";
        String sig = "sha256=" + hmacHex("shh", body);
        assertThat(v.verify(body, sig)).isTrue();
    }

    @Test
    void rejectsTamperedBody() throws Exception {
        InternalHmacVerifier v = new InternalHmacVerifier();
        ReflectionTestUtils.setField(v, "sharedSecret", "shh");
        String sig = "sha256=" + hmacHex("shh", "{\"crId\":42}");
        assertThat(v.verify("{\"crId\":99}", sig)).isFalse();
    }

    @Test
    void rejectsWhenSecretMissing() {
        InternalHmacVerifier v = new InternalHmacVerifier();
        ReflectionTestUtils.setField(v, "sharedSecret", "");
        assertThat(v.verify("{}", "sha256=abc")).isFalse();
    }

    @Test
    void rejectsMissingHeader() {
        InternalHmacVerifier v = new InternalHmacVerifier();
        ReflectionTestUtils.setField(v, "sharedSecret", "shh");
        assertThat(v.verify("{}", null)).isFalse();
    }

    @Test
    void rejectsHeaderWithoutSha256Prefix() {
        InternalHmacVerifier v = new InternalHmacVerifier();
        ReflectionTestUtils.setField(v, "sharedSecret", "shh");
        // Even if the hex bytes downstream were correct, the missing prefix
        // means a non-sha256 alg is being claimed — reject.
        assertThat(v.verify("{}", "deadbeef")).isFalse();
    }

    @Test
    void rejectsMalformedHex() {
        InternalHmacVerifier v = new InternalHmacVerifier();
        ReflectionTestUtils.setField(v, "sharedSecret", "shh");
        // Odd-length / non-hex input would blow up parseHex; we expect a clean
        // false return rather than a propagated IllegalArgumentException.
        assertThat(v.verify("{}", "sha256=zzz")).isFalse();
        assertThat(v.verify("{}", "sha256=abc")).isFalse(); // odd length
    }

    @Test
    void rejectsSignatureOfDifferentLength() throws Exception {
        // MessageDigest.isEqual returns false for unequal-length arrays in
        // constant time relative to the inputs it does have. We don't try to
        // measure timing here, just that the check still returns false.
        InternalHmacVerifier v = new InternalHmacVerifier();
        ReflectionTestUtils.setField(v, "sharedSecret", "shh");
        assertThat(v.verify("{}", "sha256=00")).isFalse();
    }

    private static String hmacHex(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
