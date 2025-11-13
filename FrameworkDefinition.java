package com.example.obsolescence.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

public class FrameworkDefinition {

    private String name;

    @JsonProperty("acronyme-organisation")
    private String acronymeOrganisation;

    @JsonProperty("minimum-version")
    private String minimumVersion;

    @JsonProperty("latest-version")
    private String latestVersion;

    private LocalDate publie;

    @JsonProperty("end-of-support-communaute")
    private LocalDate endOfSupportCommunaute;

    @JsonProperty("end-of-support-organisation")
    private LocalDate endOfSupportOrganisation;

    @JsonProperty("list-dependances")
    private List<String> listDependances;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAcronymeOrganisation() {
        return acronymeOrganisation;
    }

    public void setAcronymeOrganisation(String acronymeOrganisation) {
        this.acronymeOrganisation = acronymeOrganisation;
    }

    public String getMinimumVersion() {
        return minimumVersion;
    }

    public void setMinimumVersion(String minimumVersion) {
        this.minimumVersion = minimumVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(String latestVersion) {
        this.latestVersion = latestVersion;
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

    public List<String> getListDependances() {
        return listDependances;
    }

    public void setListDependances(List<String> listDependances) {
        this.listDependances = listDependances;
    }
}
