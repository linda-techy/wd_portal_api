package com.wd.api.dao.implementation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.wd.api.dao.interfaces.IToolsDAO;
import com.wd.api.model.SqftCategories;
import com.wd.api.model.Lead;

@Repository
public class ToolsDAO implements IToolsDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ToolsDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SqftCategories> getAllSqftCategories() {
        String sql = "SELECT * FROM sqft_categories";
        return jdbcTemplate.query(sql, BeanPropertyRowMapper.newInstance(SqftCategories.class));
    }

    @Override
    public int saveLeadEstimate(Lead lead) {
        String sql = "INSERT INTO leads (" +
                "name, email, whatsapp_number, phone, " +
                "customer_type, project_type, project_description, requirements, " +
                "budget, lead_status, lead_source, priority, assigned_team, notes, " +
                "client_rating, probability_to_win, next_follow_up, last_contact_date, " +
                "created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

        return jdbcTemplate.update(sql,
                lead.getName(),
                lead.getEmail(),
                lead.getWhatsappNumber(),
                lead.getPhone(),
                lead.getCustomerType(),
                lead.getProjectType(),
                lead.getProjectDescription(),
                lead.getRequirements(),
                lead.getBudget(),
                lead.getLeadStatus(),
                lead.getLeadSource(),
                lead.getPriority(),
                lead.getAssignedTeam(),
                lead.getNotes(),
                lead.getClientRating(),
                lead.getProbabilityToWin(),
                lead.getNextFollowUp(),
                lead.getLastContactDate());
    }

}
