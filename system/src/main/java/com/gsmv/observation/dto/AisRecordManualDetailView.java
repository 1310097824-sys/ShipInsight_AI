package com.gsmv.observation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AisRecordManualDetailView(
        Long id,
        Long ecosystemId,
        String ecosystemName,
        Long observerUserId,
        String observerName,
        LocalDateTime observedAt,
        BigDecimal locationLat,
        BigDecimal locationLng,
        String locationName,
        String envJson,
        String note,
        LocalDateTime createdAt,
        List<AisRecordManualVesselView> speciesItems
) {
}
