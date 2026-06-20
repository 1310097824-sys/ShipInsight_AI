package com.gsmv.vessel;

import com.gsmv.audit.service.AuditService;
import com.gsmv.common.ErrorCode;
import com.gsmv.common.PageResponse;
import com.gsmv.common.exception.BusinessException;
import com.gsmv.common.exception.NotFoundException;
import com.gsmv.media.MediaFileService;
import com.gsmv.media.model.MediaFile;
import com.gsmv.security.SecurityUtils;
import com.gsmv.versioning.EntityVersionService;
import com.gsmv.versioning.dto.EntityVersionView;
import com.gsmv.versioning.dto.VersionFieldChangeView;
import com.gsmv.vessel.dto.VesselDetailView;
import com.gsmv.vessel.dto.VesselImageView;
import com.gsmv.vessel.dto.VesselRow;
import com.gsmv.vessel.dto.VesselSaveRequest;
import com.gsmv.vessel.dto.VesselTypeOption;
import com.gsmv.vessel.dto.VesselVersionSnapshot;
import com.gsmv.vessel.dto.VesselView;
import com.gsmv.vessel.mapper.VesselMapper;
import com.gsmv.vessel.mapper.VesselTypeMapper;
import com.gsmv.vessel.model.VesselProfile;
import com.gsmv.vessel.model.VesselType;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VesselService {

    private static final String VESSEL_IMAGE_BUSINESS_TYPE = "VESSEL_IMAGE";

    private final VesselMapper vesselMapper;
    private final VesselTypeMapper vesselTypeMapper;
    private final MediaFileService mediaFileService;
    private final AuditService auditService;
    private final EntityVersionService entityVersionService;

    public VesselService(
            VesselMapper vesselMapper,
            VesselTypeMapper vesselTypeMapper,
            MediaFileService mediaFileService,
            AuditService auditService,
            EntityVersionService entityVersionService
    ) {
        this.vesselMapper = vesselMapper;
        this.vesselTypeMapper = vesselTypeMapper;
        this.mediaFileService = mediaFileService;
        this.auditService = auditService;
        this.entityVersionService = entityVersionService;
    }

    public PageResponse<VesselView> listVessels(
            String keyword,
            Integer status,
            Long typeId,
            String riskLevel,
            String navigationStatus,
            String routeKeyword,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        List<VesselType> types = vesselTypeMapper.findAll();
        List<Long> typeIds = resolveTypeFilterIds(types, typeId);
        if (typeId != null && typeIds.isEmpty()) {
            return new PageResponse<>(List.of(), 0, safePage, safeSize);
        }

        Map<Long, VesselType> typeMap = toTypeMap(types);
        List<VesselView> items = vesselMapper.findPage(
                        normalizeNullable(keyword),
                        status,
                        typeIds,
                        normalizeNullable(riskLevel),
                        normalizeNullable(navigationStatus),
                        normalizeNullable(routeKeyword),
                        safeSize,
                        offset
                ).stream()
                .map(row -> toSummaryView(row, typeMap))
                .toList();
        long total = vesselMapper.count(
                normalizeNullable(keyword),
                status,
                typeIds,
                normalizeNullable(riskLevel),
                normalizeNullable(navigationStatus),
                normalizeNullable(routeKeyword)
        );
        return new PageResponse<>(items, total, safePage, safeSize);
    }

    public VesselDetailView getVessel(Long id) {
        VesselRow row = requireVesselRow(id);
        return toDetailView(row, toTypeMap(vesselTypeMapper.findAll()));
    }

    public List<EntityVersionView> listVersions(Long id) {
        requireVesselRow(id);
        return entityVersionService.listVersions(EntityVersionService.ENTITY_TYPE_VESSEL, id);
    }

    public List<VesselTypeOption> listTypes() {
        return vesselTypeMapper.findAll().stream()
                .map(type -> new VesselTypeOption(
                        type.getId(),
                        type.getParentId(),
                        type.getCode(),
                        type.getName(),
                        type.getDescription()
                ))
                .toList();
    }

    @Transactional
    public VesselDetailView createVessel(VesselSaveRequest request) {
        Long currentUserId = SecurityUtils.requireCurrentUser().userId();
        validateUniqueIdentity(request, null);
        validateType(request.vesselTypeId());
        VesselProfile vessel = toEntity(request);
        vesselMapper.insert(vessel);
        VesselDetailView detail = getVessel(vessel.getId());
        entityVersionService.recordVersion(
                EntityVersionService.ENTITY_TYPE_VESSEL,
                vessel.getId(),
                "CREATE",
                VesselVersionSnapshot.fromDetail(detail),
                buildChanges(null, VesselVersionSnapshot.fromDetail(detail)),
                currentUserId,
                null
        );
        auditService.record(currentUserId, "VESSEL", "CREATE", "VESSEL", vessel.getId(), true,
                "{\"vesselName\":\"" + escapeJson(detail.vesselName()) + "\"}");
        return detail;
    }

    @Transactional
    public VesselDetailView updateVessel(Long id, VesselSaveRequest request) {
        if (vesselMapper.findById(id) == null) {
            throw new NotFoundException("船舶档案不存在");
        }
        Long currentUserId = SecurityUtils.requireCurrentUser().userId();
        VesselVersionSnapshot before = VesselVersionSnapshot.fromDetail(getVessel(id));
        validateUniqueIdentity(request, id);
        validateType(request.vesselTypeId());
        VesselProfile vessel = toEntity(request);
        vessel.setId(id);
        vesselMapper.update(vessel);
        VesselDetailView detail = getVessel(id);
        VesselVersionSnapshot after = VesselVersionSnapshot.fromDetail(detail);
        entityVersionService.recordVersion(
                EntityVersionService.ENTITY_TYPE_VESSEL,
                id,
                "UPDATE",
                after,
                buildChanges(before, after),
                currentUserId,
                null
        );
        auditService.record(currentUserId, "VESSEL", "UPDATE", "VESSEL", id, true,
                "{\"vesselName\":\"" + escapeJson(detail.vesselName()) + "\"}");
        return detail;
    }

    @Transactional
    public void archiveVessel(Long id) {
        VesselProfile existing = vesselMapper.findById(id);
        if (existing == null) {
            throw new NotFoundException("船舶档案不存在");
        }
        Long currentUserId = SecurityUtils.requireCurrentUser().userId();
        VesselVersionSnapshot before = VesselVersionSnapshot.fromDetail(getVessel(id));
        vesselMapper.archiveById(id);
        VesselVersionSnapshot after = VesselVersionSnapshot.fromDetail(getVessel(id));
        entityVersionService.recordVersion(
                EntityVersionService.ENTITY_TYPE_VESSEL,
                id,
                "ARCHIVE",
                after,
                buildChanges(before, after),
                currentUserId,
                null
        );
        auditService.record(currentUserId, "VESSEL", "ARCHIVE", "VESSEL", id, true,
                "{\"vesselId\":" + id + "}");
    }

    @Transactional
    public VesselDetailView rollbackVessel(Long id, Long versionId) {
        Long currentUserId = SecurityUtils.requireCurrentUser().userId();
        if (vesselMapper.findById(id) == null) {
            throw new NotFoundException("船舶档案不存在");
        }
        VesselVersionSnapshot targetSnapshot = entityVersionService.readSnapshot(
                EntityVersionService.ENTITY_TYPE_VESSEL,
                id,
                versionId,
                VesselVersionSnapshot.class
        );
        VesselSaveRequest targetRequest = targetSnapshot.toSaveRequest();
        VesselVersionSnapshot before = VesselVersionSnapshot.fromDetail(getVessel(id));
        validateUniqueIdentity(targetRequest, id);
        validateType(targetRequest.vesselTypeId());
        VesselProfile vessel = toEntity(targetRequest);
        vessel.setId(id);
        vesselMapper.update(vessel);
        VesselDetailView detail = getVessel(id);
        VesselVersionSnapshot after = VesselVersionSnapshot.fromDetail(detail);
        entityVersionService.recordVersion(
                EntityVersionService.ENTITY_TYPE_VESSEL,
                id,
                "ROLLBACK",
                after,
                buildChanges(before, after),
                currentUserId,
                versionId
        );
        auditService.record(currentUserId, "VESSEL", "ROLLBACK", "VESSEL", id, true,
                "{\"versionId\":" + versionId + "}");
        return detail;
    }

    @Transactional
    public VesselImageView uploadImage(Long vesselId, MultipartFile file) {
        if (vesselMapper.findById(vesselId) == null) {
            throw new NotFoundException("船舶档案不存在");
        }
        if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传图片格式的船舶资料", HttpStatus.BAD_REQUEST);
        }
        return toImageView(mediaFileService.upload(VESSEL_IMAGE_BUSINESS_TYPE, vesselId, file));
    }

    public MediaFile getVesselImage(Long mediaId) {
        MediaFile mediaFile = mediaFileService.getRequired(mediaId);
        if (!VESSEL_IMAGE_BUSINESS_TYPE.equalsIgnoreCase(mediaFile.getBusinessType())) {
            throw new NotFoundException("船舶图片不存在");
        }
        return mediaFile;
    }

    private VesselRow requireVesselRow(Long id) {
        VesselRow row = vesselMapper.findRowById(id);
        if (row == null) {
            throw new NotFoundException("船舶档案不存在");
        }
        return row;
    }

    private void validateUniqueIdentity(VesselSaveRequest request, Long excludeId) {
        String mmsi = normalizeNullable(request.mmsi());
        if (mmsi != null && vesselMapper.findByMmsi(mmsi, excludeId) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "MMSI 已存在，请检查是否重复建档", HttpStatus.CONFLICT);
        }
        String imo = normalizeNullable(request.imo());
        if (imo != null && vesselMapper.findByImo(imo, excludeId) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "IMO 已存在，请检查是否重复建档", HttpStatus.CONFLICT);
        }
    }

    private void validateType(Long typeId) {
        if (typeId != null && vesselTypeMapper.findById(typeId) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "船型不存在", HttpStatus.BAD_REQUEST);
        }
    }

    private VesselProfile toEntity(VesselSaveRequest request) {
        VesselProfile vessel = new VesselProfile();
        vessel.setVesselName(normalizeRequired(request.vesselName()));
        vessel.setMmsi(normalizeNullable(request.mmsi()));
        vessel.setImo(normalizeNullable(request.imo()));
        vessel.setCallSign(normalizeNullable(request.callSign()));
        vessel.setVesselTypeId(request.vesselTypeId());
        vessel.setFlagState(normalizeNullable(request.flagState()));
        vessel.setOperatorName(normalizeNullable(request.operatorName()));
        vessel.setOwnerName(normalizeNullable(request.ownerName()));
        vessel.setLengthM(request.lengthM());
        vessel.setWidthM(request.widthM());
        vessel.setDraftM(request.draftM());
        vessel.setGrossTonnage(request.grossTonnage());
        vessel.setDeadweightTonnage(request.deadweightTonnage());
        vessel.setRiskLevel(normalizeNullable(request.riskLevel()));
        vessel.setNavigationStatus(normalizeNullable(request.navigationStatus()));
        vessel.setHomePort(normalizeNullable(request.homePort()));
        vessel.setUsualRegion(normalizeNullable(request.usualRegion()));
        vessel.setRouteArea(normalizeNullable(request.routeArea()));
        vessel.setNote(normalizeNullable(request.note()));
        vessel.setSourceText(normalizeNullable(request.sourceText()));
        vessel.setStatus(request.status());
        return vessel;
    }

    private VesselView toSummaryView(VesselRow row, Map<Long, VesselType> typeMap) {
        return new VesselView(
                row.id(),
                row.vesselName(),
                row.mmsi(),
                row.imo(),
                row.callSign(),
                row.vesselTypeId(),
                row.vesselTypeName(),
                resolveTypePath(row.vesselTypeId(), typeMap),
                row.flagState(),
                row.operatorName(),
                row.lengthM(),
                row.widthM(),
                row.draftM(),
                row.riskLevel(),
                row.navigationStatus(),
                row.usualRegion(),
                row.routeArea(),
                row.status(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private VesselDetailView toDetailView(VesselRow row, Map<Long, VesselType> typeMap) {
        List<VesselImageView> images = mediaFileService.list(VESSEL_IMAGE_BUSINESS_TYPE, row.id()).stream()
                .map(this::toImageView)
                .toList();
        return new VesselDetailView(
                row.id(),
                row.vesselName(),
                row.mmsi(),
                row.imo(),
                row.callSign(),
                row.vesselTypeId(),
                row.vesselTypeName(),
                resolveTypePath(row.vesselTypeId(), typeMap),
                row.flagState(),
                row.operatorName(),
                row.ownerName(),
                row.lengthM(),
                row.widthM(),
                row.draftM(),
                row.grossTonnage(),
                row.deadweightTonnage(),
                row.riskLevel(),
                row.navigationStatus(),
                row.homePort(),
                row.usualRegion(),
                row.routeArea(),
                row.note(),
                row.sourceText(),
                row.status(),
                row.createdAt(),
                row.updatedAt(),
                images
        );
    }

    private VesselImageView toImageView(MediaFile mediaFile) {
        return new VesselImageView(
                mediaFile.getId(),
                "/api/v1/vessels/images/" + mediaFile.getId(),
                mediaFile.getOriginalFilename()
        );
    }

    private List<Long> resolveTypeFilterIds(List<VesselType> types, Long rootTypeId) {
        if (rootTypeId == null) {
            return null;
        }
        Map<Long, List<VesselType>> childrenMap = types.stream()
                .filter(type -> type.getParentId() != null)
                .collect(Collectors.groupingBy(VesselType::getParentId));
        List<Long> ids = new ArrayList<>();
        ArrayDeque<Long> stack = new ArrayDeque<>();
        stack.push(rootTypeId);
        while (!stack.isEmpty()) {
            Long currentId = stack.pop();
            ids.add(currentId);
            for (VesselType child : childrenMap.getOrDefault(currentId, List.of())) {
                stack.push(child.getId());
            }
        }
        return ids;
    }

    private Map<Long, VesselType> toTypeMap(List<VesselType> types) {
        return types.stream().collect(Collectors.toMap(VesselType::getId, type -> type, (left, right) -> left));
    }

    private String resolveTypePath(Long typeId, Map<Long, VesselType> typeMap) {
        if (typeId == null) {
            return null;
        }
        ArrayDeque<String> names = new ArrayDeque<>();
        Long currentId = typeId;
        while (currentId != null) {
            VesselType type = typeMap.get(currentId);
            if (type == null) {
                break;
            }
            names.push(type.getName());
            currentId = type.getParentId();
        }
        return String.join(" / ", names);
    }

    private List<VersionFieldChangeView> buildChanges(VesselVersionSnapshot before, VesselVersionSnapshot after) {
        List<VersionFieldChangeView> changes = new ArrayList<>();
        addChange(changes, "vesselName", "船名", before == null ? null : before.vesselName(), after == null ? null : after.vesselName());
        addChange(changes, "mmsi", "MMSI", before == null ? null : before.mmsi(), after == null ? null : after.mmsi());
        addChange(changes, "imo", "IMO", before == null ? null : before.imo(), after == null ? null : after.imo());
        addChange(changes, "callSign", "呼号", before == null ? null : before.callSign(), after == null ? null : after.callSign());
        addChange(changes, "vesselTypeName", "船型", before == null ? null : before.vesselTypeName(), after == null ? null : after.vesselTypeName());
        addChange(changes, "flagState", "船旗", before == null ? null : before.flagState(), after == null ? null : after.flagState());
        addChange(changes, "operatorName", "运营方", before == null ? null : before.operatorName(), after == null ? null : after.operatorName());
        addChange(changes, "ownerName", "所有方", before == null ? null : before.ownerName(), after == null ? null : after.ownerName());
        addChange(changes, "lengthM", "船长", decimalToString(before == null ? null : before.lengthM()), decimalToString(after == null ? null : after.lengthM()));
        addChange(changes, "widthM", "船宽", decimalToString(before == null ? null : before.widthM()), decimalToString(after == null ? null : after.widthM()));
        addChange(changes, "draftM", "吃水", decimalToString(before == null ? null : before.draftM()), decimalToString(after == null ? null : after.draftM()));
        addChange(changes, "riskLevel", "风险等级", before == null ? null : before.riskLevel(), after == null ? null : after.riskLevel());
        addChange(changes, "navigationStatus", "航行状态", before == null ? null : before.navigationStatus(), after == null ? null : after.navigationStatus());
        addChange(changes, "homePort", "常驻港", before == null ? null : before.homePort(), after == null ? null : after.homePort());
        addChange(changes, "usualRegion", "常用区域", before == null ? null : before.usualRegion(), after == null ? null : after.usualRegion());
        addChange(changes, "routeArea", "航线范围", before == null ? null : before.routeArea(), after == null ? null : after.routeArea());
        addChange(changes, "note", "备注", before == null ? null : before.note(), after == null ? null : after.note());
        addChange(changes, "sourceText", "资料来源", before == null ? null : before.sourceText(), after == null ? null : after.sourceText());
        addChange(changes, "status", "档案状态", formatStatus(before == null ? null : before.status()), formatStatus(after == null ? null : after.status()));
        return changes;
    }

    private void addChange(List<VersionFieldChangeView> changes, String fieldKey, String fieldLabel, String oldValue, String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(new VersionFieldChangeView(fieldKey, fieldLabel, oldValue, newValue));
        }
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String decimalToString(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private String formatStatus(Integer value) {
        if (value == null) {
            return null;
        }
        return value == 1 ? "启用" : "归档";
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
