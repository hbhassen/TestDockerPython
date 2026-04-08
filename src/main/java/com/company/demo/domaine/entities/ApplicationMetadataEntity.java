package com.company.demo.domaine.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entite JPA stockant les metadonnees applicatives exposees par le endpoint d exemple.
 *
 * <p>Le projet genere persiste une seule ligne dans cette table. Vous pouvez
 * garder cette entite comme exemple de reference simple ou la remplacer par
 * vos propres entites de domaine quand le service implementera de vrais cas metier.
 */
@Entity
@Table(name = "application_metadata")
public class ApplicationMetadataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(nullable = false, length = 255)
    private String description;

    /**
     * Constructeur protege sans argument requis par JPA.
     */
    protected ApplicationMetadataEntity() {
    }

    /**
     * Cree une nouvelle ligne de metadonnees applicatives.
     *
     * @param name nom d affichage de l application
     * @param version version de l application exposee par l API
     * @param description description fonctionnelle exposee aux clients
     */
    public ApplicationMetadataEntity(String name, String version, String description) {
        this.name = name;
        this.version = version;
        this.description = description;
    }

    /**
     * @return identifiant technique genere
     */
    public Long getId() {
        return id;
    }

    /**
     * @return nom d affichage de l application
     */
    public String getName() {
        return name;
    }

    /**
     * @return version de l application
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return description fonctionnelle exposee par l API
     */
    public String getDescription() {
        return description;
    }
}
