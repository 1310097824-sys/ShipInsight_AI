package com.gsmv.vessel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gsmv.ais.dto.AisRecordView;
import com.gsmv.ais.dto.AisVesselSummaryView;
import com.gsmv.common.ApiResponse;
import com.gsmv.common.PageResponse;
import com.gsmv.media.MediaFileService;
import java.util.List;
import org.junit.jupiter.api.Test;

class VesselControllerAisEndpointsTests {

    private final VesselService vesselService = mock(VesselService.class);
    private final VesselController controller = new VesselController(vesselService, mock(MediaFileService.class));

    @Test
    void returnsAisSummaryResponse() {
        AisVesselSummaryView summary = AisVesselSummaryView.empty();
        when(vesselService.getAisSummary(7L)).thenReturn(summary);

        ApiResponse<AisVesselSummaryView> response = controller.getAisSummary(7L);

        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.data()).isSameAs(summary);
    }

    @Test
    void returnsAisRecordsPageResponse() {
        PageResponse<AisRecordView> page = new PageResponse<>(List.of(), 0, 1, 5);
        when(vesselService.listAisRecords(7L, 1, 5)).thenReturn(page);

        ApiResponse<PageResponse<AisRecordView>> response = controller.listAisRecords(7L, 1, 5);

        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.data()).isSameAs(page);
    }
}
