package com.example.obsolescence.config;

import com.example.obsolescence.model.CatalogRoot;
import com.example.obsolescence.model.LanguageDefinition;
import com.example.obsolescence.model.LanguageVersionDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ObsolescenceCatalog {

    private final CatalogRoot root;
    private final Map<String, Map<String, LanguageVersionDefinition>> byLangAndVersion;

    public ObsolescenceCatalog() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        var resource = new ClassPathResource("obsolescence-catalog.yml");
        this.root = mapper.readValue(resource.getInputStream(), CatalogRoot.class);

        this.byLangAndVersion = root.getLanguages().stream()
                .collect(Collectors.toMap(
                        LanguageDefinition::getCode,
                        lang -> lang.getVersions().stream()
                                .collect(Collectors.toMap(
                                        LanguageVersionDefinition::getVersion,
                                        v -> v
                                ))
                ));
    }

    public LanguageVersionDefinition findLanguageVersion(String langCode, String version) {
        Map<String, LanguageVersionDefinition> versions = byLangAndVersion.get(langCode);
        if (versions == null) {
            return null;
        }
        return versions.get(version);
    }

    public LocalDate today() {
        return LocalDate.now();
    }

    public CatalogRoot getRoot() {
        return root;
    }
}
