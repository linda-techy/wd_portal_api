package com.wd.api.service;

import com.wd.api.model.*;
import com.wd.api.repository.*;
import com.wd.api.util.NumberToWords;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.wd.api.dto.ChallanDtos;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
public class ChallanService {

    @Autowired
    private PaymentChallanRepository challanRepository;

    @Autowired
    private ChallanSequenceRepository sequenceRepository;

    @Autowired
    private PaymentTransactionRepository transactionRepository;

    @Autowired
    private TemplateEngine templateEngine;

    @Transactional
    public PaymentChallan generateChallan(Long transactionId, Long userId) {
        // Check if challan already exists
        return challanRepository.findByTransactionId(transactionId)
                .orElseGet(() -> createNewChallan(transactionId, userId));
    }

    @SuppressWarnings("null")
    private PaymentChallan createNewChallan(Long transactionId, Long userId) {
        PaymentTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!"COMPLETED".equals(tx.getStatus())) {
            throw new RuntimeException("Challan can only be generated for COMPLETED transactions");
        }

        String fy = getFinancialYear(tx.getPaymentDate());

        // Pessimistic lock on sequence for this FY
        ChallanSequence seq = sequenceRepository.findByFyWithLock(fy)
                .orElseGet(() -> {
                    ChallanSequence newSeq = new ChallanSequence();
                    newSeq.setFy(fy);
                    newSeq.setLastSequence(0);
                    return sequenceRepository.save(newSeq);
                });

        int nextVal = seq.getLastSequence() + 1;
        seq.setLastSequence(nextVal);
        sequenceRepository.save(seq);

        String challanNumber = String.format("WAL/CH/%s/%03d", fy, nextVal);

        PaymentChallan challan = new PaymentChallan();
        challan.setTransaction(tx);
        challan.setChallanNumber(challanNumber);
        challan.setFy(fy);
        challan.setSequenceNumber(nextVal);
        challan.setTransactionDate(tx.getPaymentDate());
        challan.setGeneratedById(userId);

        return challanRepository.save(challan);
    }

    @SuppressWarnings("null")
    public byte[] generateChallanPdf(Long challanId) {
        PaymentChallan challan = challanRepository.findById(challanId)
                .orElseThrow(() -> new RuntimeException("Challan not found"));

        Context context = new Context();
        context.setVariable("challan", challan);
        context.setVariable("transaction", challan.getTransaction());
        context.setVariable("amountInWords", NumberToWords.convert(challan.getTransaction().getAmount()));

        // Relationship graph for client/project info
        CustomerProject project = challan.getTransaction().getSchedule().getDesignPayment().getProject();
        context.setVariable("projectName", project.getName());
        context.setVariable("clientName",
                project.getCustomer() != null
                        ? project.getCustomer().getFirstName() + " " + project.getCustomer().getLastName()
                        : "N/A");

        String html = templateEngine.process("challan-template", context);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "/");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    @SuppressWarnings("null")
    public byte[] generateBulkZip(List<Long> challanIds) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (Long id : challanIds) {
                PaymentChallan challan = challanRepository.findById(id).orElse(null);
                if (challan == null)
                    continue;

                byte[] pdf = generateChallanPdf(id);
                String fileName = String.format("Walldot_Challan_%s_%s.pdf",
                        challan.getFy(), challan.getChallanNumber().replace("/", "_"));

                ZipEntry entry = new ZipEntry(fileName);
                zos.putNextEntry(entry);
                zos.write(pdf);
                zos.closeEntry();
            }
            zos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error creating ZIP", e);
        }
    }

    public List<ChallanDtos.ChallanResponse> searchChallans(ChallanDtos.ChallanFilterRequest filter) {
        Specification<PaymentChallan> spec = (Root<PaymentChallan> root, CriteriaQuery<?> query,
                CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.getFy() != null) {
                predicates.add(cb.equal(root.get("fy"), filter.getFy()));
            }
            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), filter.getStartDate()));
            }
            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), filter.getEndDate()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return challanRepository.findAll(spec).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ChallanDtos.ChallanResponse toResponse(PaymentChallan challan) {
        ChallanDtos.ChallanResponse res = new ChallanDtos.ChallanResponse();
        res.setId(challan.getId());
        res.setTransactionId(challan.getTransaction().getId());
        res.setChallanNumber(challan.getChallanNumber());
        res.setFy(challan.getFy());
        res.setTransactionDate(challan.getTransactionDate());
        res.setAmount(challan.getTransaction().getAmount());
        res.setGeneratedAt(challan.getGeneratedAt());
        res.setStatus(challan.getStatus());

        CustomerProject project = challan.getTransaction().getSchedule().getDesignPayment().getProject();
        res.setProjectName(project.getName());
        res.setClientName(project.getCustomer() != null
                ? project.getCustomer().getFirstName() + " " + project.getCustomer().getLastName()
                : "N/A");

        return res;
    }

    private String getFinancialYear(LocalDateTime date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        if (month < 4) {
            return (year - 1) + "-" + (year % 100);
        } else {
            return year + "-" + ((year + 1) % 100);
        }
    }
}
