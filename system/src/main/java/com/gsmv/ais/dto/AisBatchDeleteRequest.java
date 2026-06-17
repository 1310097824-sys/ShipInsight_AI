package com.gsmv.ais.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AisBatchDeleteRequest(
        List<String> ids,
        Boolean allMatched,
        String keyword,
        LocalDateTime observedFrom,
        LocalDateTime observedTo
) {
}
