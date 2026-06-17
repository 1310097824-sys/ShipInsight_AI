package com.gsmv.ais.dto;

public record AisImportResult(
        String sourceFile,
        int imported,
        int skipped,
        int limit
) {
}
