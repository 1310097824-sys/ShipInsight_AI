package com.gsmv.ais.dto;

public record AisConvertedCsvSaveResult(
        String fileName,
        String savedPath,
        long sizeBytes
) {
}
