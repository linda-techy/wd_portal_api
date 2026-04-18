package com.wd.api.service;

import com.wd.api.model.CctvCamera;
import com.wd.api.model.CustomerProject;
import com.wd.api.repository.CctvCameraRepository;
import com.wd.api.repository.CustomerProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CctvCameraService {

    private final CctvCameraRepository cameraRepository;
    private final CustomerProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<CctvCamera> getProjectCameras(Long projectId) {
        return cameraRepository.findByProjectIdOrderByDisplayOrder(projectId);
    }

    @Transactional(readOnly = true)
    public List<CctvCamera> getActiveCameras(Long projectId) {
        return cameraRepository.findByProjectIdAndIsActiveTrueOrderByDisplayOrder(projectId);
    }

    @Transactional(readOnly = true)
    public CctvCamera getCamera(Long id) {
        return cameraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Camera not found"));
    }

    @Transactional
    public CctvCamera createCamera(Long projectId, CctvCamera camera) {
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        camera.setProject(project);
        return cameraRepository.save(camera);
    }

    @Transactional
    public CctvCamera updateCamera(Long id, CctvCamera updates) {
        CctvCamera existing = getCamera(id);
        if (updates.getCameraName() != null) existing.setCameraName(updates.getCameraName());
        if (updates.getLocation() != null) existing.setLocation(updates.getLocation());
        if (updates.getProvider() != null) existing.setProvider(updates.getProvider());
        if (updates.getStreamProtocol() != null) existing.setStreamProtocol(updates.getStreamProtocol());
        if (updates.getStreamUrl() != null) existing.setStreamUrl(updates.getStreamUrl());
        if (updates.getSnapshotUrl() != null) existing.setSnapshotUrl(updates.getSnapshotUrl());
        if (updates.getUsername() != null) existing.setUsername(updates.getUsername());
        if (updates.getPassword() != null) existing.setPassword(updates.getPassword());
        if (updates.getPort() != null) existing.setPort(updates.getPort());
        if (updates.getResolution() != null) existing.setResolution(updates.getResolution());
        if (updates.getInstallationDate() != null) existing.setInstallationDate(updates.getInstallationDate());
        if (updates.getDisplayOrder() != null) existing.setDisplayOrder(updates.getDisplayOrder());
        return cameraRepository.save(existing);
    }

    @Transactional
    public void deleteCamera(Long id) {
        CctvCamera camera = getCamera(id);
        cameraRepository.delete(camera); // soft-delete via @SQLDelete
    }

    @Transactional
    public CctvCamera toggleActive(Long id) {
        CctvCamera camera = getCamera(id);
        camera.setIsActive(!camera.getIsActive());
        return cameraRepository.save(camera);
    }
}
