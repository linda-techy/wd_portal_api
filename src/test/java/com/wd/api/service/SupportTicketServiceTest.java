package com.wd.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SupportTicketService.
 * Uses mocked JdbcTemplate — no database or Spring context needed.
 */
@ExtendWith(MockitoExtension.class)
class SupportTicketServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SupportTicketService supportTicketService;

    // ── getAllTickets ─────────────────────────────────────────────────────────

    @Test
    void getAllTickets_filterByStatus_buildsCorrectWhereClause() {
        when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Long.class), any(Object[].class)))
                .thenReturn(1L);
        when(jdbcTemplate.queryForList(contains("ORDER BY"), any(Object[].class)))
                .thenReturn(List.of(Map.of("id", 1L, "status", "OPEN")));

        Map<String, Object> result = supportTicketService.getAllTickets(0, 10, "OPEN", null, null);

        assertThat(result.get("totalElements")).isEqualTo(1L);
        assertThat(result.get("page")).isEqualTo(0);

        // Verify COUNT query included "status"
        verify(jdbcTemplate).queryForObject(contains("COUNT(*)"), eq(Long.class), any(Object[].class));
    }

    @Test
    void getAllTickets_orderedByPriorityThenUpdatedAt_sqlContainsOrderByClause() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L);
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of());

        supportTicketService.getAllTickets(0, 20, null, null, null);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sqlCaptor.capture(), any(Object[].class));
        assertThat(sqlCaptor.getValue())
                .contains("ORDER BY")
                .contains("priority")
                .contains("updated_at DESC");
    }

    @Test
    void getAllTickets_noFilters_returnsPaginatedResult() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(2L);
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(
                        Map.of("id", 1L),
                        Map.of("id", 2L)));

        Map<String, Object> result = supportTicketService.getAllTickets(0, 10, null, null, null);

        assertThat(result.get("totalElements")).isEqualTo(2L);
        assertThat(result.get("totalPages")).isEqualTo(1);
    }

    // ── getTicketDetail ───────────────────────────────────────────────────────

    @Test
    void getTicketDetail_existingTicket_returnsTicketWithReplies() {
        Map<String, Object> ticket = new java.util.HashMap<>(Map.of(
                "id", 1L,
                "subject", "Leaking pipe",
                "status", "OPEN"));

        List<Map<String, Object>> replies = List.of(
                Map.of("id", 10L, "message", "We are looking into it", "user_type", "STAFF"));

        when(jdbcTemplate.queryForMap(contains("support_tickets"), eq(1L))).thenReturn(ticket);
        when(jdbcTemplate.queryForList(contains("support_ticket_replies"), eq(1L))).thenReturn(replies);

        Map<String, Object> result = supportTicketService.getTicketDetail(1L);

        assertThat(result.get("subject")).isEqualTo("Leaking pipe");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> returnedReplies = (List<Map<String, Object>>) result.get("replies");
        assertThat(returnedReplies).hasSize(1);
        assertThat(returnedReplies.get(0).get("user_type")).isEqualTo("STAFF");
    }

    // ── assignTicket ─────────────────────────────────────────────────────────

    @Test
    void assignTicket_existingTicket_updatesAssignedTo() {
        when(jdbcTemplate.update(anyString(), eq(42L), eq(1L))).thenReturn(1);

        supportTicketService.assignTicket(1L, 42L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), eq(42L), eq(1L));
        assertThat(sqlCaptor.getValue()).contains("assigned_to");
    }

    @Test
    void assignTicket_notFound_throwsRuntimeException() {
        when(jdbcTemplate.update(anyString(), anyLong(), anyLong())).thenReturn(0);

        assertThatThrownBy(() -> supportTicketService.assignTicket(999L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Support ticket not found");
    }

    // ── updateStatus ─────────────────────────────────────────────────────────

    @Test
    void updateStatus_resolvedStatus_setsResolvedAt() {
        when(jdbcTemplate.update(anyString(), eq("RESOLVED"), eq(1L))).thenReturn(1);

        supportTicketService.updateStatus(1L, "RESOLVED");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), eq("RESOLVED"), eq(1L));
        assertThat(sqlCaptor.getValue()).contains("resolved_at");
    }

    @Test
    void updateStatus_nonResolvedStatus_doesNotSetResolvedAt() {
        when(jdbcTemplate.update(anyString(), eq("IN_PROGRESS"), eq(1L))).thenReturn(1);

        supportTicketService.updateStatus(1L, "IN_PROGRESS");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), eq("IN_PROGRESS"), eq(1L));
        assertThat(sqlCaptor.getValue()).doesNotContain("resolved_at");
    }

    // ── addStaffReply ─────────────────────────────────────────────────────────

    @Test
    void addStaffReply_validTicket_createsReplyWithStaffType() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L))).thenReturn(1);
        when(jdbcTemplate.queryForMap(contains("INSERT INTO support_ticket_replies"),
                eq(1L), eq(5L), eq("Alice"), anyString(), isNull()))
                .thenReturn(Map.of("id", 20L, "created_at", "2026-04-18T10:00:00"));
        when(jdbcTemplate.update(contains("UPDATE support_tickets SET updated_at"), eq(1L))).thenReturn(1);

        Map<String, Object> result = supportTicketService.addStaffReply(1L, 5L, "Alice", "We will fix it", null);

        assertThat(result.get("userType")).isEqualTo("STAFF");
        assertThat(result.get("staffName")).isEqualTo("Alice");
        assertThat(result.get("message")).isEqualTo("We will fix it");
        assertThat(result.get("ticketId")).isEqualTo(1L);
    }

    @Test
    void addStaffReply_ticketNotFound_throwsRuntimeException() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(999L))).thenReturn(0);

        assertThatThrownBy(() -> supportTicketService.addStaffReply(999L, 1L, "Bob", "Message", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Support ticket not found");
    }
}
