package com.gsmv.ais.dto;

import java.time.LocalDateTime;

public record AisImportProgress(
        String taskId,
        String sourceFile,
        String status,
        long bytesRead,
        long totalBytes,
        int imported,
        int skipped,
        int limit,
        int progress,
        String message,
        LocalDateTime startedAt,
        LocalDateTime updatedAt
) {
}
