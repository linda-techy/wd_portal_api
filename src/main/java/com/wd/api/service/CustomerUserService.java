package com.wd.api.service;

import com.wd.api.dto.CustomerCreateRequest;
import com.wd.api.dto.CustomerResponse;
import com.wd.api.dto.CustomerUpdateRequest;
import com.wd.api.model.CustomerUser;
import com.wd.api.repository.CustomerUserRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.CustomerRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service layer for Customer User business logic
 * Handles customer CRUD operations with proper validation and transaction
 * management
 */
@Service
@Transactional
public class CustomerUserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerUserService.class);

    // Email validation pattern (RFC 5322 simplified)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Password pattern: min 8 chars, uppercase, lowercase, digit, special char
    private static final Pattern PASSWORD_PATTERN = Pattern
            .compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    @Autowired
    private CustomerUserRepository customerUserRepository;

    @Autowired
    private CustomerProjectRepository customerProjectRepository;

    @Autowired
    private CustomerRoleRepository customerRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Get all customers with pagination
     * Uses optimized query to avoid N+1 problem
     */
    @Transactional(readOnly = true)
    public Page<CustomerResponse> getAllCustomersPaginated(Pageable pageable) {
        // Optimized query to fetch customers and project counts in one go
        Page<Object[]> results = customerUserRepository.findAllCustomersWithProjectCountPaginated(pageable);

        return results.map(result -> {
            CustomerUser customer = (CustomerUser) result[0];
            Long projectCount = (Long) result[1];
            CustomerResponse response = new CustomerResponse(customer);
            response.setProjectCount(projectCount.intValue());
            return response;
        });
    }

    /**
     * Get all customers (non-paginated) - OPTIMIZED
     * Uses custom query to fetch all customers with project counts in single DB hit
     * Eliminates N+1 problem: 1 query instead of (1 + N) queries
     */
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        List<Object[]> results = customerUserRepository.findAllCustomersWithProjectCount();
        return results.stream()
                .map(result -> {
                    CustomerUser customer = (CustomerUser) result[0];
                    Long projectCount = (Long) result[1];
                    CustomerResponse response = new CustomerResponse(customer);
                    response.setProjectCount(projectCount.intValue());
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get customer by ID
     */
    @Transactional(readOnly = true)
    public Optional<CustomerUser> getCustomerById(Long id) {
        return customerUserRepository.findById(id);
    }

    /**
     * Get customer response with project count
     */
    @Transactional(readOnly = true)
    public Optional<CustomerResponse> getCustomerResponseById(Long id) {
        return customerUserRepository.findById(id)
                .map(customer -> {
                    CustomerResponse response = new CustomerResponse(customer);
                    response.setProjectCount(customerProjectRepository.countByCustomer_Id(id));
                    return response;
                });
    }

    /**
     * Create new customer
     */
    public CustomerUser createCustomer(CustomerCreateRequest request) {
        // Validate required fields
        validateCustomerCreateRequest(request);

        // Check if email already exists
        Optional<CustomerUser> existingCustomer = customerUserRepository.findByEmail(request.getEmail());
        if (existingCustomer.isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create new customer user
        CustomerUser customerUser = new CustomerUser();
        customerUser.setEmail(request.getEmail().trim());
        customerUser.setFirstName(request.getFirstName().trim());
        customerUser.setLastName(request.getLastName().trim());
        customerUser.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        customerUser.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

        // Map business fields
        customerUser.setPhone(request.getPhone());
        customerUser.setWhatsappNumber(request.getWhatsappNumber());
        customerUser.setAddress(request.getAddress());
        customerUser.setCompanyName(request.getCompanyName());
        customerUser.setGstNumber(request.getGstNumber());
        customerUser.setLeadSource(request.getLeadSource());
        customerUser.setNotes(request.getNotes());

        // Set role by looking up CustomerRole entity
        if (request.getRoleId() != null) {
            customerRoleRepository.findById(request.getRoleId()).ifPresent(customerUser::setRole);
        }

        return customerUserRepository.save(customerUser);
    }

    /**
     * Update existing customer
     */
    public CustomerUser updateCustomer(Long id, CustomerUpdateRequest request) {
        // Find existing customer
        CustomerUser customerUser = customerUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer with ID " + id + " not found"));

        // Validate request
        validateCustomerUpdateRequest(request);

        // Validate password strength if provided
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            if (request.getPassword().trim().length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters");
            }
        }

        // Check if email is being changed and if new email already exists
        if (!customerUser.getEmail().equals(request.getEmail())) {
            Optional<CustomerUser> existingCustomer = customerUserRepository.findByEmail(request.getEmail());
            if (existingCustomer.isPresent()) {
                throw new IllegalArgumentException("Email already exists");
            }
        }

        // Update fields
        customerUser.setEmail(request.getEmail().trim());
        customerUser.setFirstName(request.getFirstName().trim());
        customerUser.setLastName(request.getLastName().trim());

        // Update business fields
        if (request.getPhone() != null)
            customerUser.setPhone(request.getPhone());
        if (request.getWhatsappNumber() != null)
            customerUser.setWhatsappNumber(request.getWhatsappNumber());
        if (request.getAddress() != null)
            customerUser.setAddress(request.getAddress());
        if (request.getCompanyName() != null)
            customerUser.setCompanyName(request.getCompanyName());
        if (request.getGstNumber() != null)
            customerUser.setGstNumber(request.getGstNumber());
        if (request.getLeadSource() != null)
            customerUser.setLeadSource(request.getLeadSource());
        if (request.getNotes() != null)
            customerUser.setNotes(request.getNotes());

        // Update password only if provided
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            customerUser.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }

        // Update enabled status
        if (request.getEnabled() != null) {
            customerUser.setEnabled(request.getEnabled());
        }

        // Update role if provided
        if (request.getRoleId() != null) {
            customerRoleRepository.findById(request.getRoleId()).ifPresent(customerUser::setRole);
        }

        return customerUserRepository.save(customerUser);
    }

    /**
     * Delete customer
     * Checks for associated projects before deletion
     */
    public void deleteCustomer(Long id) {
        CustomerUser customer = customerUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer with ID " + id + " not found"));

        // Check for associated projects
        List<com.wd.api.model.CustomerProject> projects = customerProjectRepository.findByCustomer_Id(id);
        if (!projects.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete customer with associated projects. Please delete the projects first.");
        }

        customerUserRepository.delete(customer);
        logger.info("Customer deleted successfully - ID: {}", id);
    }

    // ==================== Private Validation Methods ====================

    private void validateCustomerCreateRequest(CustomerCreateRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        // Validate email format
        validateEmail(request.getEmail());

        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        // Validate password strength
        validatePassword(request.getPassword());
    }

    private void validateCustomerUpdateRequest(CustomerUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        // Validate email format
        validateEmail(request.getEmail());

        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
    }

    /**
     * Validate email format using regex pattern
     */
    private void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException(
                    "Invalid email format. Please provide a valid email address.");
        }
    }

    /**
     * Validate password strength
     * Requirements: min 8 chars, uppercase, lowercase, digit, special char
     * (@$!%*?&)
     */
    private void validatePassword(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "Password must contain at least one uppercase letter, one lowercase letter, " +
                            "one digit, and one special character (@$!%*?&)");
        }
    }
}
