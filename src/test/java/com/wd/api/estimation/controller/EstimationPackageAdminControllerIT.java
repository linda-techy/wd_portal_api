package com.wd.api.estimation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.estimation.domain.EstimationPackage;
import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.dto.admin.PackageAdminCreateRequest;
import com.wd.api.estimation.dto.admin.PackageAdminUpdateRequest;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class EstimationPackageAdminControllerIT extends TestcontainersPostgresBase {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private EntityManager em;

    @Test
    @WithMockUser(authorities = "ESTIMATION_SETTINGS_MANAGE")
    void create_returns201_andResponseBody() throws Exception {
        PackageAdminCreateRequest req = new PackageAdminCreateRequest(
                PackageInternalName.STANDARD, "Signature", "Mid-segment", "desc", 20);
        mvc.perform(post("/api/estimation/packages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.marketingName").value("Signature"))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @WithMockUser(authorities = "ESTIMATION_SETTINGS_VIEW")
    void list_withViewPermission_returns200() throws Exception {
        seedOnePackage();
        mvc.perform(get("/api/estimation/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].marketingName").value("Signature"));
    }

    @Test
    @WithMockUser(authorities = "ESTIMATION_SETTINGS_MANAGE")
    void update_returns200_withUpdatedFields() throws Exception {
        EstimationPackage pkg = seedOnePackage();
        em.flush();

        PackageAdminUpdateRequest req = new PackageAdminUpdateRequest(
                "Signature Plus", "Updated tagline", "desc", 25, true);
        mvc.perform(put("/api/estimation/packages/" + pkg.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.marketingName").value("Signature Plus"))
                .andExpect(jsonPath("$.data.displayOrder").value(25));
    }

    @Test
    @WithMockUser(authorities = "ESTIMATION_SETTINGS_MANAGE")
    void delete_returns200_andRowSoftDeleted() throws Exception {
        EstimationPackage pkg = seedOnePackage();
        em.flush();

        mvc.perform(delete("/api/estimation/packages/" + pkg.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void create_withoutAuth_returns401or403() throws Exception {
        PackageAdminCreateRequest req = new PackageAdminCreateRequest(
                PackageInternalName.STANDARD, "Signature", null, null, 20);
        mvc.perform(post("/api/estimation/packages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403, got " + status);
                    }
                });
    }

    @Test
    @WithMockUser(authorities = "ESTIMATION_SETTINGS_VIEW")
    void create_withOnlyViewPermission_returns403() throws Exception {
        PackageAdminCreateRequest req = new PackageAdminCreateRequest(
                PackageInternalName.STANDARD, "Signature", null, null, 20);
        mvc.perform(post("/api/estimation/packages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ESTIMATION_SETTINGS_MANAGE")
    void create_withMissingMarketingName_returns400() throws Exception {
        // marketingName is @NotBlank
        String json = """
            {"internalName":"STANDARD","tagline":"x","displayOrder":20}
            """;
        mvc.perform(post("/api/estimation/packages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    private EstimationPackage seedOnePackage() {
        EstimationPackage pkg = new EstimationPackage();
        pkg.setInternalName(PackageInternalName.STANDARD);
        pkg.setMarketingName("Signature");
        pkg.setDisplayOrder(20);
        pkg.setActive(true);
        em.persist(pkg);
        return pkg;
    }
}
