package com.wd.api.repository;

import com.wd.api.model.CustomerUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerUserRepository extends JpaRepository<CustomerUser, Long> {
    Optional<CustomerUser> findByEmail(String email);

    /**
     * Fetch all customers with project count in a single query
     * Eliminates N+1 problem by using LEFT JOIN and GROUP BY
     * Returns list of Object[] where:
     * - index 0: CustomerUser entity
     * - index 1: Long projectCount
     */
    @Query("SELECT c, COUNT(p.id) as projectCount " +
            "FROM CustomerUser c " +
            "LEFT JOIN CustomerProject p ON p.customer.id = c.id " +
            "GROUP BY c.id, c.email, c.firstName, c.lastName, c.password, c.enabled, c.role " +
            "ORDER BY c.id DESC")
    List<Object[]> findAllCustomersWithProjectCount();

    /**
     * Fetch paginated customers with project count in a single query
     * Eliminates N+1 problem for paginated lists
     */
    @Query(value = "SELECT c, COUNT(p.id) as projectCount " +
            "FROM CustomerUser c " +
            "LEFT JOIN CustomerProject p ON p.customer.id = c.id " +
            "GROUP BY c.id, c.email, c.firstName, c.lastName, c.password, c.enabled, c.role " +
            "ORDER BY c.id DESC", countQuery = "SELECT COUNT(c) FROM CustomerUser c")
    Page<Object[]> findAllCustomersWithProjectCountPaginated(Pageable pageable);
}
