package com.company.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test de fumee de base qui verifie que le contexte Spring genere demarre.
 *
 * <p>Gardez ce test leger. Il ne doit echouer que si la configuration,
 * le cablage des dependances ou la logique de demarrage est casse.
 */
@SpringBootTest
class ApplicationContextTest {

    /**
     * Verifie que le contexte applicatif se charge correctement.
     */
    @Test
    void contextLoads() {
    }
}
