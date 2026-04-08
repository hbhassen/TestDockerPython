package com.company.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Centralise la definition OpenAPI exposee par springdoc.
 *
 * <p>Etendez cette configuration lorsque vous devez declarer les contacts,
 * les informations de licence, les serveurs, les schemas de securite ou une
 * documentation API groupee.
 */
@Configuration
public class OpenApiConfig {

    private final ApplicationMetadataProperties applicationMetadataProperties;

    public OpenApiConfig(ApplicationMetadataProperties applicationMetadataProperties) {
        this.applicationMetadataProperties = applicationMetadataProperties;
    }

    /**
     * Construit l objet OpenAPI affiche par Swagger UI.
     *
     * @return la description OpenAPI utilisee par springdoc
     */
    @Bean
    public OpenAPI enterpriseOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title(applicationMetadataProperties.name())
                        .version(applicationMetadataProperties.version())
                        .description(applicationMetadataProperties.description()));
    }
}
