package com.example.obsolescence.domain;

import java.util.List;

public class ProjectObsolescenceReport {

    private String projectId;
    private LanguageInfo languageInfo;
    private ObsolescenceStatus languageStatus;
    private List<FrameworkUsageReport> frameworks;
    private ObsolescenceStatus overallStatus;
    private String explanation;

    public ProjectObsolescenceReport() {
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public LanguageInfo getLanguageInfo() {
        return languageInfo;
    }

    public void setLanguageInfo(LanguageInfo languageInfo) {
        this.languageInfo = languageInfo;
    }

    public ObsolescenceStatus getLanguageStatus() {
        return languageStatus;
    }

    public void setLanguageStatus(ObsolescenceStatus languageStatus) {
        this.languageStatus = languageStatus;
    }

    public List<FrameworkUsageReport> getFrameworks() {
        return frameworks;
    }

    public void setFrameworks(List<FrameworkUsageReport> frameworks) {
        this.frameworks = frameworks;
    }

    public ObsolescenceStatus getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(ObsolescenceStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
