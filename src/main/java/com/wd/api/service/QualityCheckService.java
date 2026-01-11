package com.wd.api.service;

import com.wd.api.model.QualityCheck;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.QualityCheckRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.PortalUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class QualityCheckService {

    @Autowired
    private QualityCheckRepository qualityCheckRepository;

    @Autowired
    private CustomerProjectRepository projectRepository;

    @Autowired
    private PortalUserRepository portalUserRepository;

    @Transactional(readOnly = true)
    public List<QualityCheck> getProjectChecks(Long projectId) {
        return qualityCheckRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public QualityCheck getCheckById(Long id) {
        return qualityCheckRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quality check not found with id: " + id));
    }

    @Transactional
    public QualityCheck createCheck(QualityCheck check, Long projectId, Long conductedById) {
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        PortalUser conductedBy = portalUserRepository.findById(conductedById)
                .orElseThrow(() -> new RuntimeException("User not found"));

        check.setProject(project);
        check.setConductedBy(conductedBy);

        if (check.getCheckDate() == null) {
            check.setCheckDate(LocalDateTime.now());
        }

        return qualityCheckRepository.save(check);
    }

    @Transactional
    public QualityCheck updateCheck(Long id, QualityCheck checkDetails) {
        QualityCheck check = getCheckById(id);

        check.setTitle(checkDetails.getTitle());
        check.setDescription(checkDetails.getDescription());
        check.setStatus(checkDetails.getStatus());
        check.setResult(checkDetails.getResult());
        check.setRemarks(checkDetails.getRemarks());

        return qualityCheckRepository.save(check);
    }

    @Transactional
    public void deleteCheck(Long id) {
        qualityCheckRepository.deleteById(id);
    }
}
