package com.gsmv.versioning.model;

import java.time.LocalDateTime;

public class EntityVersionRecord {

    private Long id;
    private String entityType;
    private Long entityId;
    private Integer versionNo;
    private String action;
    private String snapshotJson;
    private String diffJson;
    private Long changedBy;
    private Long rollbackSourceVersionId;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public String getDiffJson() {
        return diffJson;
    }

    public void setDiffJson(String diffJson) {
        this.diffJson = diffJson;
    }

    public Long getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Long changedBy) {
        this.changedBy = changedBy;
    }

    public Long getRollbackSourceVersionId() {
        return rollbackSourceVersionId;
    }

    public void setRollbackSourceVersionId(Long rollbackSourceVersionId) {
        this.rollbackSourceVersionId = rollbackSourceVersionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
