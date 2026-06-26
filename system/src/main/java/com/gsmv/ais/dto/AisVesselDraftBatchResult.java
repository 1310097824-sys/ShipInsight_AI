package com.gsmv.ais.dto;

public record AisVesselDraftBatchResult(
        int scanned,
        int created,
        int skippedExisting,
        int skippedInvalid,
        int limit
) {
}
