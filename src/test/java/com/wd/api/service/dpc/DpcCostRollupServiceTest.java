package com.wd.api.service.dpc;

import com.wd.api.dto.dpc.DpcCostRollupDto;
import com.wd.api.dto.dpc.DpcMasterCostSummaryDto;
import com.wd.api.model.BoqCategory;
import com.wd.api.model.BoqDocument;
import com.wd.api.model.BoqItem;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.DpcScopeTemplate;
import com.wd.api.model.enums.BoqDocumentStatus;
import com.wd.api.model.enums.ItemKind;
import com.wd.api.repository.BoqDocumentRepository;
import com.wd.api.repository.BoqItemRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.DpcScopeTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Pure-unit tests for {@link DpcCostRollupService}.  All repos mocked; no
 * Spring context.  Focused on the math + scope-routing rules — exhaustive
 * edge cases live in IT-level tests.
 */
@ExtendWith(MockitoExtension.class)
class DpcCostRollupServiceTest {

    @Mock private BoqItemRepository boqItemRepository;
    @Mock private BoqDocumentRepository boqDocumentRepository;
    @Mock private DpcScopeTemplateRepository dpcScopeTemplateRepository;
    @Mock private CustomerProjectRepository customerProjectRepository;

    @InjectMocks
    private DpcCostRollupService service;

    private CustomerProject project;
    private BoqDocument approvedBoq;

    @BeforeEach
    void setUp() {
        project = new CustomerProject();
        project.setId(1L);
        project.setName("Test Villa");
        project.setSqfeet(new BigDecimal("2000.00"));

        approvedBoq = new BoqDocument();
        approvedBoq.setId(50L);
        approvedBoq.setProject(project);
        approvedBoq.setStatus(BoqDocumentStatus.APPROVED);
    }

    // ── Helper builders ───────────────────────────────────────────────────────

    private DpcScopeTemplate template(long id, String code, String title, int order, List<String> patterns) {
        DpcScopeTemplate t = new DpcScopeTemplate();
        t.setId(id);
        t.setCode(code);
        t.setTitle(title);
        t.setDisplayOrder(order);
        t.setBoqCategoryPatterns(patterns);
        t.setIsActive(true);
        return t;
    }

    private BoqCategory category(String name) {
        BoqCategory c = new BoqCategory();
        c.setName(name);
        return c;
    }

    private BoqItem item(long id, BoqCategory cat, ItemKind kind, BigDecimal total) {
        BoqItem i = new BoqItem();
        i.setId(id);
        i.setProject(project);
        i.setBoqDocument(approvedBoq);
        i.setCategory(cat);
        i.setItemKind(kind);
        i.setTotalAmount(total);
        i.setIsActive(true);
        return i;
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void computeForProject_sumsBaseAndAddonAcrossScopes() {
        DpcScopeTemplate foundation = template(101L, "FOUNDATION", "Foundation", 1, List.of("Foundation"));
        DpcScopeTemplate plumbing   = template(102L, "PLUMBING",   "Plumbing",   2, List.of("Plumbing"));
        DpcScopeTemplate elevation  = template(199L, "ELEVATION",  "Elevation",  9, List.of("Elevation"));

        BoqItem foundationBase = item(1L, category("Foundation Works"), ItemKind.BASE,  new BigDecimal("100000"));
        BoqItem plumbingBase   = item(2L, category("Plumbing"),         ItemKind.BASE,  new BigDecimal("50000"));
        BoqItem plumbingAddon  = item(3L, category("Plumbing"),         ItemKind.ADDON, new BigDecimal("20000"));

        when(boqDocumentRepository.findById(50L)).thenReturn(Optional.of(approvedBoq));
        when(customerProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(boqItemRepository.findByProjectIdWithAssociations(1L))
                .thenReturn(List.of(foundationBase, plumbingBase, plumbingAddon));
        when(dpcScopeTemplateRepository.findByIsActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(foundation, plumbing, elevation));

        DpcMasterCostSummaryDto summary = service.computeForProject(1L, 50L);

        assertThat(summary.totalOriginal()).isEqualByComparingTo("150000");
        assertThat(summary.totalCustomized()).isEqualByComparingTo("170000");
        assertThat(summary.totalVariance()).isEqualByComparingTo("20000");
        assertThat(summary.scopes()).hasSize(3);

        DpcCostRollupDto plumbingRollup = summary.scopes().stream()
                .filter(s -> "PLUMBING".equals(s.scopeCode())).findFirst().orElseThrow();
        assertThat(plumbingRollup.originalAmount()).isEqualByComparingTo("50000");
        assertThat(plumbingRollup.customizedAmount()).isEqualByComparingTo("70000");
        assertThat(plumbingRollup.variance()).isEqualByComparingTo("20000");
        assertThat(plumbingRollup.hasCustomization()).isTrue();
    }

    @Test
    void computeForProject_perSqftDividesTotalsBySqfeet() {
        DpcScopeTemplate foundation = template(101L, "FOUNDATION", "Foundation", 1, List.of("Foundation"));
        DpcScopeTemplate elevation  = template(199L, "ELEVATION",  "Elevation",  9, List.of("Elevation"));

        BoqItem base = item(1L, category("Foundation"), ItemKind.BASE, new BigDecimal("400000"));

        when(boqDocumentRepository.findById(50L)).thenReturn(Optional.of(approvedBoq));
        when(customerProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(boqItemRepository.findByProjectIdWithAssociations(1L)).thenReturn(List.of(base));
        when(dpcScopeTemplateRepository.findByIsActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(foundation, elevation));

        DpcMasterCostSummaryDto summary = service.computeForProject(1L, 50L);

        // 400000 / 2000 sqft = 200 per sqft, rounded HALF_UP at scale 0.
        assertThat(summary.originalPerSqft()).isEqualByComparingTo("200");
        assertThat(summary.customizedPerSqft()).isEqualByComparingTo("200");
        assertThat(summary.sqfeet()).isEqualByComparingTo("2000.00");
    }

    @Test
    void computeForProject_throwsWhenBoqIsNotApproved() {
        BoqDocument draftBoq = new BoqDocument();
        draftBoq.setId(50L);
        draftBoq.setStatus(BoqDocumentStatus.DRAFT);
        when(boqDocumentRepository.findById(50L)).thenReturn(Optional.of(draftBoq));

        assertThatThrownBy(() -> service.computeForProject(1L, 50L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    void findMatchingScope_routesUnknownCategoryToElevationFallback() {
        DpcScopeTemplate foundation = template(101L, "FOUNDATION", "Foundation", 1, List.of("Foundation"));
        DpcScopeTemplate elevation  = template(199L, "ELEVATION",  "Elevation",  9, List.of("Elevation"));
        List<DpcScopeTemplate> templates = List.of(foundation, elevation);

        // "Solar PV" matches no pattern in either template — direct match returns empty.
        Optional<DpcScopeTemplate> direct = service.findMatchingScope(category("Solar PV Panels"), templates);
        assertThat(direct).isEmpty();

        // End-to-end: rollup routes that item into ELEVATION.
        BoqItem orphan = item(1L, category("Solar PV Panels"), ItemKind.BASE, new BigDecimal("10000"));
        when(boqDocumentRepository.findById(50L)).thenReturn(Optional.of(approvedBoq));
        when(customerProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(boqItemRepository.findByProjectIdWithAssociations(1L)).thenReturn(List.of(orphan));
        when(dpcScopeTemplateRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(templates);

        DpcMasterCostSummaryDto summary = service.computeForProject(1L, 50L);

        DpcCostRollupDto elevationRollup = summary.scopes().stream()
                .filter(s -> "ELEVATION".equals(s.scopeCode())).findFirst().orElseThrow();
        assertThat(elevationRollup.originalAmount()).isEqualByComparingTo("10000");

        DpcCostRollupDto foundationRollup = summary.scopes().stream()
                .filter(s -> "FOUNDATION".equals(s.scopeCode())).findFirst().orElseThrow();
        assertThat(foundationRollup.originalAmount()).isEqualByComparingTo("0");
    }
}
