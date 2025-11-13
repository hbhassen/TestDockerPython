package com.example.obsolescence.model;

import java.util.List;

public class LanguageDefinition {

    private String code; // java, python, node
    private List<LanguageVersionDefinition> versions;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<LanguageVersionDefinition> getVersions() {
        return versions;
    }

    public void setVersions(List<LanguageVersionDefinition> versions) {
        this.versions = versions;
    }
}
