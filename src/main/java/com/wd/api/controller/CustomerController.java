package com.wd.api.controller;

import com.wd.api.dto.CustomerCreateRequest;
import com.wd.api.dto.CustomerResponse;
import com.wd.api.dto.CustomerUpdateRequest;
import com.wd.api.dto.CustomerRoleDTO;
import com.wd.api.model.CustomerUser;
import com.wd.api.repository.CustomerUserRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.CustomerRoleRepository;
// import com.wd.api.utils.ValidationUtils; // TODO: Re-enable after testing
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize; // TODO: Re-enable after verifying role names
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/customers")
public class CustomerController {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);
    
    @Autowired
    private CustomerUserRepository customerUserRepository;
    
    @Autowired
    private CustomerProjectRepository customerProjectRepository;



    @Autowired
    private CustomerRoleRepository customerRoleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Get all customers (paginated)
     */
    @GetMapping("/paginated")
    public ResponseEntity<Page<CustomerResponse>> getCustomersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<CustomerUser> customerPage = customerUserRepository.findAll(pageable);
            Page<CustomerResponse> responsePage = customerPage.map(CustomerResponse::new);
            
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
    // @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // TODO: Re-enable after verifying role names in database
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        try {
            List<CustomerUser> customerUsers = customerUserRepository.findAll();
            List<CustomerResponse> customers = customerUsers.stream()
                    .map(CustomerResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(customers);
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
            Optional<CustomerUser> customerOpt = customerUserRepository.findById(id);
            if (customerOpt.isPresent()) {
                return ResponseEntity.ok(new CustomerResponse(customerOpt.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error fetching customer with ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Create new customer
     */
    @PostMapping
    // @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // TODO: Re-enable after verifying role names in database
    public ResponseEntity<?> createCustomer(@RequestBody CustomerCreateRequest request) {
        try {
            // Validate required fields
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Email is required");
            }
            if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("First name is required");
            }
            if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Last name is required");
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Password is required");
            }
            
            // Check if email already exists
            Optional<CustomerUser> existingCustomer = customerUserRepository.findByEmail(request.getEmail());
            if (existingCustomer.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
            }
            
            // Create new customer user
            CustomerUser customerUser = new CustomerUser();
            customerUser.setEmail(request.getEmail().trim());
            customerUser.setFirstName(request.getFirstName().trim());
            customerUser.setLastName(request.getLastName().trim());
            customerUser.setPassword(passwordEncoder.encode(request.getPassword().trim()));
            customerUser.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
            customerUser.setRoleId(request.getRoleId());
            
            CustomerUser savedCustomer = customerUserRepository.save(customerUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(new CustomerResponse(savedCustomer));
            
        } catch (Exception e) {
            logger.error("Error creating customer", e);
            return ResponseEntity.internalServerError().body("Error creating customer");
        }
    }
    
    /**
     * Update customer
     */
    @PutMapping("/{id}")
    // @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // TODO: Re-enable after verifying role names in database
    public ResponseEntity<?> updateCustomer(@PathVariable Long id, @RequestBody CustomerUpdateRequest request) {
        try {
            // Validate ID
            if (id == null) {
                return ResponseEntity.badRequest().body("Customer ID is required");
            }
            
            Optional<CustomerUser> customerOpt = customerUserRepository.findById(id);
            if (!customerOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            CustomerUser customerUser = customerOpt.get();
            
            // Validate required fields
            if (request == null) {
                return ResponseEntity.badRequest().body("Request body is required");
            }
            
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Email is required");
            }
            if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("First name is required");
            }
            if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Last name is required");
            }
            
            // Validate password only if provided
            if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                if (request.getPassword().trim().length() < 6) {
                    return ResponseEntity.badRequest().body("Password must be at least 6 characters");
                }
            }
            
            // Check if email is being changed and if new email already exists
            if (!customerUser.getEmail().equals(request.getEmail())) {
                Optional<CustomerUser> existingCustomer = customerUserRepository.findByEmail(request.getEmail());
                if (existingCustomer.isPresent()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
                }
            }
            
            // Update fields
            customerUser.setEmail(request.getEmail().trim());
            customerUser.setFirstName(request.getFirstName().trim());
            customerUser.setLastName(request.getLastName().trim());
            
            // Update password only if provided (leave empty to keep current)
            if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                try {
                    customerUser.setPassword(passwordEncoder.encode(request.getPassword().trim()));
                } catch (Exception e) {
                    logger.error("Error encoding password for customer ID: {}", id, e);
                    return ResponseEntity.internalServerError().body("Error encoding password");
                }
            }
            
            // Update enabled status
            if (request.getEnabled() != null) {
                customerUser.setEnabled(request.getEnabled());
            }
            
            // Update role ID if provided
            if (request.getRoleId() != null) {
                customerUser.setRoleId(request.getRoleId());
            }
            
            // Save the updated customer
            try {
                CustomerUser updatedCustomer = customerUserRepository.save(customerUser);
                return ResponseEntity.ok(new CustomerResponse(updatedCustomer));
            } catch (Exception e) {
                logger.error("Error saving customer with ID: {}", id, e);
                return ResponseEntity.internalServerError().body("Error saving customer");
            }
            
        } catch (IllegalArgumentException e) {
            logger.warn("Illegal argument error updating customer ID: {} - {}", id, e.getMessage());
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating customer with ID: {}", id, e);
            return ResponseEntity.internalServerError().body("Error updating customer");
        }
    }
    
    /**
     * Delete customer
     */
    @DeleteMapping("/{id}")
    // @PreAuthorize("hasRole('ADMIN')") // TODO: Re-enable after verifying role names in database
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id) {
        try {
            Optional<CustomerUser> customerOpt = customerUserRepository.findById(id);
            if (!customerOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            // Check for associated projects
            List<com.wd.api.model.CustomerProject> projects = customerProjectRepository.findByCustomerId(id);
            if (!projects.isEmpty()) {
                return ResponseEntity.badRequest().body("Cannot delete customer with associated projects. Please delete the projects first.");
            }
            
            customerUserRepository.deleteById(id);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Customer deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error deleting customer with ID: {}", id, e);
            return ResponseEntity.internalServerError().body("Error deleting customer");
        }
    }
    
    /**
     * Get all customer roles
     */
    @GetMapping("/roles")
    public ResponseEntity<List<CustomerRoleDTO>> getCustomerRoles() {
        try {
            List<CustomerRoleDTO> roles = customerRoleRepository.findAll().stream()
                    .map(CustomerRoleDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            logger.error("Error fetching customer roles", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

