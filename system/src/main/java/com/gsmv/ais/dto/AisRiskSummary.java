package com.gsmv.ais.dto;

public record AisRiskSummary(
        long total,
        long lowSpeedCount,
        long stoppedCount,
        long abnormalNoteCount,
        long uniqueVesselCount
) {
    public static AisRiskSummary empty() {
        return new AisRiskSummary(0, 0, 0, 0, 0);
    }

    public boolean hasSignals() {
        return lowSpeedCount > 0 || stoppedCount > 0 || abnormalNoteCount > 0;
    }
}
