package com.wd.api.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for portal staff to manage support tickets.
 * Uses JdbcTemplate for direct SQL access — support_tickets table is owned by
 * the Customer API but both APIs share the same PostgreSQL database.
 */
@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private static final Logger logger = LoggerFactory.getLogger(SupportTicketService.class);

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get all tickets with optional filters, paginated.
     * Ordered by priority (HIGH first) then updated_at descending.
     */
    public Map<String, Object> getAllTickets(int page, int size, String status, String category, Long assignedTo) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            where.append(" AND st.status = ?");
            params.add(status);
        }
        if (category != null && !category.isBlank()) {
            where.append(" AND st.category = ?");
            params.add(category);
        }
        if (assignedTo != null) {
            where.append(" AND st.assigned_to = ?");
            params.add(assignedTo);
        }

        String countSql = "SELECT COUNT(*) FROM support_tickets st" + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

        String dataSql = "SELECT st.id, st.ticket_number, st.subject, st.description, st.status, "
                + "st.category, st.priority, st.customer_id, st.assigned_to, "
                + "st.created_at, st.updated_at, st.resolved_at "
                + "FROM support_tickets st"
                + where
                + " ORDER BY CASE st.priority WHEN 'URGENT' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END, "
                + "st.updated_at DESC "
                + "LIMIT ? OFFSET ?";

        params.add(size);
        params.add((long) page * size);

        List<Map<String, Object>> tickets = jdbcTemplate.queryForList(dataSql, params.toArray());

        return Map.of(
                "content", tickets,
                "page", page,
                "size", size,
                "totalElements", total != null ? total : 0L,
                "totalPages", total != null ? (int) Math.ceil((double) total / size) : 0
        );
    }

    /**
     * Get a single ticket with its replies.
     */
    public Map<String, Object> getTicketDetail(Long ticketId) {
        String ticketSql = "SELECT st.id, st.ticket_number, st.subject, st.description, st.status, "
                + "st.category, st.priority, st.customer_id, st.assigned_to, "
                + "st.created_at, st.updated_at, st.resolved_at "
                + "FROM support_tickets st WHERE st.id = ?";

        Map<String, Object> ticket = jdbcTemplate.queryForMap(ticketSql, ticketId);

        String repliesSql = "SELECT r.id, r.ticket_id, r.message, r.sender_name, r.user_type, "
                + "r.attachment_url, r.created_at "
                + "FROM support_ticket_replies r "
                + "WHERE r.ticket_id = ? "
                + "ORDER BY r.created_at ASC";

        List<Map<String, Object>> replies = jdbcTemplate.queryForList(repliesSql, ticketId);

        ticket.put("replies", replies);
        return ticket;
    }

    /**
     * Assign a ticket to a portal staff member.
     */
    public void assignTicket(Long ticketId, Long assignedTo) {
        String sql = "UPDATE support_tickets SET assigned_to = ?, updated_at = NOW() WHERE id = ?";
        int rows = jdbcTemplate.update(sql, assignedTo, ticketId);
        if (rows == 0) {
            throw new RuntimeException("Support ticket not found: " + ticketId);
        }
        logger.info("Ticket {} assigned to staff user {}", ticketId, assignedTo);
    }

    /**
     * Update the status of a ticket. Sets resolved_at when status is RESOLVED.
     */
    public void updateStatus(Long ticketId, String status) {
        String sql;
        if ("RESOLVED".equalsIgnoreCase(status)) {
            sql = "UPDATE support_tickets SET status = ?, resolved_at = NOW(), updated_at = NOW() WHERE id = ?";
        } else {
            sql = "UPDATE support_tickets SET status = ?, updated_at = NOW() WHERE id = ?";
        }
        int rows = jdbcTemplate.update(sql, status, ticketId);
        if (rows == 0) {
            throw new RuntimeException("Support ticket not found: " + ticketId);
        }
        logger.info("Ticket {} status updated to {}", ticketId, status);
    }

    /**
     * Add a staff reply to a ticket.
     */
    public Map<String, Object> addStaffReply(Long ticketId, Long staffUserId, String staffName,
                                              String message, String attachmentUrl) {
        // Verify ticket exists
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM support_tickets WHERE id = ?", Integer.class, ticketId);
        if (count == null || count == 0) {
            throw new RuntimeException("Support ticket not found: " + ticketId);
        }

        String sql = "INSERT INTO support_ticket_replies "
                + "(ticket_id, sender_id, sender_name, user_type, message, attachment_url, created_at) "
                + "VALUES (?, ?, ?, 'STAFF', ?, ?, NOW()) RETURNING id, created_at";

        Map<String, Object> reply = jdbcTemplate.queryForMap(sql, ticketId, staffUserId, staffName,
                message, attachmentUrl);

        // Touch updated_at on the parent ticket
        jdbcTemplate.update("UPDATE support_tickets SET updated_at = NOW() WHERE id = ?", ticketId);

        logger.info("Staff reply added to ticket {} by user {}", ticketId, staffUserId);

        return Map.of(
                "id", reply.get("id"),
                "ticketId", ticketId,
                "staffUserId", staffUserId,
                "staffName", staffName,
                "message", message,
                "attachmentUrl", attachmentUrl,
                "userType", "STAFF",
                "createdAt", reply.get("created_at")
        );
    }
}
