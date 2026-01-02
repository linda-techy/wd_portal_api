package com.wd.api.service;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.ProjectWarranty;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.ProjectWarrantyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectWarrantyService {

    @Autowired
    private ProjectWarrantyRepository warrantyRepository;

    @Autowired
    private CustomerProjectRepository projectRepository;

    public List<ProjectWarranty> getWarrantiesByProject(Long projectId) {
        return warrantyRepository.findByProjectId(projectId);
    }

    @Transactional
    public ProjectWarranty createWarranty(ProjectWarranty warranty, Long projectId) {
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        warranty.setProject(project);

        // Auto-calculate status if null
        if (warranty.getStatus() == null) {
            warranty.setStatus("ACTIVE");
        }

        return warrantyRepository.save(warranty);
    }

    @Transactional
    public ProjectWarranty updateWarranty(Long id, ProjectWarranty details) {
        ProjectWarranty existing = warrantyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Warranty not found: " + id));

        existing.setComponentName(details.getComponentName());
        existing.setProviderName(details.getProviderName());
        existing.setDescription(details.getDescription());
        existing.setStartDate(details.getStartDate());
        existing.setEndDate(details.getEndDate());
        existing.setStatus(details.getStatus());
        existing.setCoverage_details(details.getCoverage_details());

        return warrantyRepository.save(existing);
    }

    @Transactional
    public void deleteWarranty(Long id) {
        warrantyRepository.deleteById(id);
    }
}
