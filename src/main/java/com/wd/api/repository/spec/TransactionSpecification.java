package com.wd.api.repository.spec;

import com.wd.api.model.PaymentTransaction;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class TransactionSpecification {

    public static Specification<PaymentTransaction> search(String query) {
        return (root, criteriaQuery, cb) -> {
            if (query == null || query.isBlank()) {
                return null;
            }
            String pattern = "%" + query.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("receiptNumber")), pattern),
                    cb.like(cb.lower(root.get("referenceNumber")), pattern),
                    cb.like(cb.lower(root.get("notes")), pattern));
        };
    }

    public static Specification<PaymentTransaction> dateBetween(LocalDateTime start, LocalDateTime end) {
        return (root, criteriaQuery, cb) -> {
            if (start == null && end == null)
                return null;
            if (start != null && end != null)
                return cb.between(root.get("paymentDate"), start, end);
            if (start != null)
                return cb.greaterThanOrEqualTo(root.get("paymentDate"), start);
            return cb.lessThanOrEqualTo(root.get("paymentDate"), end);
        };
    }

    public static Specification<PaymentTransaction> methodIs(String method) {
        return (root, criteriaQuery, cb) -> {
            if (method == null || method.isBlank())
                return null;
            return cb.equal(root.get("paymentMethod"), method);
        };
    }

    public static Specification<PaymentTransaction> statusIs(String status) {
        return (root, criteriaQuery, cb) -> {
            if (status == null || status.isBlank())
                return null;
            return cb.equal(root.get("status"), status);
        };
    }
}
