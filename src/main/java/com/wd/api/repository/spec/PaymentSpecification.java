package com.wd.api.repository.spec;

import com.wd.api.model.DesignPackagePayment;
import com.wd.api.model.CustomerProject;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class PaymentSpecification {

    public static Specification<DesignPackagePayment> search(String search) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(search)) {
                return null;
            }

            String searchPattern = "%" + search.toLowerCase() + "%";

            // Join with Project
            Join<DesignPackagePayment, CustomerProject> projectJoin = root.join("project", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("packageName")), searchPattern),
                    cb.like(cb.lower(root.get("status")), searchPattern),
                    // Search by Project Name
                    cb.like(cb.lower(projectJoin.get("name")), searchPattern),
                    // Search by Project Code (if available)
                    cb.like(cb.lower(projectJoin.get("code")), searchPattern));
        };
    }

    public static Specification<DesignPackagePayment> statusNot(String status) {
        return (root, query, cb) -> cb.notEqual(root.get("status"), status);
    }
}
