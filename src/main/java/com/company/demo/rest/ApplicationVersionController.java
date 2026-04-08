package com.company.demo.rest;

import com.company.demo.domaine.dto.ApplicationVersionResponse;
import com.company.demo.services.ApplicationInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controleur REST exposant le endpoint d exemple des metadonnees applicatives.
 *
 * <p>Utilisez ce controleur comme reference pour le decoupage attendu :
 * controller -> service -> repository -> database.
 */
@RestController
@RequestMapping("/api/application")
@Tag(name = "Application", description = "Endpoints des metadonnees applicatives")
public class ApplicationVersionController {

    private final ApplicationInfoService applicationInfoService;

    public ApplicationVersionController(ApplicationInfoService applicationInfoService) {
        this.applicationInfoService = applicationInfoService;
    }

    /**
     * Retourne les metadonnees de l application generee.
     *
     * @return charge utile des metadonnees applicatives
     */
    @GetMapping("/version")
    @Operation(summary = "Obtenir la version de l application", description = "Retourne les metadonnees de l application generee.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Metadonnees applicatives retournees avec succes",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApplicationVersionResponse.class)
                    )
            )
    })
    public ResponseEntity<ApplicationVersionResponse> getApplicationVersion() {
        return ResponseEntity.ok(applicationInfoService.getApplicationVersion());
    }
}
