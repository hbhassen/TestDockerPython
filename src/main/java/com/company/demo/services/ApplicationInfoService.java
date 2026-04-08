package com.company.demo.services;

import com.company.demo.domaine.dto.ApplicationVersionResponse;

/**
 * Contrat de service utilise par la couche REST.
 *
 * <p>Gardez ici la logique d orchestration lorsque le endpoint commencera a
 * agreger plusieurs repositories, appels distants ou regles metier.
 */
public interface ApplicationInfoService {

    /**
     * Retourne les metadonnees exposees par le endpoint de version d exemple.
     *
     * @return DTO retourne aux clients de l API
     */
    ApplicationVersionResponse getApplicationVersion();
}
