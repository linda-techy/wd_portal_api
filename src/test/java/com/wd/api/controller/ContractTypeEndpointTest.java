package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.EnumValueDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for CommonController.getContractTypes().
 *
 * <p>CommonController has no service dependencies, so we instantiate it directly
 * rather than spinning up a full Spring context. This tests the business rule
 * that only TURNKEY and ITEM_RATE are returned for new-project dropdowns.
 */
class ContractTypeEndpointTest {

    private final CommonController controller = new CommonController();

    @Test
    void getContractTypes_returnsExactlyTurnkeyAndItemRate() {
        ResponseEntity<ApiResponse<List<EnumValueDTO>>> response = controller.getContractTypes();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        List<EnumValueDTO> body = response.getBody().getData();

        assertThat(body).hasSize(2);
        assertThat(body.stream().map(EnumValueDTO::getValue))
                .containsExactlyInAnyOrder("TURNKEY", "ITEM_RATE");
    }

    @Test
    void getContractTypes_doesNotContainLaborOnlyOrCostPlus() {
        ResponseEntity<ApiResponse<List<EnumValueDTO>>> response = controller.getContractTypes();

        List<String> values = response.getBody().getData().stream()
                .map(EnumValueDTO::getValue)
                .toList();

        assertThat(values).isNotEmpty().doesNotContain("LABOR_ONLY", "COST_PLUS");
    }
}
