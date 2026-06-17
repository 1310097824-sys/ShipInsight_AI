package com.gsmv.ais;

import com.gsmv.ais.dto.AisBatchDeleteRequest;
import com.gsmv.ais.dto.AisBatchOperationResult;
import com.gsmv.ais.dto.AisBatchUpdateRequest;
import com.gsmv.ais.dto.AisImportResult;
import com.gsmv.ais.dto.AisRecordView;
import com.gsmv.common.ApiResponse;
import com.gsmv.common.PageResponse;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ais-records")
public class AisController {

    private final AisService aisService;

    public AisController(AisService aisService) {
        this.aisService = aisService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('OBS_READ')")
    public ApiResponse<PageResponse<AisRecordView>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime observedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime observedTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(aisService.list(keyword, observedFrom, observedTo, page, size));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('OBS_WRITE')")
    public ApiResponse<AisImportResult> importFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.success(aisService.importFile(file, limit));
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('OBS_WRITE')")
    public ApiResponse<AisBatchOperationResult> deleteBatch(@RequestBody AisBatchDeleteRequest request) {
        return ApiResponse.success(aisService.deleteBatch(request));
    }

    @PatchMapping("/batch")
    @PreAuthorize("hasAuthority('OBS_WRITE')")
    public ApiResponse<AisBatchOperationResult> updateBatch(@RequestBody AisBatchUpdateRequest request) {
        return ApiResponse.success(aisService.updateBatch(request));
    }
}
