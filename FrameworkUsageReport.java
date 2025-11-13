package com.example.obsolescence.domain;

public class FrameworkUsageReport {

    private String name;
    private String detectedVersion;
    private ObsolescenceStatus status;
    private String reason;

    public FrameworkUsageReport() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDetectedVersion() {
        return detectedVersion;
    }

    public void setDetectedVersion(String detectedVersion) {
        this.detectedVersion = detectedVersion;
    }

    public ObsolescenceStatus getStatus() {
        return status;
    }

    public void setStatus(ObsolescenceStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
