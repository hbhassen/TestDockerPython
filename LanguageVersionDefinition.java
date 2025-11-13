package com.example.obsolescence.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

public class LanguageVersionDefinition {

    private String version;
    private String name;
    private LocalDate publie;

    @JsonProperty("end-of-support-communaute")
    private LocalDate endOfSupportCommunaute;

    @JsonProperty("end-of-support-organisation")
    private LocalDate endOfSupportOrganisation;

    private List<FrameworkDefinition> frameworks;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getPublie() {
        return publie;
    }

    public void setPublie(LocalDate publie) {
        this.publie = publie;
    }

    public LocalDate getEndOfSupportCommunaute() {
        return endOfSupportCommunaute;
    }

    public void setEndOfSupportCommunaute(LocalDate endOfSupportCommunaute) {
        this.endOfSupportCommunaute = endOfSupportCommunaute;
    }

    public LocalDate getEndOfSupportOrganisation() {
        return endOfSupportOrganisation;
    }

    public void setEndOfSupportOrganisation(LocalDate endOfSupportOrganisation) {
        this.endOfSupportOrganisation = endOfSupportOrganisation;
    }

    public List<FrameworkDefinition> getFrameworks() {
        return frameworks;
    }

    public void setFrameworks(List<FrameworkDefinition> frameworks) {
        this.frameworks = frameworks;
    }
}
