package com.gsmv.ais.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AisRecordView(
        String id,
        String mmsi,
        LocalDateTime baseDateTime,
        BigDecimal longitude,
        BigDecimal latitude,
        BigDecimal sog,
        BigDecimal cog,
        Integer heading,
        String vesselName,
        String imo,
        String callSign,
        Integer vesselType,
        Integer status,
        BigDecimal length,
        BigDecimal width,
        BigDecimal draft,
        Integer cargo,
        String transceiver,
        String note,
        String sourceFile,
        Long importedByUserId,
        String importedByName,
        LocalDateTime importedAt,
        AisLinkedVesselView linkedVessel
) {
    public AisRecordView withLinkedVessel(AisLinkedVesselView linkedVessel) {
        return new AisRecordView(
                id,
                mmsi,
                baseDateTime,
                longitude,
                latitude,
                sog,
                cog,
                heading,
                vesselName,
                imo,
                callSign,
                vesselType,
                status,
                length,
                width,
                draft,
                cargo,
                transceiver,
                note,
                sourceFile,
                importedByUserId,
                importedByName,
                importedAt,
                linkedVessel
        );
    }
}
