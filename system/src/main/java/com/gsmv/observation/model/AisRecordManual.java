package com.gsmv.observation.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AisRecordManual {

    private Long id;
    private Long ecosystemId;
    private Long observerUserId;
    private LocalDateTime observedAt;
    private BigDecimal locationLat;
    private BigDecimal locationLng;
    private String locationName;
    private String envJson;
    private String note;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEcosystemId() {
        return ecosystemId;
    }

    public void setEcosystemId(Long ecosystemId) {
        this.ecosystemId = ecosystemId;
    }

    public Long getObserverUserId() {
        return observerUserId;
    }

    public void setObserverUserId(Long observerUserId) {
        this.observerUserId = observerUserId;
    }

    public LocalDateTime getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(LocalDateTime observedAt) {
        this.observedAt = observedAt;
    }

    public BigDecimal getLocationLat() {
        return locationLat;
    }

    public void setLocationLat(BigDecimal locationLat) {
        this.locationLat = locationLat;
    }

    public BigDecimal getLocationLng() {
        return locationLng;
    }

    public void setLocationLng(BigDecimal locationLng) {
        this.locationLng = locationLng;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getEnvJson() {
        return envJson;
    }

    public void setEnvJson(String envJson) {
        this.envJson = envJson;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
