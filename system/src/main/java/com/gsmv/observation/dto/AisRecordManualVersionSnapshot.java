package com.gsmv.observation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AisRecordManualVersionSnapshot(
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
        List<AisRecordManualVersionVesselSnapshot> vesselItems
) {

    public static AisRecordManualVersionSnapshot fromDetail(AisRecordManualDetailView detail) {
        return new AisRecordManualVersionSnapshot(
                detail.ecosystemId(),
                detail.ecosystemName(),
                detail.observerUserId(),
                detail.observerName(),
                detail.observedAt(),
                detail.locationLat(),
                detail.locationLng(),
                detail.locationName(),
                detail.envJson(),
                detail.note(),
                detail.speciesItems().stream()
                        .map(AisRecordManualVersionVesselSnapshot::fromDetailItem)
                        .toList()
        );
    }

    public AisRecordManualSaveRequest toSaveRequest() {
        return new AisRecordManualSaveRequest(
                ecosystemId,
                observedAt,
                locationLat,
                locationLng,
                locationName,
                envJson,
                note,
                vesselItems == null ? List.of() : vesselItems.stream()
                        .map(AisRecordManualVersionVesselSnapshot::toInput)
                        .toList()
        );
    }
}
