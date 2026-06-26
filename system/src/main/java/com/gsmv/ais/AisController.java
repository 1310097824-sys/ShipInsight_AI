package com.gsmv.ais;

import com.gsmv.ais.dto.AisBatchDeleteRequest;
import com.gsmv.ais.dto.AisBatchOperationResult;
import com.gsmv.ais.dto.AisConvertedCsvSaveResult;
import com.gsmv.ais.dto.AisBatchUpdateRequest;
import com.gsmv.ais.dto.AisDatasetDateStat;
import com.gsmv.ais.dto.AisImportProgress;
import com.gsmv.ais.dto.AisImportResult;
import com.gsmv.ais.dto.AisRankingStat;
import com.gsmv.ais.dto.AisRecordView;
import com.gsmv.ais.dto.AisRiskSummary;
import com.gsmv.ais.dto.AisVesselDraftBatchRequest;
import com.gsmv.ais.dto.AisVesselDraftBatchResult;
import com.gsmv.common.ApiResponse;
import com.gsmv.common.PageResponse;
import com.gsmv.vessel.VesselService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final VesselService vesselService;

    public AisController(AisService aisService, VesselService vesselService) {
        this.aisService = aisService;
        this.vesselService = vesselService;
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

    @GetMapping("/map")
    @PreAuthorize("hasAuthority('OBS_READ')")
    public ApiResponse<PageResponse<AisRecordView>> mapLatest(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datasetDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime observedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime observedTo,
            @RequestParam(defaultValue = "50000") int limit
    ) {
        return ApiResponse.success(aisService.mapLatest(keyword, observedFrom, observedTo, datasetDate, limit));
    }

    @GetMapping("/dataset-dates")
    @PreAuthorize("hasAuthority('OBS_READ')")
    public ApiResponse<List<String>> datasetDates() {
        return ApiResponse.success(aisService.datasetDates());
    }

    @GetMapping("/stats/dataset-dates")
    @PreAuthorize("hasAuthority('OBS_READ')")
    public ApiResponse<List<AisDatasetDateStat>> datasetDateStats(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime observedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime observedTo
    ) {
        return ApiResponse.success(aisService.datasetDateStats(keyword, observedFrom, observedTo));
    }

    @GetMapping("/stats/importers")
    @PreAuthorize("hasAuthority('OBS_READ')")
    public ApiResponse<List<AisRankingStat>> importerStats(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime observedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime observedTo,
            @RequestParam(defaultValue = "6") int limit
    ) {
        return ApiResponse.success(aisService.importerStats(keyword, observedFrom, observedTo, limit));
    }

    @GetMapping("/stats/risk-summary")
    @PreAuthorize("hasAuthority('OBS_READ')")
    public ApiResponse<AisRiskSummary> riskSummary(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime observedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime observedTo
    ) {
        return ApiResponse.success(aisService.riskSummary(keyword, observedFrom, observedTo));
    }

    @GetMapping("/{mmsi}/track")
    @PreAuthorize("hasAuthority('OBS_READ')")
    public ApiResponse<PageResponse<AisRecordView>> vesselTrack(
            @PathVariable String mmsi,
            @RequestParam(defaultValue = "5000") int limit
    ) {
        return ApiResponse.success(aisService.vesselTrack(mmsi, limit));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('OBS_WRITE')")
    public ApiResponse<AisImportResult> importFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String taskId
    ) {
        return ApiResponse.success(aisService.importFile(file, limit, taskId));
    }

    @PostMapping(value = "/converted-csv/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('OBS_WRITE')")
    public ApiResponse<AisConvertedCsvSaveResult> saveConvertedCsv(
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(aisService.saveConvertedCsv(file));
    }

    @GetMapping("/import/progress/{taskId}")
    @PreAuthorize("hasAuthority('OBS_WRITE')")
    public ApiResponse<AisImportProgress> importProgress(@PathVariable String taskId) {
        return ApiResponse.success(aisService.importProgress(taskId));
    }

    @PostMapping("/vessel-drafts")
    @PreAuthorize("hasAuthority('VESSEL_WRITE') or hasRole('ADMIN')")
    public ApiResponse<AisVesselDraftBatchResult> generateVesselDrafts(@RequestBody(required = false) AisVesselDraftBatchRequest request) {
        return ApiResponse.success(vesselService.generateVesselDraftsFromAis(request));
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
