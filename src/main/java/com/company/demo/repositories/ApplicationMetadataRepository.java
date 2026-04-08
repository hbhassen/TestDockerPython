package com.company.demo.repositories;

import com.company.demo.domaine.entities.ApplicationMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository Spring Data JPA utilise directement par la couche service generee.
 *
 * <p>Gardez ici les methodes de requete specifiques au framework. Quand
 * l application grandit, ajoutez dans cette interface les requetes derivees
 * ou JPQL avant de complexifier l architecture de persistance.
 */
public interface ApplicationMetadataRepository extends JpaRepository<ApplicationMetadataEntity, Long> {

    /**
     * Retourne la premiere ligne de metadonnees stockee en base.
     *
     * @return premiere ligne de metadonnees, si presente
     */
    Optional<ApplicationMetadataEntity> findFirstByOrderByIdAsc();
}
