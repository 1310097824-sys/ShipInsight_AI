package com.gsmv.ais.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AisBatchUpdateRequest(
        List<String> ids,
        Boolean allMatched,
        String keyword,
        LocalDateTime observedFrom,
        LocalDateTime observedTo,
        Map<String, Object> fields
) {
}
