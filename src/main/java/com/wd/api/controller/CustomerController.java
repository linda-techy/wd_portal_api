package com.wd.api.controller;

import com.wd.api.dto.CustomerCreateRequest;
import com.wd.api.dto.CustomerResponse;
import com.wd.api.dto.CustomerSearchFilter;
import com.wd.api.dto.CustomerUpdateRequest;
import com.wd.api.dto.CustomerRoleDTO;
import com.wd.api.dto.ApiResponse;
import com.wd.api.model.CustomerUser;
import com.wd.api.service.CustomerUserService;
import com.wd.api.service.CustomerPasswordResetService;
import com.wd.api.repository.CustomerRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    @Autowired
    private CustomerUserService customerUserService;

    @Autowired
    private CustomerPasswordResetService customerPasswordResetService;

    @Autowired
    private CustomerRoleRepository customerRoleRepository;

    /**
     * Search customers with filters
     */
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public ResponseEntity<Page<CustomerUser>> searchCustomers(@ModelAttribute CustomerSearchFilter filter) {
        try {
            Page<CustomerUser> customers = customerUserService.searchCustomers(filter);
            return ResponseEntity.ok(customers);
        } catch (Exception e) {
            logger.error("Error searching customers", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all customers (paginated)
     */
    @GetMapping("/paginated")
    @Deprecated
    public ResponseEntity<Page<CustomerResponse>> getCustomersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<CustomerResponse> responsePage = customerUserService.getAllCustomersPaginated(pageable);
            return ResponseEntity.ok(responsePage);
        } catch (Exception e) {
            logger.error("Error fetching paginated customers", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all customers
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers() {
        try {
            List<CustomerResponse> customers = customerUserService.getAllCustomers();
            return ResponseEntity.ok(ApiResponse.success("Customers retrieved successfully", customers));
        } catch (Exception e) {
            logger.error("Error fetching customers", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get customer by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        try {
            return customerUserService.getCustomerResponseById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error fetching customer with ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create new customer
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('CUSTOMER_CREATE', 'CUSTOMER_EDIT')")
    public ResponseEntity<?> createCustomer(@RequestBody CustomerCreateRequest request) {
        try {
            CustomerUser savedCustomer = customerUserService.createCustomer(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new CustomerResponse(savedCustomer));
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error creating customer: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating customer", e);
            return ResponseEntity.internalServerError().body("Error creating customer");
        }
    }

    /**
     * Update customer
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER_EDIT', 'CUSTOMER_CREATE')")
    public ResponseEntity<?> updateCustomer(@PathVariable Long id, @RequestBody CustomerUpdateRequest request) {
        try {
            CustomerUser updatedCustomer = customerUserService.updateCustomer(id, request);
            CustomerResponse response = new CustomerResponse(updatedCustomer);
            return customerUserService.getCustomerResponseById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.ok(response));
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error updating customer ID: {} - {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating customer with ID: {}", id, e);
            return ResponseEntity.internalServerError().body("Error updating customer");
        }
    }

    /**
     * Delete customer
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_DELETE')")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id) {
        try {
            customerUserService.deleteCustomer(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Customer deleted successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Customer not found for deletion: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.warn("Cannot delete customer with associated projects: {}", id);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Error deleting customer with ID: {}", id, e);
            return ResponseEntity.internalServerError().body("Error deleting customer");
        }
    }

    /**
     * Send a password-reset email to a customer.
     * Portal staff triggers this; the customer resets their password via the customer app.
     * Token is stored in the shared customer_password_reset_tokens table so the
     * customer API can validate it when the customer submits their new password.
     */
    @PostMapping("/{id}/send-password-reset")
    @PreAuthorize("hasAnyAuthority('CUSTOMER_EDIT', 'CUSTOMER_CREATE')")
    public ResponseEntity<ApiResponse<Void>> sendPasswordResetEmail(@PathVariable Long id) {
        try {
            customerPasswordResetService.sendPasswordResetEmail(id);
            return ResponseEntity.ok(ApiResponse.success(
                    "Password reset email sent to customer successfully", null));
        } catch (IllegalArgumentException e) {
            logger.warn("Cannot send password reset — {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            // Cooldown period active
            logger.warn("Password reset cooldown active for customer {}: {}", id, e.getMessage());
            return ResponseEntity.status(429).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error sending password reset for customer {}", id, e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("Failed to send password reset email"));
        }
    }

    /**
     * Get all customer roles
     */
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<CustomerRoleDTO>>> getCustomerRoles() {
        try {
            List<CustomerRoleDTO> roles = customerRoleRepository.findAll().stream()
                    .map(CustomerRoleDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success("Customer roles retrieved successfully", roles));
        } catch (Exception e) {
            logger.error("Error fetching customer roles", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Error fetching customer roles"));
        }
    }
}
