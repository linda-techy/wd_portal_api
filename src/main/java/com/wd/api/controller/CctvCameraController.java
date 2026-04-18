package com.wd.api.controller;

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
    public ResponseEntity<CctvCamera> createCamera(@PathVariable Long projectId,
                                                     @RequestBody CctvCamera camera) {
        return ResponseEntity.ok(cctvCameraService.createCamera(projectId, camera));
    }

    @GetMapping("/projects/{projectId}/cctv-cameras")
    public ResponseEntity<List<CctvCamera>> getProjectCameras(@PathVariable Long projectId) {
        return ResponseEntity.ok(cctvCameraService.getProjectCameras(projectId));
    }

    @GetMapping("/cctv-cameras/{id}")
    public ResponseEntity<CctvCamera> getCamera(@PathVariable Long id) {
        return ResponseEntity.ok(cctvCameraService.getCamera(id));
    }

    @PutMapping("/cctv-cameras/{id}")
    public ResponseEntity<CctvCamera> updateCamera(@PathVariable Long id,
                                                     @RequestBody CctvCamera updates) {
        return ResponseEntity.ok(cctvCameraService.updateCamera(id, updates));
    }

    @DeleteMapping("/cctv-cameras/{id}")
    public ResponseEntity<Void> deleteCamera(@PathVariable Long id) {
        cctvCameraService.deleteCamera(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/cctv-cameras/{id}/toggle")
    public ResponseEntity<CctvCamera> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(cctvCameraService.toggleActive(id));
    }
}
