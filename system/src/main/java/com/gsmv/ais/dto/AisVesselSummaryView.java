package com.gsmv.ais.dto;

import java.time.LocalDateTime;

public record AisVesselSummaryView(
        long totalRecords,
        LocalDateTime firstBaseDateTime,
        LocalDateTime latestBaseDateTime,
        AisRecordView latestRecord
) {
    public static AisVesselSummaryView empty() {
        return new AisVesselSummaryView(0, null, null, null);
    }
}
