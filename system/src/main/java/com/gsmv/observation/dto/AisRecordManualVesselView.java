package com.gsmv.observation.dto;

public record AisRecordManualVesselView(
        Long vesselId,
        String vesselName,
        String mmsi,
        String imo,
        Integer status,
        Integer countEstimated,
        String behavior,
        String comment
) {
}
