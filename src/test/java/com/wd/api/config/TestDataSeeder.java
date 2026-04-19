package com.wd.api.config;

import com.wd.api.model.ActivityType;
import com.wd.api.model.CustomerRole;
import com.wd.api.model.CustomerUser;
import com.wd.api.model.Permission;
import com.wd.api.model.PortalRole;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.ActivityTypeRepository;
import com.wd.api.repository.CustomerRoleRepository;
import com.wd.api.repository.CustomerUserRepository;
import com.wd.api.repository.PermissionRepository;
import com.wd.api.repository.PortalRoleRepository;
import com.wd.api.repository.PortalUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Seeds baseline test data (roles, permissions, users) for integration tests.
 * <p>
 * Usage: autowire this component in any {@code @SpringBootTest} and call {@link #seed()}.
 * The method is idempotent -- it checks for existing data before inserting.
 * <p>
 * Created entities are accessible via getters so that tests can reference IDs,
 * tokens, or relationships without additional lookups.
 */
@Component
public class TestDataSeeder {

    private static final String DEFAULT_PASSWORD = "password123";

    private static final List<String> ALL_PERMISSIONS = List.of(
            "BOQ_VIEW", "BOQ_CREATE", "BOQ_EDIT", "BOQ_DELETE", "BOQ_APPROVE", "BOQ_EXPORT",
            "PROJECT_VIEW", "PROJECT_CREATE", "PROJECT_EDIT", "PROJECT_DELETE",
            "INVOICE_VIEW", "INVOICE_CREATE",
            "PAYMENT_VIEW", "PAYMENT_CREATE",
            "CHANGE_ORDER_VIEW", "CHANGE_ORDER_CREATE", "CHANGE_ORDER_APPROVE",
            "SITE_REPORT_VIEW", "SITE_REPORT_CREATE",
            "SITE_VISIT_VIEW", "SITE_VISIT_CREATE",
            "LABOUR_VIEW", "LABOUR_CREATE",
            "PROCUREMENT_VIEW", "PROCUREMENT_CREATE",
            "EXPORT_VIEW",
            "STAGE_VIEW", "DELAY_VIEW", "DELAY_CREATE"
    );

    /**
     * Activity type names required by services exercised in integration tests.
     * Without these, BOQ workflow and Change Order workflow calls to
     * {@code ActivityFeedService.logProjectActivity()} throw RuntimeException.
     */
    private static final List<String> REQUIRED_ACTIVITY_TYPES = List.of(
            "BOQ_SUBMITTED", "BOQ_APPROVED", "BOQ_REJECTED",
            "CO_INTERNALLY_APPROVED", "CO_INTERNALLY_REJECTED",
            "CO_SENT_TO_CUSTOMER", "CO_APPROVED", "CO_REJECTED",
            "LEAD_CONVERTED"
    );

    /**
     * Role-code to permission-name mapping.
     * ADMIN gets everything; other roles get a relevant subset.
     */
    private static final Map<String, List<String>> ROLE_PERMISSIONS = new LinkedHashMap<>();

    static {
        ROLE_PERMISSIONS.put("ADMIN", ALL_PERMISSIONS);
        ROLE_PERMISSIONS.put("PROJECT_MANAGER", List.of(
                "BOQ_VIEW", "BOQ_CREATE", "BOQ_EDIT", "BOQ_APPROVE", "BOQ_EXPORT",
                "PROJECT_VIEW", "PROJECT_CREATE", "PROJECT_EDIT",
                "CHANGE_ORDER_VIEW", "CHANGE_ORDER_CREATE", "CHANGE_ORDER_APPROVE",
                "SITE_REPORT_VIEW", "SITE_REPORT_CREATE",
                "LABOUR_VIEW", "LABOUR_CREATE",
                "PROCUREMENT_VIEW", "PROCUREMENT_CREATE",
                "INVOICE_VIEW", "PAYMENT_VIEW", "EXPORT_VIEW"
        ));
        ROLE_PERMISSIONS.put("SITE_ENGINEER", List.of(
                "BOQ_VIEW", "BOQ_EXPORT",
                "PROJECT_VIEW",
                "CHANGE_ORDER_VIEW",
                "SITE_REPORT_VIEW", "SITE_REPORT_CREATE",
                "LABOUR_VIEW", "LABOUR_CREATE"
        ));
        ROLE_PERMISSIONS.put("ACCOUNTS", List.of(
                "BOQ_VIEW", "BOQ_EXPORT",
                "PROJECT_VIEW",
                "INVOICE_VIEW", "INVOICE_CREATE",
                "PAYMENT_VIEW", "PAYMENT_CREATE",
                "CHANGE_ORDER_VIEW",
                "EXPORT_VIEW"
        ));
    }

    @Autowired
    private ActivityTypeRepository activityTypeRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PortalRoleRepository portalRoleRepository;

    @Autowired
    private PortalUserRepository portalUserRepository;

    @Autowired
    private CustomerRoleRepository customerRoleRepository;

    @Autowired
    private CustomerUserRepository customerUserRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    // --- Seeded entities (populated after seed()) ---

    private Map<String, Permission> permissions;
    private Map<String, PortalRole> portalRoles;
    private Map<String, PortalUser> portalUsers;
    private CustomerRole customerRole;
    private Map<String, CustomerUser> customerUsers;

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Seed all baseline test data. Safe to call multiple times -- existing data
     * is detected via email/name lookups and reused rather than re-created.
     */
    public void seed() {
        seedPermissions();
        seedPortalRoles();
        seedPortalUsers();
        seedCustomerRoleAndUsers();
        seedActivityTypes();
    }

    // --- Permission getters ---

    public Permission getPermission(String name) {
        return permissions.get(name);
    }

    public Map<String, Permission> getPermissions() {
        return permissions;
    }

    // --- Portal role getters ---

    public PortalRole getAdminRole() {
        return portalRoles.get("ADMIN");
    }

    public PortalRole getProjectManagerRole() {
        return portalRoles.get("PROJECT_MANAGER");
    }

    public PortalRole getSiteEngineerRole() {
        return portalRoles.get("SITE_ENGINEER");
    }

    public PortalRole getAccountsRole() {
        return portalRoles.get("ACCOUNTS");
    }

    public Map<String, PortalRole> getPortalRoles() {
        return portalRoles;
    }

    // --- Portal user getters ---

    public PortalUser getAdmin() {
        return portalUsers.get("admin@test.com");
    }

    public PortalUser getProjectManager() {
        return portalUsers.get("pm@test.com");
    }

    public PortalUser getSiteEngineer() {
        return portalUsers.get("engineer@test.com");
    }

    public PortalUser getAccountsUser() {
        return portalUsers.get("accounts@test.com");
    }

    public Map<String, PortalUser> getPortalUsers() {
        return portalUsers;
    }

    // --- Customer getters ---

    public CustomerRole getCustomerRole() {
        return customerRole;
    }

    public CustomerUser getCustomerA() {
        return customerUsers.get("customerA@test.com");
    }

    public CustomerUser getCustomerB() {
        return customerUsers.get("customerB@test.com");
    }

    public CustomerUser getCustomerC() {
        return customerUsers.get("customerC@test.com");
    }

    public Map<String, CustomerUser> getCustomerUsers() {
        return customerUsers;
    }

    // ------------------------------------------------------------------
    // Internal seeding methods
    // ------------------------------------------------------------------

    private void seedPermissions() {
        permissions = new LinkedHashMap<>();
        for (String name : ALL_PERMISSIONS) {
            Permission perm = permissionRepository.findByName(name).orElseGet(() -> {
                Permission p = new Permission();
                p.setName(name);
                p.setDescription("Test permission: " + name);
                return permissionRepository.save(p);
            });
            permissions.put(name, perm);
        }
    }

    private void seedPortalRoles() {
        portalRoles = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : ROLE_PERMISSIONS.entrySet()) {
            String code = entry.getKey();
            List<String> permNames = entry.getValue();

            PortalRole role = portalRoleRepository.findByCode(code).orElseGet(() -> {
                PortalRole r = new PortalRole();
                r.setName(code.replace("_", " "));
                r.setCode(code);
                r.setDescription("Test role: " + code);
                return r;
            });

            Set<Permission> rolePerms = permNames.stream()
                    .map(permissions::get)
                    .collect(Collectors.toSet());
            role.setPermissions(rolePerms);

            portalRoles.put(code, portalRoleRepository.save(role));
        }
    }

    private void seedPortalUsers() {
        portalUsers = new LinkedHashMap<>();
        String encoded = passwordEncoder.encode(DEFAULT_PASSWORD);

        createPortalUser("admin@test.com", "Admin", "User", "ADMIN", encoded);
        createPortalUser("pm@test.com", "Project", "Manager", "PROJECT_MANAGER", encoded);
        createPortalUser("engineer@test.com", "Site", "Engineer", "SITE_ENGINEER", encoded);
        createPortalUser("accounts@test.com", "Accounts", "Officer", "ACCOUNTS", encoded);
    }

    private void createPortalUser(String email, String firstName, String lastName,
                                  String roleCode, String encodedPassword) {
        PortalUser user = portalUserRepository.findByEmail(email).orElseGet(() -> {
            PortalUser u = new PortalUser();
            u.setEmail(email);
            u.setPassword(encodedPassword);
            u.setFirstName(firstName);
            u.setLastName(lastName);
            u.setRole(portalRoles.get(roleCode));
            u.setEnabled(true);
            return portalUserRepository.save(u);
        });
        portalUsers.put(email, user);
    }

    private void seedCustomerRoleAndUsers() {
        customerRole = customerRoleRepository.findByName("CUSTOMER").orElseGet(() -> {
            CustomerRole r = new CustomerRole();
            r.setName("CUSTOMER");
            r.setDescription("Default customer role");
            return customerRoleRepository.save(r);
        });

        customerUsers = new LinkedHashMap<>();
        String encoded = passwordEncoder.encode(DEFAULT_PASSWORD);

        createCustomerUser("customerA@test.com", "Alice", "Customer", "9876543210", encoded);
        createCustomerUser("customerB@test.com", "Bob", "Customer", "9876543211", encoded);
        createCustomerUser("customerC@test.com", "Charlie", "Customer", "9876543212", encoded);
    }

    private void createCustomerUser(String email, String firstName, String lastName,
                                    String phone, String encodedPassword) {
        CustomerUser user = customerUserRepository.findByEmail(email).orElseGet(() -> {
            CustomerUser u = new CustomerUser();
            u.setEmail(email);
            u.setPassword(encodedPassword);
            u.setFirstName(firstName);
            u.setLastName(lastName);
            u.setPhone(phone);
            u.setRole(customerRole);
            u.setEnabled(true);
            return customerUserRepository.save(u);
        });
        customerUsers.put(email, user);
    }

    /**
     * Seeds activity types required by BOQ workflow and Change Order workflow.
     * Without these, {@code ActivityFeedService.logProjectActivity()} throws
     * RuntimeException("Activity Type not found: ...").
     */
    private void seedActivityTypes() {
        for (String name : REQUIRED_ACTIVITY_TYPES) {
            activityTypeRepository.findByName(name).orElseGet(() -> {
                ActivityType at = new ActivityType();
                at.setName(name);
                at.setDescription("Test activity type: " + name);
                return activityTypeRepository.save(at);
            });
        }
    }
}
