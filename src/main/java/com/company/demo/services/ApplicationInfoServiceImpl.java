package com.company.demo.services;

import com.company.demo.domaine.dto.ApplicationVersionResponse;
import com.company.demo.domaine.entities.ApplicationMetadataEntity;
import com.company.demo.repositories.ApplicationMetadataRepository;
import org.springframework.stereotype.Service;

/**
 * Implementation de service par defaut derriere l API REST d exemple.
 *
 * <p>Gardez les responsabilites HTTP hors de cette classe. Son role est de recuperer
 * les donnees de domaine depuis les repositories et de les transformer en DTO sortants.
 */
@Service
public class ApplicationInfoServiceImpl implements ApplicationInfoService {

    private final ApplicationMetadataRepository applicationMetadataRepository;

    public ApplicationInfoServiceImpl(ApplicationMetadataRepository applicationMetadataRepository) {
        this.applicationMetadataRepository = applicationMetadataRepository;
    }

    /**
     * Recupere la ligne de metadonnees courante et la mappe vers le DTO REST.
     *
     * @return metadonnees applicatives retournees par le endpoint
     */
    @Override
    public ApplicationVersionResponse getApplicationVersion() {
        ApplicationMetadataEntity metadata = applicationMetadataRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException(
                        "Les metadonnees applicatives sont absentes. Verifiez l initialisation JPA."));

        return new ApplicationVersionResponse(
                metadata.getName(),
                metadata.getVersion(),
                metadata.getDescription()
        );
    }
}
