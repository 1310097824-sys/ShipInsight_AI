package com.gsmv.ais.dto;

public record AisLinkedVesselView(
        Long vesselId,
        String vesselName,
        String mmsi,
        String imo,
        String riskLevel,
        String navigationStatus,
        Integer status,
        String matchMethod
) {
}
