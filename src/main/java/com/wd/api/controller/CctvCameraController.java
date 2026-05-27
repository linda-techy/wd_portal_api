package com.wd.api.controller;

import com.wd.api.dto.CctvCameraResponse;
import com.wd.api.model.CctvCamera;
import com.wd.api.service.CctvCameraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CctvCameraController {

    private final CctvCameraService cctvCameraService;

    @PostMapping("/projects/{projectId}/cctv-cameras")
    public ResponseEntity<CctvCameraResponse> createCamera(@PathVariable Long projectId,
                                                     @RequestBody CctvCamera camera) {
        return ResponseEntity.ok(CctvCameraResponse.from(cctvCameraService.createCamera(projectId, camera)));
    }

    @GetMapping("/projects/{projectId}/cctv-cameras")
    public ResponseEntity<List<CctvCameraResponse>> getProjectCameras(@PathVariable Long projectId) {
        return ResponseEntity.ok(cctvCameraService.getProjectCameras(projectId)
                .stream().map(CctvCameraResponse::from).toList());
    }

    @GetMapping("/cctv-cameras/{id}")
    public ResponseEntity<CctvCameraResponse> getCamera(@PathVariable Long id) {
        return ResponseEntity.ok(CctvCameraResponse.from(cctvCameraService.getCamera(id)));
    }

    @PutMapping("/cctv-cameras/{id}")
    public ResponseEntity<CctvCameraResponse> updateCamera(@PathVariable Long id,
                                                     @RequestBody CctvCamera updates) {
        return ResponseEntity.ok(CctvCameraResponse.from(cctvCameraService.updateCamera(id, updates)));
    }

    @DeleteMapping("/cctv-cameras/{id}")
    public ResponseEntity<Void> deleteCamera(@PathVariable Long id) {
        cctvCameraService.deleteCamera(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/cctv-cameras/{id}/toggle")
    public ResponseEntity<CctvCameraResponse> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(CctvCameraResponse.from(cctvCameraService.toggleActive(id)));
    }
}
