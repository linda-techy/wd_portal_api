package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.EnumValueDTO;
import com.wd.api.dto.StateDTO;
import com.wd.api.model.enums.ProjectPhase;
import com.wd.api.model.enums.ContractType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Common Controller for Master Data and Dropdown APIs
 * Provides reference data for dropdowns, enums, and common lookups
 */
@RestController
@RequestMapping("/api/common")
// CORS configuration is handled globally in SecurityConfig
public class CommonController {
    
    private static final Logger logger = LoggerFactory.getLogger(CommonController.class);
    
    // Indian States and Districts Master Data
    private static final Map<String, List<String>> STATES_DISTRICTS = new LinkedHashMap<>();
    
    static {
        // Kerala Districts
        STATES_DISTRICTS.put("Kerala", Arrays.asList(
            "Thiruvananthapuram", "Kollam", "Pathanamthitta", "Alappuzha", "Kottayam",
            "Idukki", "Ernakulam", "Thrissur", "Palakkad", "Malappuram",
            "Kozhikode", "Wayanad", "Kannur", "Kasaragod"
        ));
        
        // Karnataka Districts
        STATES_DISTRICTS.put("Karnataka", Arrays.asList(
            "Bengaluru Urban", "Bengaluru Rural", "Mysuru", "Mangaluru", "Belagavi",
            "Hubli-Dharwad", "Tumakuru", "Ballari", "Vijayapura", "Kalaburagi"
        ));
        
        // Tamil Nadu Districts
        STATES_DISTRICTS.put("Tamil Nadu", Arrays.asList(
            "Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem",
            "Tirunelveli", "Tiruppur", "Erode", "Vellore", "Thoothukudi"
        ));
        
        // Maharashtra Districts
        STATES_DISTRICTS.put("Maharashtra", Arrays.asList(
            "Mumbai", "Pune", "Nagpur", "Thane", "Nashik",
            "Aurangabad", "Solapur", "Kolhapur", "Ratnagiri", "Satara"
        ));
        
        // Goa Districts
        STATES_DISTRICTS.put("Goa", Arrays.asList(
            "North Goa", "South Goa"
        ));
        
        // Andhra Pradesh Districts
        STATES_DISTRICTS.put("Andhra Pradesh", Arrays.asList(
            "Visakhapatnam", "Vijayawada", "Guntur", "Nellore", "Kurnool",
            "Tirupati", "Rajahmundry", "Kadapa", "Anantapur", "Kakinada"
        ));
        
        // Telangana Districts
        STATES_DISTRICTS.put("Telangana", Arrays.asList(
            "Hyderabad", "Warangal", "Nizamabad", "Khammam", "Karimnagar",
            "Ramagundam", "Mahbubnagar", "Nalgonda", "Adilabad", "Suryapet"
        ));
    }
    
    /**
     * Get all Project Phase enum values
     * GET /api/common/project-phases
     */
    @GetMapping("/project-phases")
    public ResponseEntity<ApiResponse<List<EnumValueDTO>>> getProjectPhases() {
        try {
            List<EnumValueDTO> phases = Arrays.stream(ProjectPhase.values())
                .map(phase -> new EnumValueDTO(
                    phase.name(),
                    phase.getDisplayName(),
                    phase.getDescription(),
                    phase.getOrder()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("Project phases retrieved successfully", phases));
        } catch (Exception e) {
            logger.error("Error fetching project phases", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to fetch project phases"));
        }
    }
    
    /**
     * Get all Contract Type enum values
     * GET /api/common/contract-types
     */
    @GetMapping("/contract-types")
    public ResponseEntity<ApiResponse<List<EnumValueDTO>>> getContractTypes() {
        try {
            List<EnumValueDTO> contractTypes = Arrays.stream(ContractType.values())
                .map(type -> new EnumValueDTO(
                    type.name(),
                    type.getDisplayName(),
                    null
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("Contract types retrieved successfully", contractTypes));
        } catch (Exception e) {
            logger.error("Error fetching contract types", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to fetch contract types"));
        }
    }
    
    /**
     * Get list of Indian states
     * GET /api/common/states
     */
    @GetMapping("/states")
    public ResponseEntity<ApiResponse<List<StateDTO>>> getStates() {
        try {
            List<StateDTO> states = STATES_DISTRICTS.entrySet().stream()
                .map(entry -> new StateDTO(entry.getKey(), entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("States retrieved successfully", states));
        } catch (Exception e) {
            logger.error("Error fetching states", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to fetch states"));
        }
    }
    
    /**
     * Get list of state names only
     * GET /api/common/state-names
     */
    @GetMapping("/state-names")
    public ResponseEntity<ApiResponse<List<String>>> getStateNames() {
        try {
            List<String> stateNames = new ArrayList<>(STATES_DISTRICTS.keySet());
            return ResponseEntity.ok(ApiResponse.success("State names retrieved successfully", stateNames));
        } catch (Exception e) {
            logger.error("Error fetching state names", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to fetch state names"));
        }
    }
    
    /**
     * Get districts for a specific state
     * GET /api/common/districts?state=Kerala
     */
    @GetMapping("/districts")
    public ResponseEntity<ApiResponse<List<String>>> getDistricts(@RequestParam(required = false) String state) {
        try {
            if (state == null || state.trim().isEmpty()) {
                // Return all districts from all states
                List<String> allDistricts = STATES_DISTRICTS.values().stream()
                    .flatMap(List::stream)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
                return ResponseEntity.ok(ApiResponse.success("All districts retrieved successfully", allDistricts));
            }
            
            List<String> districts = STATES_DISTRICTS.get(state);
            if (districts == null) {
                return ResponseEntity.ok(ApiResponse.success("No districts found for state: " + state, new ArrayList<>()));
            }
            
            return ResponseEntity.ok(ApiResponse.success("Districts retrieved successfully", districts));
        } catch (Exception e) {
            logger.error("Error fetching districts for state: {}", state, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to fetch districts"));
        }
    }
    
    /**
     * Get project types (construction project categories)
     * GET /api/common/project-types
     */
    @GetMapping("/project-types")
    public ResponseEntity<ApiResponse<List<String>>> getProjectTypes() {
        try {
            List<String> projectTypes = Arrays.asList(
                "Residential Villa",
                "Residential Apartment",
                "Commercial Complex",
                "Office Building",
                "Retail Space",
                "Industrial Building",
                "Warehouse",
                "Mixed-Use Development",
                "Renovation",
                "Interior Design",
                "Landscape Development",
                "Infrastructure",
                "Other"
            );
            
            return ResponseEntity.ok(ApiResponse.success("Project types retrieved successfully", projectTypes));
        } catch (Exception e) {
            logger.error("Error fetching project types", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to fetch project types"));
        }
    }
    
    /**
     * Get design package options
     * GET /api/common/design-packages
     */
    @GetMapping("/design-packages")
    public ResponseEntity<ApiResponse<List<String>>> getDesignPackages() {
        try {
            List<String> packages = Arrays.asList(
                "Basic",
                "Standard",
                "Premium",
                "Luxury",
                "Custom"
            );
            
            return ResponseEntity.ok(ApiResponse.success("Design packages retrieved successfully", packages));
        } catch (Exception e) {
            logger.error("Error fetching design packages", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to fetch design packages"));
        }
    }
    
    /**
     * Get facing/direction options for properties
     * GET /api/common/facing-options
     */
    @GetMapping("/facing-options")
    public ResponseEntity<ApiResponse<List<String>>> getFacingOptions() {
        try {
            List<String> facingOptions = Arrays.asList(
                "North",
                "South",
                "East",
                "West",
                "North-East",
                "North-West",
                "South-East",
                "South-West"
            );
            
            return ResponseEntity.ok(ApiResponse.success("Facing options retrieved successfully", facingOptions));
        } catch (Exception e) {
            logger.error("Error fetching facing options", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to fetch facing options"));
        }
    }
}
