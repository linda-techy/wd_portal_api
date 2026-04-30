package com.wd.api.service;

import com.wd.api.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Validates the fluid bag-of-fields {@code Map<String, Object>} that
 * the multipart {@code reportJson} part decodes into, plus the photo +
 * metadata lists that arrive alongside. Centralised so create / update /
 * addPhotos all enforce the same bounds — the alternative was scattered
 * checks that drifted (audit caught GPS bounds, future dates, photo
 * counts, and string lengths all unchecked).
 */
public final class SiteReportInputValidator {

    /** Per-report photo cap. 50 photos × ~1.5 MB ≈ 75 MB per upload — already aggressive. */
    public static final int MAX_PHOTOS_PER_REPORT = 50;

    /** Hard column limits from the SiteReport schema (V77). */
    private static final int MAX_TITLE_LEN = 255;
    private static final int MAX_WEATHER_LEN = 100;
    private static final int MAX_PHOTO_CAPTION_LEN = 255;

    private SiteReportInputValidator() {}

    public static void validateLatitude(Double lat, String fieldLabel) {
        if (lat == null) return;
        if (lat < -90.0 || lat > 90.0) {
            throw new BusinessException(
                    fieldLabel + " latitude " + lat + " is outside [-90, 90]",
                    HttpStatus.BAD_REQUEST, "INVALID_LATITUDE");
        }
    }

    public static void validateLongitude(Double lng, String fieldLabel) {
        if (lng == null) return;
        if (lng < -180.0 || lng > 180.0) {
            throw new BusinessException(
                    fieldLabel + " longitude " + lng + " is outside [-180, 180]",
                    HttpStatus.BAD_REQUEST, "INVALID_LONGITUDE");
        }
    }

    public static void validateNotInFuture(LocalDateTime when, String fieldLabel) {
        if (when == null) return;
        // Allow up to 5 minutes of clock skew between client and server —
        // a phone with a fast clock shouldn't be rejected.
        LocalDateTime cutoff = LocalDateTime.now().plusMinutes(5);
        if (when.isAfter(cutoff)) {
            throw new BusinessException(
                    fieldLabel + " cannot be in the future",
                    HttpStatus.BAD_REQUEST, "FUTURE_DATE_NOT_ALLOWED");
        }
    }

    public static void validateMaxLength(String value, int max, String fieldLabel) {
        if (value == null) return;
        if (value.length() > max) {
            throw new BusinessException(
                    fieldLabel + " exceeds maximum length of " + max + " characters",
                    HttpStatus.BAD_REQUEST, "FIELD_TOO_LONG");
        }
    }

    public static void validateTitleLen(String title) {
        validateMaxLength(title, MAX_TITLE_LEN, "title");
    }

    public static void validateWeatherLen(String weather) {
        validateMaxLength(weather, MAX_WEATHER_LEN, "weather");
    }

    public static void validatePhotoCaptionLen(String caption) {
        validateMaxLength(caption, MAX_PHOTO_CAPTION_LEN, "photo caption");
    }

    public static void validatePhotoCount(List<MultipartFile> photos) {
        if (photos == null) return;
        if (photos.size() > MAX_PHOTOS_PER_REPORT) {
            throw new BusinessException(
                    "Too many photos: " + photos.size() + ". Maximum is " + MAX_PHOTOS_PER_REPORT
                            + " per upload — split into multiple reports if needed.",
                    HttpStatus.BAD_REQUEST, "TOO_MANY_PHOTOS");
        }
    }

    /**
     * Metadata must either be null/empty (no per-photo metadata) OR exactly
     * match the photo count. Mismatch silently misaligned the captions to
     * the wrong photos before this guard.
     */
    public static void validateMetadataAlignsWithPhotos(
            List<Map<String, Object>> metadata, List<MultipartFile> photos) {
        if (metadata == null || metadata.isEmpty()) return;
        int photoCount = photos != null ? photos.size() : 0;
        if (metadata.size() != photoCount) {
            throw new BusinessException(
                    "Metadata list length (" + metadata.size()
                            + ") must match photo count (" + photoCount + ")",
                    HttpStatus.BAD_REQUEST, "METADATA_PHOTO_MISMATCH");
        }
    }
}
