package com.company.demo;

import com.company.demo.config.ApplicationMetadataProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifie le contrat REST public expose par le endpoint d exemple genere.
 *
 * <p>Ce test utilise la datasource de test generee pour pouvoir s executer
 * sans base externe, meme quand la cible d execution principale est PostgreSQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApplicationVersionControllerTest {

    private final MockMvc mockMvc;
    private final ApplicationMetadataProperties applicationMetadataProperties;

    @Autowired
    ApplicationVersionControllerTest(
            MockMvc mockMvc,
            ApplicationMetadataProperties applicationMetadataProperties
    ) {
        this.mockMvc = mockMvc;
        this.applicationMetadataProperties = applicationMetadataProperties;
    }

    /**
     * Verifie que le endpoint de version retourne bien la charge utile de metadonnees configuree.
     *
     * @throws Exception si MockMvc n arrive pas a executer la requete
     */
    @Test
    void shouldReturnApplicationVersion() throws Exception {
        mockMvc.perform(get("/api/application/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(applicationMetadataProperties.name()))
                .andExpect(jsonPath("$.version").value(applicationMetadataProperties.version()))
                .andExpect(jsonPath("$.description").value(applicationMetadataProperties.description()));
    }
}
