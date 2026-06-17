package com.gsmv.ais.dto;

public record AisBatchOperationResult(
        long affected,
        String operation
) {
}
