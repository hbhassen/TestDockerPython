package com.company.demo.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Lie les metadonnees metier exposees par le endpoint REST d exemple.
 *
 * <p>Personnalisez ces valeurs dans {@code application.yml} pour qu elles
 * correspondent a l identite du service genere visible dans la reponse API
 * et dans la definition OpenAPI.
 */
@Validated
@ConfigurationProperties(prefix = "application.metadata")
public record ApplicationMetadataProperties(
        @NotBlank String name,
        @NotBlank String version,
        @NotBlank String description
) {
}
