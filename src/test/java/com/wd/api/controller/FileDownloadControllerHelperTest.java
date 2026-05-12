package com.wd.api.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * G-63 regression coverage for the path-traversal defence-in-depth helpers in
 * {@link FileDownloadController}. These rules sit upstream of the
 * {@code normalize() + startsWith(basePath)} containment check, so the unit
 * tests guard against silent regressions of the early-reject layer.
 */
class FileDownloadControllerHelperTest {

    // ---- hasParentSegment ----------------------------------------------------

    @Test
    void hasParentSegment_obviousTraversal() {
        assertThat(FileDownloadController.hasParentSegment("../etc/passwd")).isTrue();
        assertThat(FileDownloadController.hasParentSegment("projects/../secrets")).isTrue();
        assertThat(FileDownloadController.hasParentSegment("a/b/../../c")).isTrue();
    }

    @Test
    void hasParentSegment_backslashSeparators() {
        // Windows-style separators must also be rejected.
        assertThat(FileDownloadController.hasParentSegment("..\\windows\\system32")).isTrue();
        assertThat(FileDownloadController.hasParentSegment("foo\\..\\bar")).isTrue();
    }

    @Test
    void hasParentSegment_legitFilenames() {
        assertThat(FileDownloadController.hasParentSegment("projects/1/documents/file.pdf")).isFalse();
        assertThat(FileDownloadController.hasParentSegment("site-reports/10/image.png")).isFalse();
        // A filename containing "..", e.g. "..hidden", is NOT a parent segment.
        assertThat(FileDownloadController.hasParentSegment("docs/..hidden.txt")).isFalse();
        assertThat(FileDownloadController.hasParentSegment("docs/a..b/c")).isFalse();
    }

    @Test
    void hasParentSegment_whitespacePaddedDots() {
        // Defensive: " .. " between separators is still a parent segment.
        assertThat(FileDownloadController.hasParentSegment("foo/ .. /bar")).isTrue();
    }

    // ---- isAbsolutePathInput -------------------------------------------------

    @Test
    void isAbsolutePathInput_windowsDriveLetter() {
        assertThat(FileDownloadController.isAbsolutePathInput("C:\\Windows\\System32")).isTrue();
        assertThat(FileDownloadController.isAbsolutePathInput("D:/data/secrets.csv")).isTrue();
    }

    @Test
    void isAbsolutePathInput_uncRoot() {
        assertThat(FileDownloadController.isAbsolutePathInput("\\\\server\\share\\file")).isTrue();
        assertThat(FileDownloadController.isAbsolutePathInput("//server/share/file")).isTrue();
    }

    @Test
    void isAbsolutePathInput_legitRelative() {
        assertThat(FileDownloadController.isAbsolutePathInput("projects/1/file.pdf")).isFalse();
        assertThat(FileDownloadController.isAbsolutePathInput("file.txt")).isFalse();
        // A single leading slash is stripped before this helper runs.
        assertThat(FileDownloadController.isAbsolutePathInput("/projects/1/file.pdf")).isFalse();
    }
}
