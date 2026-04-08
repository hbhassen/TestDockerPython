package com.company.demo.domaine.dto;

/**
 * DTO REST retourne par le endpoint de version d exemple.
 *
 * <p>Les records sont un bon choix par defaut pour les charges utiles sortantes
 * immuables. Ajoutez ici de nouveaux champs lorsque le contrat REST evolue.
 */
public record ApplicationVersionResponse(
        String name,
        String version,
        String description
) {
}
