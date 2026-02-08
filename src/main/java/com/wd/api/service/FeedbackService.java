package com.wd.api.service;

import com.wd.api.dto.FeedbackFormDto;
import com.wd.api.dto.FeedbackResponseDto;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.FeedbackForm;
import com.wd.api.model.FeedbackResponse;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.FeedbackFormRepository;
import com.wd.api.repository.FeedbackResponseRepository;
import com.wd.api.repository.PortalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackFormRepository feedbackFormRepository;
    private final FeedbackResponseRepository feedbackResponseRepository;
    private final CustomerProjectRepository projectRepository;
    private final PortalUserRepository portalUserRepository;

    // ==================== FORM OPERATIONS ====================

    @Transactional
    public FeedbackFormDto createForm(Long projectId, String title, String description, 
                                       String formSchema, String creatorEmail) {
        CustomerProject project = projectRepository.findById(Objects.requireNonNull(projectId, "Project ID is required"))
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
        
        PortalUser creator = portalUserRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + creatorEmail));

        FeedbackForm form = new FeedbackForm();
        form.setProject(project);
        form.setTitle(title);
        form.setDescription(description);
        form.setFormSchema(formSchema);
        form.setCreatedBy(creator);
        form.setIsActive(true);

        FeedbackForm saved = feedbackFormRepository.save(form);
        return FeedbackFormDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<FeedbackFormDto> getProjectForms(Long projectId, Boolean activeOnly) {
        List<FeedbackForm> forms;
        if (Boolean.TRUE.equals(activeOnly)) {
            forms = feedbackFormRepository.findByProjectIdAndIsActiveTrueOrderByCreatedAtDesc(projectId);
        } else {
            forms = feedbackFormRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        }

        return forms.stream()
                .map(form -> {
                    FeedbackFormDto dto = FeedbackFormDto.fromEntity(form);
                    dto.setResponseCount(feedbackResponseRepository.countByFormId(form.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FeedbackFormDto getFormById(Long formId) {
        FeedbackForm form = feedbackFormRepository.findById(Objects.requireNonNull(formId, "Form ID is required"))
                .orElseThrow(() -> new RuntimeException("Feedback form not found: " + formId));
        
        FeedbackFormDto dto = FeedbackFormDto.fromEntity(form);
        dto.setResponseCount(feedbackResponseRepository.countByFormId(formId));
        return dto;
    }

    @Transactional
    public FeedbackFormDto updateForm(Long formId, String title, String description, 
                                       String formSchema, Boolean isActive) {
        FeedbackForm form = feedbackFormRepository.findById(Objects.requireNonNull(formId, "Form ID is required"))
                .orElseThrow(() -> new RuntimeException("Feedback form not found: " + formId));

        if (title != null) {
            form.setTitle(title);
        }
        if (description != null) {
            form.setDescription(description);
        }
        if (formSchema != null) {
            form.setFormSchema(formSchema);
        }
        if (isActive != null) {
            form.setIsActive(isActive);
        }

        FeedbackForm saved = feedbackFormRepository.save(Objects.requireNonNull(form));
        return FeedbackFormDto.fromEntity(saved);
    }

    @Transactional
    public void deactivateForm(Long formId) {
        FeedbackForm form = feedbackFormRepository.findById(Objects.requireNonNull(formId, "Form ID is required"))
                .orElseThrow(() -> new RuntimeException("Feedback form not found: " + formId));
        
        form.setIsActive(false);
        feedbackFormRepository.save(form);
    }

    @Transactional
    public void deleteForm(Long formId) {
        FeedbackForm form = feedbackFormRepository.findById(Objects.requireNonNull(formId, "Form ID is required"))
                .orElseThrow(() -> new RuntimeException("Feedback form not found: " + formId));
        
        // Check if form has responses - if so, deactivate instead of delete
        Long responseCount = feedbackResponseRepository.countByFormId(formId);
        if (responseCount > 0) {
            form.setIsActive(false);
            feedbackFormRepository.save(form);
        } else {
            feedbackFormRepository.delete(Objects.requireNonNull(form));
        }
    }

    // ==================== RESPONSE OPERATIONS ====================

    @Transactional(readOnly = true)
    public List<FeedbackResponseDto> getFormResponses(Long formId) {
        List<FeedbackResponse> responses = feedbackResponseRepository.findByFormIdOrderBySubmittedAtDesc(formId);
        return responses.stream()
                .map(FeedbackResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponseDto> getProjectResponses(Long projectId) {
        List<FeedbackResponse> responses = feedbackResponseRepository.findByProjectIdOrderBySubmittedAtDesc(projectId);
        return responses.stream()
                .map(FeedbackResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FeedbackResponseDto getResponseById(Long responseId) {
        FeedbackResponse response = feedbackResponseRepository.findById(Objects.requireNonNull(responseId, "Response ID is required"))
                .orElseThrow(() -> new RuntimeException("Feedback response not found: " + responseId));
        return FeedbackResponseDto.fromEntity(response);
    }

    // ==================== STATISTICS ====================

    @Transactional(readOnly = true)
    public Long getFormResponseCount(Long formId) {
        return feedbackResponseRepository.countByFormId(formId);
    }

    @Transactional(readOnly = true)
    public Long getActiveFormCount(Long projectId) {
        return feedbackFormRepository.countActiveFormsByProject(projectId);
    }
}
