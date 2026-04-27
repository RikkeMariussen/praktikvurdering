package dk.ek.praktikvurdering.controller;

import dk.ek.praktikvurdering.service.EvaluationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class EvaluationController {

    private final EvaluationService service;

    public EvaluationController(EvaluationService service) {
        this.service = service;
    }

    @GetMapping("/reports")
    public Map<String, List<String>> listReports() {
        return Map.of("reports", service.listReports());
    }

    @PostMapping("/evaluate")
    public Map<String, String> evaluate(@RequestBody Map<String, String> body) {
        String filename = body.get("filename");
        String notes = body.getOrDefault("notes", "");
        return service.evaluate(filename, notes);
    }

    @GetMapping("/output/{filename}")
    public Map<String, String> getOutput(@PathVariable String filename) {
        return Map.of("content", service.getOutput(filename));
    }
}
