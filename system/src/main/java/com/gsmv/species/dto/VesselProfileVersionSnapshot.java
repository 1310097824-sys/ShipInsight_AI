package com.gsmv.species.dto;

import java.math.BigDecimal;

public record VesselProfileVersionSnapshot(
        String phylumName,
        String className,
        String orderName,
        String familyName,
        String genusName,
        String scientificName,
        String chineseName,
        String protectionLevel,
        String iucnStatus,
        String description,
        String morphology,
        String habit,
        String habitat,
        String distribution,
        BigDecimal distributionLat,
        BigDecimal distributionLng,
        String geoRangeText,
        String videoUrl,
        String referenceText,
        Integer status
) {

    public static VesselProfileVersionSnapshot fromDetail(VesselProfileDetailView detail) {
        return new VesselProfileVersionSnapshot(
                detail.phylumName(),
                detail.className(),
                detail.orderName(),
                detail.familyName(),
                detail.genusName(),
                detail.scientificName(),
                detail.chineseName(),
                detail.protectionLevel(),
                detail.iucnStatus(),
                detail.description(),
                detail.morphology(),
                detail.habit(),
                detail.habitat(),
                detail.distribution(),
                detail.distributionLat(),
                detail.distributionLng(),
                detail.geoRangeText(),
                detail.videoUrl(),
                detail.referenceText(),
                detail.status()
        );
    }

    public VesselProfileSaveRequest toSaveRequest() {
        return new VesselProfileSaveRequest(
                phylumName,
                className,
                orderName,
                familyName,
                genusName,
                scientificName,
                chineseName,
                protectionLevel,
                iucnStatus,
                description,
                morphology,
                habit,
                habitat,
                distribution,
                distributionLat,
                distributionLng,
                geoRangeText,
                videoUrl,
                referenceText,
                status
        );
    }
}
