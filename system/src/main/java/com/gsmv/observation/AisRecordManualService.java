package com.gsmv.observation;

import com.gsmv.ai.AssistantQueryCache;
import com.gsmv.ai.rag.RagKnowledgeService;
import com.gsmv.audit.service.AuditService;
import com.gsmv.common.ErrorCode;
import com.gsmv.common.PageResponse;
import com.gsmv.common.exception.BusinessException;
import com.gsmv.common.exception.NotFoundException;
import com.gsmv.ecosystem.mapper.ShippingZoneMapper;
import com.gsmv.observation.dto.AisRecordManualDetailView;
import com.gsmv.observation.dto.AisRecordManualSaveRequest;
import com.gsmv.observation.dto.AisRecordManualVesselInput;
import com.gsmv.observation.dto.AisRecordManualVersionSnapshot;
import com.gsmv.observation.dto.AisRecordManualVersionVesselSnapshot;
import com.gsmv.observation.dto.AisRecordManualView;
import com.gsmv.observation.mapper.AisRecordManualMapper;
import com.gsmv.observation.model.AisRecordManual;
import com.gsmv.security.CurrentUser;
import com.gsmv.security.SecurityUtils;
import com.gsmv.vessel.dto.VesselRow;
import com.gsmv.vessel.mapper.VesselMapper;
import com.gsmv.versioning.EntityVersionService;
import com.gsmv.versioning.dto.EntityVersionView;
import com.gsmv.versioning.dto.VersionFieldChangeView;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AisRecordManualService {

    private final AisRecordManualMapper observationMapper;
    private final ShippingZoneMapper ecosystemMapper;
    private final VesselMapper vesselMapper;
    private final AuditService auditService;
    private final AssistantQueryCache assistantQueryCache;
    private final EntityVersionService entityVersionService;
    private final RagKnowledgeService ragKnowledgeService;

    public AisRecordManualService(
            AisRecordManualMapper observationMapper,
            ShippingZoneMapper ecosystemMapper,
            VesselMapper vesselMapper,
            AuditService auditService,
            AssistantQueryCache assistantQueryCache,
            EntityVersionService entityVersionService,
            RagKnowledgeService ragKnowledgeService
    ) {
        this.observationMapper = observationMapper;
        this.ecosystemMapper = ecosystemMapper;
        this.vesselMapper = vesselMapper;
        this.auditService = auditService;
        this.assistantQueryCache = assistantQueryCache;
        this.entityVersionService = entityVersionService;
        this.ragKnowledgeService = ragKnowledgeService;
    }

    public PageResponse<AisRecordManualView> list(
            Long ecosystemId,
            String keyword,
            LocalDateTime observedFrom,
            LocalDateTime observedTo,
            int page,
            int size
    ) {
        validateDateRange(observedFrom, observedTo);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        String normalizedKeyword = normalizeNullable(keyword);
        List<AisRecordManualView> items = observationMapper.findPage(
                ecosystemId,
                normalizedKeyword,
                observedFrom,
                observedTo,
                safeSize,
                offset
        );
        long total = observationMapper.count(ecosystemId, normalizedKeyword, observedFrom, observedTo);
        return new PageResponse<>(items, total, safePage, safeSize);
    }

    public AisRecordManualDetailView getDetail(Long id) {
        AisRecordManualView observation = observationMapper.findViewById(id);
        if (observation == null) {
            throw new NotFoundException("观测记录不存在");
        }
        return new AisRecordManualDetailView(
                observation.id(),
                observation.ecosystemId(),
                observation.ecosystemName(),
                observation.observerUserId(),
                observation.observerName(),
                observation.observedAt(),
                observation.locationLat(),
                observation.locationLng(),
                observation.locationName(),
                observation.envJson(),
                observation.note(),
                observation.createdAt(),
                observationMapper.findVesselViews(id)
        );
    }

    public List<EntityVersionView> listVersions(Long id) {
        if (observationMapper.findById(id) == null) {
            throw new NotFoundException("观测记录不存在");
        }
        return entityVersionService.listVersions(EntityVersionService.ENTITY_TYPE_OBSERVATION, id);
    }

    @Transactional
    public AisRecordManualDetailView create(AisRecordManualSaveRequest request) {
        List<AisRecordManualVesselInput> normalizedVesselItems = validateAndNormalize(request, Set.of());
        CurrentUser currentUser = SecurityUtils.requireCurrentUser();
        AisRecordManual observation = toObservation(request, currentUser.userId());
        observationMapper.insert(observation);
        replaceVesselItems(observation.getId(), normalizedVesselItems);
        AisRecordManualDetailView detailView = getDetail(observation.getId());
        entityVersionService.recordVersion(
                EntityVersionService.ENTITY_TYPE_OBSERVATION,
                observation.getId(),
                "CREATE",
                AisRecordManualVersionSnapshot.fromDetail(detailView),
                buildObservationChanges(null, AisRecordManualVersionSnapshot.fromDetail(detailView)),
                currentUser.userId(),
                null
        );
        assistantQueryCache.invalidateAll();
        ragKnowledgeService.syncObservation(observation.getId());
        auditService.record(currentUser.userId(), "OBSERVATION", "CREATE", "OBSERVATION", observation.getId(), true,
                "{\"ecosystemId\":" + request.ecosystemId() + ",\"vesselCount\":" + normalizedVesselItems.size() + "}");
        return detailView;
    }

    @Transactional
    public AisRecordManualDetailView update(Long id, AisRecordManualSaveRequest request) {
        AisRecordManual existing = observationMapper.findById(id);
        if (existing == null) {
            throw new NotFoundException("观测记录不存在");
        }
        Set<Long> existingVesselIds = existingVesselIds(id);
        List<AisRecordManualVesselInput> normalizedVesselItems = validateAndNormalize(request, existingVesselIds);
        AisRecordManualVersionSnapshot beforeSnapshot = AisRecordManualVersionSnapshot.fromDetail(getDetail(id));
        AisRecordManual observation = toObservation(request, existing.getObserverUserId());
        observation.setId(id);
        observationMapper.update(observation);
        replaceVesselItems(id, normalizedVesselItems);
        AisRecordManualDetailView detailView = getDetail(id);
        AisRecordManualVersionSnapshot afterSnapshot = AisRecordManualVersionSnapshot.fromDetail(detailView);
        entityVersionService.recordVersion(
                EntityVersionService.ENTITY_TYPE_OBSERVATION,
                id,
                "UPDATE",
                afterSnapshot,
                buildObservationChanges(beforeSnapshot, afterSnapshot),
                SecurityUtils.requireCurrentUser().userId(),
                null
        );
        assistantQueryCache.invalidateAll();
        ragKnowledgeService.syncObservation(id);
        auditService.record(SecurityUtils.requireCurrentUser().userId(), "OBSERVATION", "UPDATE", "OBSERVATION", id, true,
                "{\"ecosystemId\":" + request.ecosystemId() + ",\"vesselCount\":" + normalizedVesselItems.size() + "}");
        return detailView;
    }

    @Transactional
    public void delete(Long id) {
        AisRecordManual existing = observationMapper.findById(id);
        if (existing == null) {
            throw new NotFoundException("观测记录不存在");
        }
        Long currentUserId = SecurityUtils.requireCurrentUser().userId();
        AisRecordManualVersionSnapshot beforeSnapshot = AisRecordManualVersionSnapshot.fromDetail(getDetail(id));
        observationMapper.deleteVesselsByObservationId(id);
        observationMapper.deleteById(id);
        entityVersionService.recordVersion(
                EntityVersionService.ENTITY_TYPE_OBSERVATION,
                id,
                "DELETE",
                beforeSnapshot,
                buildObservationChanges(beforeSnapshot, null),
                currentUserId,
                null
        );
        assistantQueryCache.invalidateAll();
        ragKnowledgeService.markSourceDeleted(RagKnowledgeService.SOURCE_OBSERVATION, id);
        auditService.record(currentUserId, "OBSERVATION", "DELETE", "OBSERVATION", id, true,
                "{\"ecosystemId\":" + existing.getEcosystemId() + "}");
    }

    @Transactional
    public AisRecordManualDetailView rollback(Long id, Long versionId) {
        Long currentUserId = SecurityUtils.requireCurrentUser().userId();
        AisRecordManualVersionSnapshot targetSnapshot = entityVersionService.readSnapshot(
                EntityVersionService.ENTITY_TYPE_OBSERVATION,
                id,
                versionId,
                AisRecordManualVersionSnapshot.class
        );
        AisRecordManual existing = observationMapper.findById(id);
        AisRecordManualVersionSnapshot beforeSnapshot = existing == null ? null : AisRecordManualVersionSnapshot.fromDetail(getDetail(id));
        Set<Long> existingVesselIds = existing == null ? Set.of() : existingVesselIds(id);
        AisRecordManualSaveRequest targetRequest = targetSnapshot.toSaveRequest();
        List<AisRecordManualVesselInput> normalizedVesselItems = validateAndNormalize(targetRequest, existingVesselIds);

        AisRecordManual observation = toObservation(targetRequest, targetSnapshot.observerUserId());
        observation.setId(id);
        if (existing == null) {
            observationMapper.insertWithId(observation);
        } else {
            observationMapper.update(observation);
            observationMapper.deleteVesselsByObservationId(id);
        }
        replaceVesselItems(id, normalizedVesselItems);

        AisRecordManualDetailView detailView = getDetail(id);
        AisRecordManualVersionSnapshot afterSnapshot = AisRecordManualVersionSnapshot.fromDetail(detailView);
        entityVersionService.recordVersion(
                EntityVersionService.ENTITY_TYPE_OBSERVATION,
                id,
                "ROLLBACK",
                afterSnapshot,
                buildObservationChanges(beforeSnapshot, afterSnapshot),
                currentUserId,
                versionId
        );
        assistantQueryCache.invalidateAll();
        ragKnowledgeService.syncObservation(id);
        auditService.record(currentUserId, "OBSERVATION", "ROLLBACK", "OBSERVATION", id, true,
                "{\"versionId\":" + versionId + "}");
        return detailView;
    }

    private AisRecordManual toObservation(AisRecordManualSaveRequest request, Long observerUserId) {
        AisRecordManual observation = new AisRecordManual();
        observation.setEcosystemId(request.ecosystemId());
        observation.setObserverUserId(observerUserId);
        observation.setObservedAt(request.observedAt());
        observation.setLocationLat(request.locationLat());
        observation.setLocationLng(request.locationLng());
        observation.setLocationName(normalizeNullable(request.locationName()));
        observation.setEnvJson(normalizeNullable(request.envJson()));
        observation.setNote(normalizeNullable(request.note()));
        return observation;
    }

    private void replaceVesselItems(Long observationId, List<AisRecordManualVesselInput> vesselItems) {
        observationMapper.deleteVesselsByObservationId(observationId);
        if (!vesselItems.isEmpty()) {
            observationMapper.insertVesselBatch(observationId, vesselItems);
        }
    }

    private Set<Long> existingVesselIds(Long observationId) {
        return observationMapper.findVesselViews(observationId).stream()
                .map(com.gsmv.observation.dto.AisRecordManualVesselView::vesselId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }

    private List<AisRecordManualVesselInput> validateAndNormalize(AisRecordManualSaveRequest request, Set<Long> existingVesselIds) {
        if (ecosystemMapper.findById(request.ecosystemId()) == null) {
            throw new NotFoundException("生态系统不存在");
        }

        List<AisRecordManualVesselInput> inputItems = request.vesselItems() == null ? List.of() : request.vesselItems();
        List<AisRecordManualVesselInput> normalizedItems = new ArrayList<>();
        Set<Long> seenVesselIds = new HashSet<>();

        for (AisRecordManualVesselInput item : inputItems) {
            if (item == null) {
                continue;
            }

            String behavior = normalizeNullable(item.behavior());
            String comment = normalizeNullable(item.comment());
            boolean hasContent = item.vesselId() != null || item.countEstimated() != null || behavior != null || comment != null;
            if (!hasContent) {
                continue;
            }

            if (item.vesselId() == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "请先为每条关联记录选择船舶", HttpStatus.BAD_REQUEST);
            }
            if (!seenVesselIds.add(item.vesselId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "同一条观测记录中不能重复关联相同船舶", HttpStatus.BAD_REQUEST);
            }
            VesselRow vessel = vesselMapper.findRowById(item.vesselId());
            if (vessel == null) {
                throw new NotFoundException("关联船舶不存在: " + item.vesselId());
            }
            if (!Objects.equals(vessel.status(), 1) && !existingVesselIds.contains(item.vesselId())) {
                throw new BusinessException(
                        ErrorCode.BAD_REQUEST,
                        "归档船舶不能新增为观测记录关联项，请先启用该物种或选择其他可用物种",
                        HttpStatus.BAD_REQUEST
                );
            }

            normalizedItems.add(new AisRecordManualVesselInput(item.vesselId(), item.countEstimated(), behavior, comment));
        }

        return normalizedItems;
    }

    private void validateDateRange(LocalDateTime observedFrom, LocalDateTime observedTo) {
        if (observedFrom != null && observedTo != null && observedFrom.isAfter(observedTo)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "开始时间不能晚于结束时间", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<VersionFieldChangeView> buildObservationChanges(
            AisRecordManualVersionSnapshot before,
            AisRecordManualVersionSnapshot after
    ) {
        List<VersionFieldChangeView> changes = new ArrayList<>();
        addChange(changes, "ecosystemName", "生态系统", before == null ? null : before.ecosystemName(), after == null ? null : after.ecosystemName());
        addChange(changes, "observerName", "观测人员", before == null ? null : before.observerName(), after == null ? null : after.observerName());
        addChange(changes, "observedAt", "观测时间", formatDateTime(before == null ? null : before.observedAt()), formatDateTime(after == null ? null : after.observedAt()));
        addChange(changes, "locationName", "地点说明", before == null ? null : before.locationName(), after == null ? null : after.locationName());
        addChange(changes, "locationLat", "纬度", decimalToString(before == null ? null : before.locationLat()), decimalToString(after == null ? null : after.locationLat()));
        addChange(changes, "locationLng", "经度", decimalToString(before == null ? null : before.locationLng()), decimalToString(after == null ? null : after.locationLng()));
        addChange(changes, "envJson", "环境参数", summarizeEnvironment(before == null ? null : before.envJson()), summarizeEnvironment(after == null ? null : after.envJson()));
        addChange(changes, "note", "备注", before == null ? null : before.note(), after == null ? null : after.note());
        addChange(changes, "vesselItems", "关联船舶", summarizeVesselItems(before == null ? null : before.vesselItems()), summarizeVesselItems(after == null ? null : after.vesselItems()));
        return changes;
    }

    private void addChange(List<VersionFieldChangeView> changes, String fieldKey, String fieldLabel, String oldValue, String newValue) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        changes.add(new VersionFieldChangeView(fieldKey, fieldLabel, oldValue, newValue));
    }

    private String summarizeVesselItems(List<AisRecordManualVersionVesselSnapshot> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        return items.stream()
                .map(item -> {
                    StringBuilder builder = new StringBuilder();
                    builder.append(item.displayName() != null ? item.displayName() : item.profileName());
                    if (item.countEstimated() != null) {
                        builder.append(" × ").append(item.countEstimated());
                    }
                    if (item.behavior() != null && !item.behavior().isBlank()) {
                        builder.append("（").append(item.behavior()).append("）");
                    }
                    if (item.comment() != null && !item.comment().isBlank()) {
                        builder.append("：").append(item.comment());
                    }
                    return builder.toString();
                })
                .toList()
                .stream()
                .collect(java.util.stream.Collectors.joining("；"));
    }

    private String summarizeEnvironment(String envJson) {
        return normalizeNullable(envJson);
    }

    private String decimalToString(java.math.BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
