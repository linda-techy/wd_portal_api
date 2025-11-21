package com.wd.api.dao.implementation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wd.api.dao.interfaces.ILeadsDAO;
import com.wd.api.dao.model.Leads;
import com.wd.api.dto.PaginatedResponse;
import com.wd.api.dto.PaginationParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.lang.Math;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class LeadsDAO implements ILeadsDAO {
    
    private static final Logger logger = LoggerFactory.getLogger(LeadsDAO.class);

    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public LeadsDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    // Custom row mapper to handle PostgreSQL arrays
    private RowMapper<Leads> leadsRowMapper = new RowMapper<Leads>() {
        @Override
        public Leads mapRow(ResultSet rs, int rowNum) throws SQLException {
            Leads lead = new Leads();
            
            // Map basic fields
            lead.setName(rs.getString("name"));
            lead.setEmail(rs.getString("email"));
            lead.setPhone(rs.getString("phone"));
            lead.setWhatsappNumber(rs.getString("whatsapp_number"));
            lead.setLeadSource(rs.getString("lead_source"));
            lead.setLeadStatus(rs.getString("lead_status"));
            lead.setPriority(rs.getString("priority"));
            lead.setCustomerType(rs.getString("customer_type"));
            lead.setProjectType(rs.getString("project_type"));
            lead.setProjectDescription(rs.getString("project_description"));
            lead.setRequirements(rs.getString("requirements"));
            lead.setBudget(rs.getBigDecimal("budget"));
            
            lead.setNextFollowUp(rs.getObject("next_follow_up", LocalDateTime.class));
            lead.setLastContactDate(rs.getObject("last_contact_date", LocalDateTime.class));
            
            lead.setAssignedTeam(rs.getString("assigned_team"));
            
            lead.setNotes(rs.getString("notes"));
            lead.setClientRating(rs.getObject("client_rating", Integer.class));
            lead.setProbabilityToWin(rs.getObject("probability_to_win", Integer.class));
            lead.setLostReason(rs.getString("lost_reason"));
            
            lead.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
            lead.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
            lead.setLeadId(String.valueOf(rs.getLong("lead_id")));
            
            // Map fields that exist in database
            lead.setDateOfEnquiry(rs.getObject("date_of_enquiry", LocalDate.class));
            lead.setState(rs.getString("state"));
            lead.setDistrict(rs.getString("district"));
            lead.setLocation(rs.getString("location"));
            lead.setAddress(rs.getString("address"));
            lead.setProjectSqftArea(rs.getBigDecimal("project_sqft_area"));
            
            return lead;
        }
    };

    // =====================================================
    // LEAD CRUD OPERATIONS
    // =====================================================

    @Override
    public List<Leads> getAllLeads() {
        try {
            // Query all leads and map to Leads class
            String sql = "SELECT * FROM leads ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, leadsRowMapper);
        } catch (Exception e) {
            // Log the error for debugging
            logger.error("Error in getAllLeads", e);
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public PaginatedResponse<Leads> getLeadsPaginated(PaginationParams params) {
        try {
            // Build WHERE clause for filtering
            StringBuilder whereClause = new StringBuilder();
            List<Object> queryParams = new java.util.ArrayList<>();
            
            if (params.getStatus() != null && !params.getStatus().isEmpty()) {
                whereClause.append(" AND lead_status = ?");
                queryParams.add(params.getStatus());
            }
            
            if (params.getSource() != null && !params.getSource().isEmpty()) {
                whereClause.append(" AND lead_source = ?");
                queryParams.add(params.getSource());
            }
            
            if (params.getPriority() != null && !params.getPriority().isEmpty()) {
                whereClause.append(" AND priority = ?");
                queryParams.add(params.getPriority());
            }
            
            if (params.getCustomerType() != null && !params.getCustomerType().isEmpty()) {
                whereClause.append(" AND customer_type = ?");
                queryParams.add(params.getCustomerType());
            }
            
            if (params.getProjectType() != null && !params.getProjectType().isEmpty()) {
                whereClause.append(" AND project_type = ?");
                queryParams.add(params.getProjectType());
            }
            
            if (params.getAssignedTeam() != null && !params.getAssignedTeam().isEmpty()) {
                whereClause.append(" AND assigned_team = ?");
                queryParams.add(params.getAssignedTeam());
            }
            
            if (params.getState() != null && !params.getState().isEmpty()) {
                whereClause.append(" AND state = ?");
                queryParams.add(params.getState());
            }
            
            if (params.getDistrict() != null && !params.getDistrict().isEmpty()) {
                whereClause.append(" AND district = ?");
                queryParams.add(params.getDistrict());
            }
            
            if (params.getMinBudget() != null) {
                whereClause.append(" AND budget >= ?");
                queryParams.add(params.getMinBudget());
            }
            
            if (params.getMaxBudget() != null) {
                whereClause.append(" AND budget <= ?");
                queryParams.add(params.getMaxBudget());
            }
            
            if (params.getStartDate() != null) {
                whereClause.append(" AND DATE(created_at) >= ?");
                queryParams.add(params.getStartDate());
            }
            
            if (params.getEndDate() != null) {
                whereClause.append(" AND DATE(created_at) <= ?");
                queryParams.add(params.getEndDate());
            }
            
            if (params.getSearch() != null && !params.getSearch().isEmpty()) {
                whereClause.append(" AND (name ILIKE ? OR email ILIKE ? OR phone ILIKE ? OR project_description ILIKE ?)");
                String searchPattern = "%" + params.getSearch() + "%";
                queryParams.add(searchPattern);
                queryParams.add(searchPattern);
                queryParams.add(searchPattern);
                queryParams.add(searchPattern);
            }
            
            // Get total count
            String countSql = "SELECT COUNT(*) FROM leads WHERE 1=1" + whereClause.toString();
            int totalItems = jdbcTemplate.queryForObject(countSql, Integer.class, queryParams.toArray());
            
            // Calculate pagination
            int totalPages = (int) Math.ceil((double) totalItems / params.getLimit());
            int offset = (params.getPage() - 1) * params.getLimit();
            
            // Build ORDER BY clause with SQL injection protection
            String orderBy = " ORDER BY " + sanitizeSortBy(params.getSortBy()) + " " + sanitizeSortOrder(params.getSortOrder());
            
            // Get paginated results
            String dataSql = "SELECT * FROM leads WHERE 1=1" + whereClause.toString() + orderBy + " LIMIT ? OFFSET ?";
            queryParams.add(params.getLimit());
            queryParams.add(offset);
            
            List<Leads> leads = jdbcTemplate.query(dataSql, leadsRowMapper, queryParams.toArray());
            
            // Create paginated response
            return new PaginatedResponse<>(
                leads,
                params.getPage(),
                totalPages,
                totalItems,
                params.getLimit(),
                params.getPage() < totalPages,
                params.getPage() > 1
            );
            
        } catch (Exception e) {
            logger.error("Error in getLeadsPaginated", e);
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public Leads getLeadById(String leadId) {
        String sql = "SELECT * FROM leads WHERE lead_id = ?";
        List<Leads> results = jdbcTemplate.query(sql, leadsRowMapper, Long.parseLong(leadId));
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public int createLead(Leads lead) {
        String sql = "INSERT INTO leads (" +
        "name, email, phone, whatsapp_number, customer_type, lead_source, " +
        "state, district, location, address, " +
        "project_type, date_of_enquiry, project_description, requirements, budget, project_sqft_area, " +
        "lead_status, priority, assigned_team, client_rating, next_follow_up, last_contact_date, probability_to_win, notes, lost_reason" +
        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        
        return jdbcTemplate.update(sql,
                lead.getName(), lead.getEmail(), lead.getPhone(), lead.getWhatsappNumber(), lead.getCustomerType(),
                lead.getLeadSource(), lead.getState(), lead.getDistrict(), lead.getLocation(), lead.getAddress(),
                lead.getProjectType(), lead.getDateOfEnquiry(), lead.getProjectDescription(), lead.getRequirements(), lead.getBudget(), lead.getProjectSqftArea(),
                lead.getLeadStatus(), lead.getPriority(), lead.getAssignedTeam(), lead.getClientRating(), lead.getNextFollowUp(), lead.getLastContactDate(),
                lead.getProbabilityToWin(), lead.getNotes(), lead.getLostReason()
                );
    }

    @Override
    public int updateLead(Leads lead) {
        String sql = "UPDATE leads SET " +
                "name = ?, email = ?, phone = ?, whatsapp_number = ?, customer_type = ?, lead_source = ?, " +
                "state = ?, district = ?, location = ?, address = ?, " +
                "project_type = ?, date_of_enquiry = ?, project_description = ?, requirements = ?, budget = ?, project_sqft_area = ?, " +
                "lead_status = ?, priority = ?, assigned_team = ?, client_rating = ?, next_follow_up = ?, last_contact_date = ?, " +
                "probability_to_win = ?, notes = ?, lost_reason = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE lead_id = ?";
        
        // Handle empty strings for unique constraint fields
        String whatsappNumber = lead.getWhatsappNumber();
        if (whatsappNumber != null && whatsappNumber.trim().isEmpty()) {
            whatsappNumber = null;
        }
        
        return jdbcTemplate.update(sql,
                lead.getName(), lead.getEmail(), lead.getPhone(), whatsappNumber, lead.getCustomerType(),
                lead.getLeadSource(), lead.getState(), lead.getDistrict(), lead.getLocation(),
                lead.getAddress(), lead.getProjectType(), lead.getDateOfEnquiry(), lead.getProjectDescription(),
                lead.getRequirements(), lead.getBudget(), lead.getProjectSqftArea(), lead.getLeadStatus(),
                lead.getPriority(), lead.getAssignedTeam(), lead.getClientRating(), lead.getNextFollowUp(),
                lead.getLastContactDate(), lead.getProbabilityToWin(), lead.getNotes(), lead.getLostReason(), 
                Long.parseLong(lead.getLeadId()));
    }

    @Override
    public int deleteLead(String leadId) {
        String sql = "DELETE FROM leads WHERE lead_id = ?";
        return jdbcTemplate.update(sql, Long.parseLong(leadId));
    }

    // =====================================================
    // LEAD FILTERING & SEARCH
    // =====================================================

    @Override
    public List<Leads> getLeadsByStatus(String status) {
        String sql = "SELECT * FROM leads WHERE lead_status = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, leadsRowMapper, status);
    }

    @Override
    public List<Leads> getLeadsByAssignedTo(UUID assignedTo) {
        String sql = "SELECT * FROM leads WHERE assigned_to = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, leadsRowMapper, assignedTo);
    }

    @Override
    public List<Leads> getLeadsBySource(String source) {
        String sql = "SELECT * FROM leads WHERE lead_source = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, leadsRowMapper, source);
    }

    @Override
    public List<Leads> getLeadsByPriority(String priority) {
        String sql = "SELECT * FROM leads WHERE priority = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, leadsRowMapper, priority);
    }

    @Override
    public List<Leads> getLeadsByDateRange(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT * FROM leads WHERE DATE(created_at) BETWEEN ? AND ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, leadsRowMapper, startDate, endDate);
    }

    @Override
    public List<Leads> searchLeads(String searchTerm) {
        String sql = "SELECT * FROM leads WHERE " +
                    "LOWER(name) LIKE LOWER(?) OR " +
                    "LOWER(email) LIKE LOWER(?) OR " +
                    "LOWER(whatsapp_number) LIKE LOWER(?) OR " +
                    "LOWER(phone) LIKE LOWER(?) OR " +
                    "LOWER(project_description) LIKE LOWER(?) " +
                    "ORDER BY created_at DESC";
        String searchPattern = "%" + searchTerm + "%";
        return jdbcTemplate.query(sql, leadsRowMapper, 
                searchPattern, searchPattern, searchPattern, searchPattern, searchPattern);
    }

    // =====================================================
    // LEAD MANAGEMENT OPERATIONS
    // =====================================================

    @Override
    public int assignLeadToTeamMember(String leadId, UUID teamMemberId) {
        String sql = "UPDATE leads SET assigned_to = ?, updated_at = CURRENT_TIMESTAMP WHERE lead_id = ?";
        return jdbcTemplate.update(sql, teamMemberId, Long.parseLong(leadId));
    }

    @Override
    public int updateLeadStatus(String leadId, String status) {
        String sql = "UPDATE leads SET lead_status = ?, updated_at = CURRENT_TIMESTAMP WHERE lead_id = ?";
        return jdbcTemplate.update(sql, status, Long.parseLong(leadId));
    }

    @Override
    public int updateLeadPriority(String leadId, String priority) {
        String sql = "UPDATE leads SET priority = ?, updated_at = CURRENT_TIMESTAMP WHERE lead_id = ?";
        return jdbcTemplate.update(sql, priority, Long.parseLong(leadId));
    }

    @Override
    public int markLeadAsConverted(String leadId) {
        String sql = "UPDATE leads SET lead_status = 'converted', updated_at = CURRENT_TIMESTAMP WHERE lead_id = ?";
        return jdbcTemplate.update(sql, Long.parseLong(leadId));
    }

    @Override
    public int markLeadAsLost(String leadId, String reason) {
        String sql = "UPDATE leads SET lead_status = 'lost', lost_reason = ?, updated_at = CURRENT_TIMESTAMP WHERE lead_id = ?";
        return jdbcTemplate.update(sql, reason, Long.parseLong(leadId));
    }

    @Override
    public List<Leads> getOverdueFollowUps() {
        String sql = "SELECT * FROM leads WHERE next_follow_up < CURRENT_TIMESTAMP AND lead_status NOT IN ('converted', 'lost') ORDER BY next_follow_up ASC";
        return jdbcTemplate.query(sql, leadsRowMapper);
    }

    // =====================================================
    // ANALYTICS & REPORTS
    // =====================================================

    @Override
    public Map<String, Object> getLeadAnalytics() {
        Map<String, Object> analytics = new java.util.HashMap<>();
        
        // Lead status distribution
        String statusSql = "SELECT lead_status, COUNT(*) as count FROM leads GROUP BY lead_status";
        List<Map<String, Object>> statusData = jdbcTemplate.queryForList(statusSql);
        analytics.put("statusDistribution", statusData);
        
        // Lead source distribution
        String sourceSql = "SELECT lead_source, COUNT(*) as count FROM leads GROUP BY lead_source";
        List<Map<String, Object>> sourceData = jdbcTemplate.queryForList(sourceSql);
        analytics.put("sourceDistribution", sourceData);
        
        // Priority distribution
        String prioritySql = "SELECT priority, COUNT(*) as count FROM leads GROUP BY priority";
        List<Map<String, Object>> priorityData = jdbcTemplate.queryForList(prioritySql);
        analytics.put("priorityDistribution", priorityData);
        
        // Monthly lead trends
        String trendSql = "SELECT DATE_TRUNC('month', created_at) as month, COUNT(*) as count " +
                         "FROM leads GROUP BY DATE_TRUNC('month', created_at) " +
                         "ORDER BY month DESC LIMIT 12";
        List<Map<String, Object>> trendData = jdbcTemplate.queryForList(trendSql);
        analytics.put("monthlyTrends", trendData);
        
        return analytics;
    }

    @Override
    public Map<String, Object> getLeadConversionMetrics() {
        Map<String, Object> metrics = new java.util.HashMap<>();
        
        // Total leads
        String totalLeadsSql = "SELECT COUNT(*) FROM leads";
        Integer totalLeads = jdbcTemplate.queryForObject(totalLeadsSql, Integer.class);
        metrics.put("totalLeads", totalLeads != null ? totalLeads : 0);
        
        // Converted leads (leads with status 'converted')
        String convertedSql = "SELECT COUNT(*) FROM leads WHERE lead_status = 'converted'";
        Integer convertedLeads = jdbcTemplate.queryForObject(convertedSql, Integer.class);
        metrics.put("convertedLeads", convertedLeads != null ? convertedLeads : 0);
        
        // Conversion rate
        if (totalLeads != null && totalLeads > 0) {
            double conversionRate = (convertedLeads != null ? convertedLeads : 0) * 100.0 / totalLeads;
            metrics.put("conversionRate", Math.round(conversionRate * 100.0) / 100.0);
        } else {
            metrics.put("conversionRate", 0.0);
        }
        
        return metrics;
    }

    @Override
    public org.springframework.jdbc.core.JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    // SQL injection protection methods
    private String sanitizeSortBy(String sortBy) {
        if (sortBy == null) return "created_at";
        
        // Whitelist of allowed sort columns
        String[] allowedColumns = {
            "created_at", "updated_at", "name", "email", "phone", 
            "lead_status", "priority", "budget", "lead_source", 
            "customer_type", "project_type", "assigned_team"
        };
        
        for (String column : allowedColumns) {
            if (column.equals(sortBy)) {
                return column;
            }
        }
        
        return "created_at"; // Default fallback
    }
    
    private String sanitizeSortOrder(String sortOrder) {
        if (sortOrder == null) return "DESC";
        
        if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
            return sortOrder.toUpperCase();
        }
        
        return "DESC"; // Default fallback
    }
} 