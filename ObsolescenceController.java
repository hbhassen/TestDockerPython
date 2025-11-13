package com.example.obsolescence.web;

import com.example.obsolescence.domain.ProjectInventory;
import com.example.obsolescence.domain.ProjectObsolescenceReport;
import com.example.obsolescence.service.ObsolescenceEvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/obsolescence")
public class ObsolescenceController {

    private final ObsolescenceEvaluationService service;

    public ObsolescenceController(ObsolescenceEvaluationService service) {
        this.service = service;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<ProjectObsolescenceReport> evaluate(
            @RequestBody ProjectInventory inventory) {

        ProjectObsolescenceReport report = service.evaluate(inventory);
        return ResponseEntity.ok(report);
    }
}
