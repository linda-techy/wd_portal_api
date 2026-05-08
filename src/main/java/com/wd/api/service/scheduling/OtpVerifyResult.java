package com.wd.api.service.scheduling;

public enum OtpVerifyResult {
    VERIFIED,
    NO_ACTIVE_TOKEN,
    EXPIRED,
    MAX_ATTEMPTS,
    WRONG_CODE
}
