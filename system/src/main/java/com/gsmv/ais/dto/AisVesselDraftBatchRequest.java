package com.gsmv.ais.dto;

import java.time.LocalDateTime;

public record AisVesselDraftBatchRequest(
        String keyword,
        LocalDateTime observedFrom,
        LocalDateTime observedTo,
        Integer limit
) {
}
