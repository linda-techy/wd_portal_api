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
import org.springframework.http.MediaType;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
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
     * Serves the HTML password-reset form to the customer.
     * Public — no authentication required (customer accesses from email link).
     * Token and email arrive as URL query params.
     */
    @GetMapping(value = "/reset-password-page", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> showResetPasswordPage(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(buildResetPasswordHtml(token, email, null, false));
    }

    /**
     * Processes the reset-password form submission.
     * Public — no authentication required.
     */
    @PostMapping(value = "/reset-password-confirm",
                 consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                 produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirmResetPassword(
            @RequestParam String token,
            @RequestParam String email,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                return ResponseEntity.ok(
                        buildResetPasswordHtml(token, email, "Passwords do not match.", false));
            }
            if (newPassword.length() < 8) {
                return ResponseEntity.ok(
                        buildResetPasswordHtml(token, email, "Password must be at least 8 characters.", false));
            }
            customerPasswordResetService.resetPassword(email, token, newPassword);
            return ResponseEntity.ok(buildResetPasswordHtml(null, null, null, true));
        } catch (IllegalArgumentException e) {
            logger.warn("Customer password reset failed: {}", e.getMessage());
            return ResponseEntity.ok(buildResetPasswordHtml(token, email, e.getMessage(), false));
        } catch (Exception e) {
            logger.error("Unexpected error during customer password reset", e);
            return ResponseEntity.ok(
                    buildResetPasswordHtml(token, email, "An unexpected error occurred. Please try again.", false));
        }
    }

    private String buildResetPasswordHtml(String token, String email, String error, boolean success) {
        if (success) {
            return """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                    <title>Password Reset - Walldot</title>
                    <style>
                      body{font-family:sans-serif;background:#f5f5f5;display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0}
                      .card{background:#fff;border-radius:8px;box-shadow:0 2px 12px rgba(0,0,0,.1);padding:40px;max-width:420px;width:90%;text-align:center}
                      .icon{font-size:48px;margin-bottom:16px}
                      h2{color:#2e7d32;margin:0 0 12px}
                      p{color:#555;line-height:1.6}
                    </style></head>
                    <body>
                    <div class="card">
                      <div class="icon">✅</div>
                      <h2>Password Reset Successful</h2>
                      <p>Your password has been updated. You can now log in to the Walldot customer app with your new password.</p>
                    </div>
                    </body></html>
                    """;
        }

        String errorHtml = error != null
                ? "<div class=\"error\">" + escapeHtml(error) + "</div>"
                : "";
        String tokenVal = token != null ? escapeHtml(token) : "";
        String emailVal = email != null ? escapeHtml(email) : "";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <title>Reset Password - Walldot</title>
                <style>
                  body{font-family:sans-serif;background:#f5f5f5;display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0}
                  .card{background:#fff;border-radius:8px;box-shadow:0 2px 12px rgba(0,0,0,.1);padding:40px;max-width:420px;width:90%}
                  .logo{text-align:center;margin-bottom:24px;font-size:22px;font-weight:700;color:#c62828}
                  h2{margin:0 0 8px;font-size:20px;color:#212121}
                  .sub{color:#777;font-size:14px;margin-bottom:24px}
                  label{display:block;font-size:13px;color:#555;margin-bottom:4px;margin-top:16px}
                  input[type=password]{width:100%;padding:10px 12px;border:1px solid #ddd;border-radius:6px;font-size:15px;box-sizing:border-box}
                  input[type=password]:focus{outline:none;border-color:#c62828}
                  button{margin-top:24px;width:100%;padding:12px;background:#c62828;color:#fff;border:none;border-radius:6px;font-size:15px;font-weight:600;cursor:pointer}
                  button:hover{background:#b71c1c}
                  .error{background:#fdecea;color:#c62828;border-radius:6px;padding:10px 14px;font-size:13px;margin-bottom:16px}
                  .hint{font-size:12px;color:#999;margin-top:4px}
                </style></head>
                <body>
                <div class="card">
                  <div class="logo">Walldot Builders</div>
                  <h2>Reset Your Password</h2>
                  <p class="sub">Enter a new password for <strong>%s</strong>. This link expires in 15 minutes.</p>
                  %s
                  <form method="POST" action="/api/customers/reset-password-confirm">
                    <input type="hidden" name="token" value="%s">
                    <input type="hidden" name="email" value="%s">
                    <label>New Password</label>
                    <input type="password" name="newPassword" placeholder="Min. 8 characters" required>
                    <label>Confirm Password</label>
                    <input type="password" name="confirmPassword" placeholder="Re-enter password" required>
                    <p class="hint">Password must be at least 8 characters.</p>
                    <button type="submit">Reset Password</button>
                  </form>
                </div>
                </body></html>
                """.formatted(emailVal, errorHtml, tokenVal, emailVal);
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
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
