package com.company.demo.config;

import com.company.demo.domaine.entities.ApplicationMetadataEntity;
import com.company.demo.repositories.ApplicationMetadataRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Alimente la ligne de metadonnees d exemple utilisee par le endpoint REST genere.
 *
 * <p>Cet initialiseur garde l application generee utilisable des le demarrage.
 * Remplacez-le par votre propre strategie d initialisation de donnees de
 * reference quand les entites metier deviennent plus complexes.
 */
@Configuration
public class ApplicationMetadataInitializer {

    /**
     * Insere une ligne de metadonnees par defaut au premier demarrage de l application.
     *
     * @param repository repository JPA utilise pour lire et persister la ligne de metadonnees
     * @param metadataProperties valeurs de metadonnees configurees dans application.yml
     * @return application runner execute apres le demarrage du contexte Spring
     */
    @Bean
    public ApplicationRunner applicationMetadataDataInitializer(
            ApplicationMetadataRepository repository,
            ApplicationMetadataProperties metadataProperties
    ) {
        return arguments -> repository.findFirstByOrderByIdAsc()
                .orElseGet(() -> repository.save(new ApplicationMetadataEntity(
                        metadataProperties.name(),
                        metadataProperties.version(),
                        metadataProperties.description()
                )));
    }
}
