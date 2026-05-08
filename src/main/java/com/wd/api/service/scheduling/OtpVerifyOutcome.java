package com.wd.api.service.scheduling;

/**
 * Atomic outcome of an OTP verification: the result enum together with the
 * code hash that was matched against during the check. The hash is captured
 * before any state mutation so callers can audit it even when the row has
 * since been consumed (used_at set).
 *
 * <p>For non-VERIFIED outcomes other than NO_ACTIVE_TOKEN, the hash is the
 * value that was on the row at the moment of the check (useful for forensic
 * logging). For NO_ACTIVE_TOKEN there is no row, so hash is {@code null}.
 */
public record OtpVerifyOutcome(OtpVerifyResult result, String hash) {}
