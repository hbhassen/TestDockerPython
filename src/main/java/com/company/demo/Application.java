package com.company.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Point d entree principal de l application Spring Boot generee.
 *
 * <p>Personnalisez cette classe seulement si vous devez changer la strategie
 * de demarrage, les frontieres de scan des composants ou les annotations
 * Spring Boot globales.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class Application {

    /**
     * Demarre l application generee.
     *
     * @param args arguments de ligne de commande JVM transmis a Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
