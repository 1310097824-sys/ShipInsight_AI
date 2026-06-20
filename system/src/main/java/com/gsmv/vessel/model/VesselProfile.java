package com.gsmv.vessel.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VesselProfile {
    private Long id;
    private String vesselName;
    private String mmsi;
    private String imo;
    private String callSign;
    private Long vesselTypeId;
    private String flagState;
    private String operatorName;
    private String ownerName;
    private BigDecimal lengthM;
    private BigDecimal widthM;
    private BigDecimal draftM;
    private BigDecimal grossTonnage;
    private BigDecimal deadweightTonnage;
    private String riskLevel;
    private String navigationStatus;
    private String homePort;
    private String usualRegion;
    private String routeArea;
    private String note;
    private String sourceText;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVesselName() {
        return vesselName;
    }

    public void setVesselName(String vesselName) {
        this.vesselName = vesselName;
    }

    public String getMmsi() {
        return mmsi;
    }

    public void setMmsi(String mmsi) {
        this.mmsi = mmsi;
    }

    public String getImo() {
        return imo;
    }

    public void setImo(String imo) {
        this.imo = imo;
    }

    public String getCallSign() {
        return callSign;
    }

    public void setCallSign(String callSign) {
        this.callSign = callSign;
    }

    public Long getVesselTypeId() {
        return vesselTypeId;
    }

    public void setVesselTypeId(Long vesselTypeId) {
        this.vesselTypeId = vesselTypeId;
    }

    public String getFlagState() {
        return flagState;
    }

    public void setFlagState(String flagState) {
        this.flagState = flagState;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public BigDecimal getLengthM() {
        return lengthM;
    }

    public void setLengthM(BigDecimal lengthM) {
        this.lengthM = lengthM;
    }

    public BigDecimal getWidthM() {
        return widthM;
    }

    public void setWidthM(BigDecimal widthM) {
        this.widthM = widthM;
    }

    public BigDecimal getDraftM() {
        return draftM;
    }

    public void setDraftM(BigDecimal draftM) {
        this.draftM = draftM;
    }

    public BigDecimal getGrossTonnage() {
        return grossTonnage;
    }

    public void setGrossTonnage(BigDecimal grossTonnage) {
        this.grossTonnage = grossTonnage;
    }

    public BigDecimal getDeadweightTonnage() {
        return deadweightTonnage;
    }

    public void setDeadweightTonnage(BigDecimal deadweightTonnage) {
        this.deadweightTonnage = deadweightTonnage;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getNavigationStatus() {
        return navigationStatus;
    }

    public void setNavigationStatus(String navigationStatus) {
        this.navigationStatus = navigationStatus;
    }

    public String getHomePort() {
        return homePort;
    }

    public void setHomePort(String homePort) {
        this.homePort = homePort;
    }

    public String getUsualRegion() {
        return usualRegion;
    }

    public void setUsualRegion(String usualRegion) {
        this.usualRegion = usualRegion;
    }

    public String getRouteArea() {
        return routeArea;
    }

    public void setRouteArea(String routeArea) {
        this.routeArea = routeArea;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
