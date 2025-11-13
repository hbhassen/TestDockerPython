package com.example.obsolescence.domain;

import java.util.List;

public class ProjectInventory {

    private String projectId;
    private List<String> collectedEntries;

    public ProjectInventory() {
    }

    public ProjectInventory(String projectId, List<String> collectedEntries) {
        this.projectId = projectId;
        this.collectedEntries = collectedEntries;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public List<String> getCollectedEntries() {
        return collectedEntries;
    }

    public void setCollectedEntries(List<String> collectedEntries) {
        this.collectedEntries = collectedEntries;
    }
}
