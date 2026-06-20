package com.gsmv.vessel.dto;

import java.math.BigDecimal;

public record VesselVersionSnapshot(
        String vesselName,
        String mmsi,
        String imo,
        String callSign,
        Long vesselTypeId,
        String vesselTypeName,
        String flagState,
        String operatorName,
        String ownerName,
        BigDecimal lengthM,
        BigDecimal widthM,
        BigDecimal draftM,
        BigDecimal grossTonnage,
        BigDecimal deadweightTonnage,
        String riskLevel,
        String navigationStatus,
        String homePort,
        String usualRegion,
        String routeArea,
        String note,
        String sourceText,
        Integer status
) {
    public static VesselVersionSnapshot fromDetail(VesselDetailView detail) {
        return new VesselVersionSnapshot(
                detail.vesselName(),
                detail.mmsi(),
                detail.imo(),
                detail.callSign(),
                detail.vesselTypeId(),
                detail.vesselTypeName(),
                detail.flagState(),
                detail.operatorName(),
                detail.ownerName(),
                detail.lengthM(),
                detail.widthM(),
                detail.draftM(),
                detail.grossTonnage(),
                detail.deadweightTonnage(),
                detail.riskLevel(),
                detail.navigationStatus(),
                detail.homePort(),
                detail.usualRegion(),
                detail.routeArea(),
                detail.note(),
                detail.sourceText(),
                detail.status()
        );
    }

    public VesselSaveRequest toSaveRequest() {
        return new VesselSaveRequest(
                vesselName,
                mmsi,
                imo,
                callSign,
                vesselTypeId,
                flagState,
                operatorName,
                ownerName,
                lengthM,
                widthM,
                draftM,
                grossTonnage,
                deadweightTonnage,
                riskLevel,
                navigationStatus,
                homePort,
                usualRegion,
                routeArea,
                note,
                sourceText,
                status
        );
    }
}
