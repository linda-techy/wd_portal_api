package com.wd.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.model.CctvCamera;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.enums.StreamProtocol;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Locks the fix for the portal camera-list endpoint (bug #13). Returning the
 * raw {@link CctvCamera} entity (a) serialised the whole lazy CustomerProject
 * graph (project↔camera recursion risk under open-in-view) and (b) leaked the
 * camera username/password. {@link CctvCameraResponse} must carry only the
 * projectId FK and safe display fields.
 */
class CctvCameraResponseTest {

    // findAndRegisterModules() pulls in JavaTimeModule (jackson-datatype-jsr310),
    // matching the LocalDate/LocalDateTime handling Spring Boot configures at runtime.
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private CctvCamera sampleCamera() {
        CustomerProject project = mock(CustomerProject.class);
        when(project.getId()).thenReturn(49L);

        return CctvCamera.builder()
                .id(2L)
                .project(project)
                .cameraName("Main Gate")
                .location("Site entrance")
                .provider("Hikvision")
                .streamProtocol(StreamProtocol.HLS)
                .streamUrl("https://stream.example/main-gate.m3u8")
                .snapshotUrl("https://stream.example/main-gate.jpg")
                .username("admin")
                .password("s3cr3t-rtsp-pass")
                .port(554)
                .isActive(true)
                .resolution("1080p")
                .installationDate(LocalDate.of(2026, 4, 23))
                .displayOrder(0)
                .build();
    }

    @Test
    void mapsProjectToIdAndDisplayFields() {
        CctvCameraResponse dto = CctvCameraResponse.from(sampleCamera());

        assertThat(dto.id()).isEqualTo(2L);
        assertThat(dto.projectId()).isEqualTo(49L);
        assertThat(dto.cameraName()).isEqualTo("Main Gate");
        assertThat(dto.location()).isEqualTo("Site entrance");
        assertThat(dto.provider()).isEqualTo("Hikvision");
        assertThat(dto.streamProtocol()).isEqualTo("HLS");
        assertThat(dto.streamUrl()).isEqualTo("https://stream.example/main-gate.m3u8");
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.resolution()).isEqualTo("1080p");
        assertThat(dto.displayOrder()).isEqualTo(0);
    }

    @Test
    void serialisedJsonNeverLeaksCredentialsOrProjectGraph() throws Exception {
        String json = mapper.writeValueAsString(CctvCameraResponse.from(sampleCamera()));

        // Safe FK + display fields present
        assertThat(json).contains("\"projectId\":49");
        assertThat(json).contains("\"cameraName\":\"Main Gate\"");

        // RTSP credentials must never reach the client
        assertThat(json).doesNotContain("password");
        assertThat(json).doesNotContain("s3cr3t-rtsp-pass");
        assertThat(json).doesNotContain("username");
        assertThat(json).doesNotContain("admin");

        // No nested project entity graph (only the scalar projectId)
        assertThat(json).doesNotContain("\"project\":");
        assertThat(json).doesNotContain("projectUuid");
        assertThat(json).doesNotContain("customer");
    }
}
