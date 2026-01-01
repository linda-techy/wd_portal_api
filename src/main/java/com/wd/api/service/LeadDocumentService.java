package com.wd.api.service;

import com.wd.api.dao.model.Leads;
import com.wd.api.model.LeadDocument;
import com.wd.api.model.User;
import com.wd.api.repository.LeadDocumentRepository;
import com.wd.api.repository.LeadsRepository;
import com.wd.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class LeadDocumentService {

    @Autowired
    private LeadDocumentRepository leadDocumentRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private LeadsRepository leadsRepository;

    @Autowired
    private UserRepository userRepository;

    public List<LeadDocument> getDocumentsByLeadId(Long leadId) {
        return leadDocumentRepository.findByLeadLeadIdAndIsActiveTrue(leadId);
    }

    @Transactional
    public LeadDocument uploadDocument(Long leadId, MultipartFile file, String description, String category,
            Long userId) {
        Leads lead = leadsRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found with id: " + leadId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Store file
        String filePath = fileStorageService.storeFile(file, "leads/" + leadId);

        // Save metadata
        LeadDocument document = new LeadDocument();
        document.setLead(lead);
        document.setFilename(file.getOriginalFilename());
        document.setFilePath(filePath);
        document.setFileType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setDescription(description);
        document.setCategory(category);
        document.setUploadedBy(user);

        return leadDocumentRepository.save(document);
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        LeadDocument document = leadDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));

        document.setActive(false);
        leadDocumentRepository.save(document);
    }
}
