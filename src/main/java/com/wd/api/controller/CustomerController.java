package com.wd.api.controller;

import com.wd.api.dto.CustomerCreateRequest;
import com.wd.api.dto.CustomerResponse;
import com.wd.api.dto.CustomerUpdateRequest;
import com.wd.api.dto.CustomerRoleDTO;
import com.wd.api.model.CustomerUser;
import com.wd.api.repository.CustomerUserRepository;
import com.wd.api.repository.CustomerRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/customers")
public class CustomerController {
    
    @Autowired
    private CustomerUserRepository customerUserRepository;
    
    @Autowired
    private CustomerRoleRepository customerRoleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Get all customers
     */
    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        try {
            List<CustomerUser> customerUsers = customerUserRepository.findAll();
            List<CustomerResponse> customers = customerUsers.stream()
                    .map(CustomerResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(customers);
        } catch (Exception e) {
            System.err.println("Error fetching customers: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("Error fetching customer: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Create new customer
     */
    @PostMapping
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
            customerUser.setEmail(request.getEmail());
            customerUser.setFirstName(request.getFirstName());
            customerUser.setLastName(request.getLastName());
            customerUser.setPassword(passwordEncoder.encode(request.getPassword()));
            customerUser.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
            customerUser.setRoleId(request.getRoleId());
            
            CustomerUser savedCustomer = customerUserRepository.save(customerUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(new CustomerResponse(savedCustomer));
            
        } catch (Exception e) {
            System.err.println("Error creating customer: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error creating customer: " + e.getMessage());
        }
    }
    
    /**
     * Update customer
     */
    @PutMapping("/{id}")
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
                    System.err.println("Error encoding password: " + e.getMessage());
                    return ResponseEntity.internalServerError().body("Error encoding password: " + e.getMessage());
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
                System.err.println("Error saving customer: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.internalServerError().body("Error saving customer: " + e.getMessage());
            }
            
        } catch (IllegalArgumentException e) {
            System.err.println("Illegal argument error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error updating customer: " + e.getMessage());
            System.err.println("Exception type: " + e.getClass().getName());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error updating customer: " + e.getMessage());
        }
    }
    
    /**
     * Delete customer
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id) {
        try {
            Optional<CustomerUser> customerOpt = customerUserRepository.findById(id);
            if (!customerOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            customerUserRepository.deleteById(id);
            return ResponseEntity.ok().body("Customer deleted successfully");
            
        } catch (Exception e) {
            System.err.println("Error deleting customer: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error deleting customer: " + e.getMessage());
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
            System.err.println("Error fetching customer roles: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}

