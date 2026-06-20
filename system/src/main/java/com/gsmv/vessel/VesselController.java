package com.gsmv.vessel;

import com.gsmv.common.ApiResponse;
import com.gsmv.common.PageResponse;
import com.gsmv.media.MediaFileService;
import com.gsmv.media.model.MediaFile;
import com.gsmv.versioning.dto.EntityVersionView;
import com.gsmv.vessel.dto.VesselDetailView;
import com.gsmv.vessel.dto.VesselImageView;
import com.gsmv.vessel.dto.VesselSaveRequest;
import com.gsmv.vessel.dto.VesselTypeOption;
import com.gsmv.vessel.dto.VesselView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/vessels")
public class VesselController {

    private final VesselService vesselService;
    private final MediaFileService mediaFileService;

    public VesselController(VesselService vesselService, MediaFileService mediaFileService) {
        this.vesselService = vesselService;
        this.mediaFileService = mediaFileService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VESSEL_READ') or hasRole('ADMIN')")
    public ApiResponse<PageResponse<VesselView>> listVessels(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String navigationStatus,
            @RequestParam(required = false) String routeKeyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(vesselService.listVessels(
                keyword,
                status,
                typeId,
                riskLevel,
                navigationStatus,
                routeKeyword,
                page,
                size
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VESSEL_READ') or hasRole('ADMIN')")
    public ApiResponse<VesselDetailView> getVessel(@PathVariable Long id) {
        return ApiResponse.success(vesselService.getVessel(id));
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('VESSEL_READ') or hasRole('ADMIN')")
    public ApiResponse<List<EntityVersionView>> listVersions(@PathVariable Long id) {
        return ApiResponse.success(vesselService.listVersions(id));
    }

    @GetMapping("/types")
    @PreAuthorize("hasAuthority('VESSEL_READ') or hasRole('ADMIN')")
    public ApiResponse<List<VesselTypeOption>> listTypes() {
        return ApiResponse.success(vesselService.listTypes());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('VESSEL_WRITE') or hasRole('ADMIN')")
    public ApiResponse<VesselDetailView> createVessel(@Valid @RequestBody VesselSaveRequest request) {
        return ApiResponse.success(vesselService.createVessel(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('VESSEL_WRITE') or hasRole('ADMIN')")
    public ApiResponse<VesselDetailView> updateVessel(@PathVariable Long id, @Valid @RequestBody VesselSaveRequest request) {
        return ApiResponse.success(vesselService.updateVessel(id, request));
    }

    @PostMapping("/{id}/versions/{versionId}/rollback")
    @PreAuthorize("hasAuthority('VESSEL_WRITE') or hasRole('ADMIN')")
    public ApiResponse<VesselDetailView> rollbackVessel(@PathVariable Long id, @PathVariable Long versionId) {
        return ApiResponse.success(vesselService.rollbackVessel(id, versionId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('VESSEL_WRITE') or hasRole('ADMIN')")
    public ApiResponse<Void> archiveVessel(@PathVariable Long id) {
        vesselService.archiveVessel(id);
        return ApiResponse.success(null);
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('VESSEL_WRITE') or hasRole('ADMIN')")
    public ApiResponse<VesselImageView> uploadVesselImage(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(vesselService.uploadImage(id, file));
    }

    @GetMapping("/images/{mediaId}")
    public ResponseEntity<byte[]> image(@PathVariable Long mediaId) {
        MediaFile mediaFile = vesselService.getVesselImage(mediaId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mediaFile.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + mediaFile.getStoredFilename() + "\"")
                .cacheControl(CacheControl.noCache())
                .body(mediaFileService.readBytes(mediaFile));
    }
}
