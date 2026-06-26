package com.gsmv.ais.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AisVesselDraftCandidate(
        String recordId,
        String mmsi,
        String imo,
        String vesselName,
        String callSign,
        BigDecimal length,
        BigDecimal width,
        BigDecimal draft,
        String sourceFile,
        LocalDateTime baseDateTime
) {
}
