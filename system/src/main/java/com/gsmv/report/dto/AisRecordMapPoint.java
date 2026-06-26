package com.gsmv.report.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AisRecordMapPoint(
        Long recordId,
        String shippingZoneName,
        String recorderName,
        LocalDateTime recordedAt,
        BigDecimal locationLat,
        BigDecimal locationLng,
        String locationName,
        long linkedVesselCount,
        String note
) {
}
