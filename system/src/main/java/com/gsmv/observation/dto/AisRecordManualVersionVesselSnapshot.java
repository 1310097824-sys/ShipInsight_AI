package com.gsmv.observation.dto;

public record AisRecordManualVersionVesselSnapshot(
        Long vesselId,
        String profileName,
        String displayName,
        Integer countEstimated,
        String behavior,
        String comment
) {

    public static AisRecordManualVersionVesselSnapshot fromDetailItem(AisRecordManualVesselView item) {
        return new AisRecordManualVersionVesselSnapshot(
                item.vesselId(),
                item.mmsi(),
                item.vesselName(),
                item.countEstimated(),
                item.behavior(),
                item.comment()
        );
    }

    public AisRecordManualVesselInput toInput() {
        return new AisRecordManualVesselInput(vesselId, countEstimated, behavior, comment);
    }
}
