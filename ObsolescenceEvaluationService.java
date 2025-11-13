package com.example.obsolescence.service;

import com.example.obsolescence.config.ObsolescenceCatalog;
import com.example.obsolescence.domain.*;
import com.example.obsolescence.model.FrameworkDefinition;
import com.example.obsolescence.model.LanguageVersionDefinition;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ObsolescenceEvaluationService {

    private final ObsolescenceCatalog catalog;
    private final InventoryParser parser;

    public ObsolescenceEvaluationService(ObsolescenceCatalog catalog,
                                         InventoryParser parser) {
        this.catalog = catalog;
        this.parser = parser;
    }

    public ProjectObsolescenceReport evaluate(ProjectInventory inventory) {
        ProjectObsolescenceReport report = new ProjectObsolescenceReport();
        report.setProjectId(inventory.getProjectId());

        LanguageInfo languageInfo = parser.detectLanguage(inventory);
        report.setLanguageInfo(languageInfo);

        if (languageInfo == null || languageInfo.version() == null) {
            report.setLanguageStatus(ObsolescenceStatus.INCONNU);
            report.setOverallStatus(ObsolescenceStatus.INCONNU);
            report.setExplanation("Langage non détecté dans l’inventaire.");
            return report;
        }

        LanguageVersionDefinition langDef =
                catalog.findLanguageVersion(languageInfo.languageCode(),
                                            languageInfo.version());

        if (langDef == null) {
            report.setLanguageStatus(ObsolescenceStatus.OBSOLETE);
            report.setOverallStatus(ObsolescenceStatus.OBSOLETE);
            report.setExplanation("Version de langage non définie dans le catalogue (obsolète par politique).");
            return report;
        }

        LocalDate today = catalog.today();

        ObsolescenceStatus langStatus = computeStatus(
                today,
                langDef.getEndOfSupportCommunaute(),
                langDef.getEndOfSupportOrganisation()
        );
        report.setLanguageStatus(langStatus);

        List<String> entries = inventory.getCollectedEntries();
        List<DependencyCoordinate> coordinates = entries.stream()
                .map(DependencyCoordinate::parse)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        List<FrameworkUsageReport> fwReports = new ArrayList<>();
        for (FrameworkDefinition fw : langDef.getFrameworks()) {
            boolean used = coordinates.stream()
                    .anyMatch(dep -> fw.getListDependances().contains(dep.ga()));

            if (!used) {
                continue;
            }

            String detectedVersion = coordinates.stream()
                    .filter(dep -> fw.getListDependances().contains(dep.ga()))
                    .map(DependencyCoordinate::version)
                    .max(Comparator.naturalOrder())
                    .orElse(null);

            FrameworkUsageReport fwReport = new FrameworkUsageReport();
            fwReport.setName(fw.getName());
            fwReport.setDetectedVersion(detectedVersion);

            ObsolescenceStatus fwStatus = computeStatus(
                    today,
                    fw.getEndOfSupportCommunaute(),
                    fw.getEndOfSupportOrganisation()
            );
            fwReport.setStatus(fwStatus);

            String reason = "Framework " + fw.getName()
                    + " version détectée " + detectedVersion
                    + " (support communauté jusqu’au " + fw.getEndOfSupportCommunaute()
                    + ", organisation jusqu’au " + fw.getEndOfSupportOrganisation() + ")";

            fwReport.setReason(reason);
            fwReports.add(fwReport);
        }

        report.setFrameworks(fwReports);

        ObsolescenceStatus overall;
        if (!fwReports.isEmpty()) {
            if (fwReports.stream().anyMatch(f -> f.getStatus() == ObsolescenceStatus.OBSOLETE)) {
                overall = ObsolescenceStatus.OBSOLETE;
            } else if (fwReports.stream().anyMatch(f -> f.getStatus() == ObsolescenceStatus.EN_MIGRATION)) {
                overall = ObsolescenceStatus.EN_MIGRATION;
            } else {
                overall = langStatus;
            }
            report.setExplanation("Statut déterminé à partir du langage et des frameworks utilisés.");
        } else {
            overall = langStatus;
            report.setExplanation("Aucun framework connu détecté, statut basé sur le langage uniquement.");
        }

        report.setOverallStatus(overall);
        return report;
    }

    private ObsolescenceStatus computeStatus(LocalDate today,
                                             LocalDate communityEnd,
                                             LocalDate orgEnd) {
        if (today.isAfter(orgEnd)) {
            return ObsolescenceStatus.OBSOLETE;
        }
        if (today.isAfter(communityEnd) && !today.isAfter(orgEnd)) {
            return ObsolescenceStatus.EN_MIGRATION;
        }
        return ObsolescenceStatus.SUPPORTE;
    }
}
