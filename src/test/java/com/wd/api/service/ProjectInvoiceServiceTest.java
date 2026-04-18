package com.wd.api.service;

import com.wd.api.dto.ProjectInvoiceDTO;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.ProjectInvoice;
import com.wd.api.model.ProjectMilestone;
import com.wd.api.model.enums.InvoiceStatus;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.ProjectInvoiceRepository;
import com.wd.api.repository.ProjectMilestoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProjectInvoiceService.
 */
@ExtendWith(MockitoExtension.class)
class ProjectInvoiceServiceTest {

    @Mock private ProjectInvoiceRepository projectInvoiceRepository;
    @Mock private CustomerProjectRepository projectRepository;
    @Mock private ProjectMilestoneRepository milestoneRepository;

    @InjectMocks
    private ProjectInvoiceService invoiceService;

    private CustomerProject project;

    @BeforeEach
    void setUp() {
        project = new CustomerProject();
        project.setId(1L);
        project.setName("Metro Bridge");
        project.setCode("MB01");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ProjectInvoice savedInvoice(CustomerProject proj, String number, BigDecimal sub,
                                        BigDecimal gstPct, BigDecimal gstAmt, BigDecimal total) {
        return ProjectInvoice.builder()
                .id(100L)
                .project(proj)
                .invoiceNumber(number)
                .invoiceDate(LocalDate.now())
                .subTotal(sub)
                .gstPercentage(gstPct)
                .gstAmount(gstAmt)
                .totalAmount(total)
                .status(InvoiceStatus.ISSUED)
                .build();
    }

    // ── createProjectInvoice ──────────────────────────────────────────────────

    @Test
    void createProjectInvoice_validDto_setsStatusToIssued() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectInvoiceRepository.getNextInvoiceNumber()).thenReturn(7L);

        BigDecimal subTotal = new BigDecimal("10000.00");
        BigDecimal gstPct   = new BigDecimal("18.00");
        BigDecimal gstAmt   = new BigDecimal("1800.00");
        BigDecimal total    = new BigDecimal("11800.00");

        when(projectInvoiceRepository.save(any())).thenReturn(savedInvoice(project, "INV-MB01-7", subTotal, gstPct, gstAmt, total));

        ProjectInvoiceDTO dto = ProjectInvoiceDTO.builder()
                .projectId(1L)
                .subTotal(subTotal)
                .gstPercentage(gstPct)
                .invoiceDate(LocalDate.now())
                .build();

        ProjectInvoiceDTO result = invoiceService.createProjectInvoice(dto);

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.ISSUED.name());
        verify(projectInvoiceRepository).save(any(ProjectInvoice.class));
    }

    @Test
    void createProjectInvoice_generatesInvoiceNumberInCorrectFormat() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectInvoiceRepository.getNextInvoiceNumber()).thenReturn(42L);

        BigDecimal sub   = new BigDecimal("5000.00");
        BigDecimal gstPct = new BigDecimal("18.00");
        BigDecimal gstAmt = new BigDecimal("900.00");
        BigDecimal total  = new BigDecimal("5900.00");

        ArgumentCaptor<ProjectInvoice> captor = ArgumentCaptor.forClass(ProjectInvoice.class);
        when(projectInvoiceRepository.save(captor.capture())).thenReturn(
                savedInvoice(project, "INV-MB01-42", sub, gstPct, gstAmt, total));

        ProjectInvoiceDTO dto = ProjectInvoiceDTO.builder()
                .projectId(1L)
                .subTotal(sub)
                .build();

        invoiceService.createProjectInvoice(dto);

        assertThat(captor.getValue().getInvoiceNumber()).isEqualTo("INV-MB01-42");
    }

    @Test
    void createProjectInvoice_calculatesGstCorrectly_defaultRate18Percent() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectInvoiceRepository.getNextInvoiceNumber()).thenReturn(1L);

        BigDecimal sub   = new BigDecimal("10000.00");
        // gstAmount = 10000 * 18 / 100 = 1800, total = 11800
        BigDecimal gstAmt = new BigDecimal("1800.00");
        BigDecimal total  = new BigDecimal("11800.00");

        ArgumentCaptor<ProjectInvoice> captor = ArgumentCaptor.forClass(ProjectInvoice.class);
        when(projectInvoiceRepository.save(captor.capture())).thenReturn(
                savedInvoice(project, "INV-MB01-1", sub, new BigDecimal("18.00"), gstAmt, total));

        ProjectInvoiceDTO dto = ProjectInvoiceDTO.builder()
                .projectId(1L)
                .subTotal(sub)
                .gstPercentage(null) // trigger default 18%
                .build();

        invoiceService.createProjectInvoice(dto);

        ProjectInvoice persisted = captor.getValue();
        assertThat(persisted.getGstPercentage()).isEqualByComparingTo(new BigDecimal("18.00"));
        assertThat(persisted.getGstAmount()).isEqualByComparingTo(new BigDecimal("1800.00"));
        assertThat(persisted.getTotalAmount()).isEqualByComparingTo(new BigDecimal("11800.00"));
    }

    @Test
    void createProjectInvoice_nullProjectId_throwsNullPointerException() {
        ProjectInvoiceDTO dto = ProjectInvoiceDTO.builder()
                .projectId(null)
                .subTotal(new BigDecimal("1000.00"))
                .build();

        assertThatThrownBy(() -> invoiceService.createProjectInvoice(dto))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void createProjectInvoice_projectNotFound_throwsRuntimeException() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        ProjectInvoiceDTO dto = ProjectInvoiceDTO.builder()
                .projectId(999L)
                .subTotal(new BigDecimal("5000.00"))
                .build();

        assertThatThrownBy(() -> invoiceService.createProjectInvoice(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Project not found");
    }

    // ── generateInvoiceForMilestone ───────────────────────────────────────────

    @Test
    void generateInvoiceForMilestone_completedMilestone_createsInvoiceAndUpdatesStatus() {
        ProjectMilestone milestone = ProjectMilestone.builder()
                .id(5L)
                .project(project)
                .name("Foundation")
                .amount(new BigDecimal("50000.00"))
                .status("COMPLETED")
                .build();

        when(milestoneRepository.findById(5L)).thenReturn(Optional.of(milestone));
        when(projectInvoiceRepository.getNextInvoiceNumber()).thenReturn(3L);

        ProjectInvoice inv = savedInvoice(project, "INV-MB01-3",
                new BigDecimal("50000.00"), new BigDecimal("18.00"),
                new BigDecimal("9000.00"), new BigDecimal("59000.00"));
        when(projectInvoiceRepository.save(any())).thenReturn(inv);
        when(milestoneRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProjectInvoiceDTO result = invoiceService.generateInvoiceForMilestone(5L);

        assertThat(result).isNotNull();
        assertThat(milestone.getStatus()).isEqualTo("INVOICED");
        assertThat(milestone.getInvoice()).isNotNull();
    }

    @Test
    void generateInvoiceForMilestone_duplicateMilestone_throwsIllegalStateException() {
        ProjectInvoice existing = ProjectInvoice.builder().id(99L).build();
        ProjectMilestone milestone = ProjectMilestone.builder()
                .id(5L)
                .project(project)
                .name("Foundation")
                .amount(new BigDecimal("50000.00"))
                .status("COMPLETED")
                .invoice(existing)
                .build();

        when(milestoneRepository.findById(5L)).thenReturn(Optional.of(milestone));

        assertThatThrownBy(() -> invoiceService.generateInvoiceForMilestone(5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invoice already exists");
    }

    @Test
    void generateInvoiceForMilestone_nonCompletedMilestone_throwsIllegalStateException() {
        ProjectMilestone milestone = ProjectMilestone.builder()
                .id(5L)
                .project(project)
                .name("Foundation")
                .amount(new BigDecimal("50000.00"))
                .status("IN_PROGRESS")
                .build();

        when(milestoneRepository.findById(5L)).thenReturn(Optional.of(milestone));

        assertThatThrownBy(() -> invoiceService.generateInvoiceForMilestone(5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETED milestone");
    }

    // ── getInvoicesByProject ──────────────────────────────────────────────────

    @Test
    void getInvoicesByProject_existingProject_returnsMappedList() {
        ProjectInvoice inv = savedInvoice(project, "INV-MB01-1",
                new BigDecimal("10000.00"), new BigDecimal("18.00"),
                new BigDecimal("1800.00"), new BigDecimal("11800.00"));
        when(projectInvoiceRepository.findByProjectId(1L)).thenReturn(List.of(inv));

        List<ProjectInvoiceDTO> result = invoiceService.getInvoicesByProject(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInvoiceNumber()).isEqualTo("INV-MB01-1");
    }

    @Test
    void getInvoicesByProject_noInvoices_returnsEmptyList() {
        when(projectInvoiceRepository.findByProjectId(2L)).thenReturn(List.of());

        List<ProjectInvoiceDTO> result = invoiceService.getInvoicesByProject(2L);

        assertThat(result).isEmpty();
    }
}
