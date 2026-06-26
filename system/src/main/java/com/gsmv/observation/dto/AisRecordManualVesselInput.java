package com.gsmv.observation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AisRecordManualVesselInput(
        @NotNull(message = "船舶不能为空") Long vesselId,
        @Min(value = 1, message = "估算数量必须大于 0") Integer countEstimated,
        String behavior,
        String comment
) {
}
