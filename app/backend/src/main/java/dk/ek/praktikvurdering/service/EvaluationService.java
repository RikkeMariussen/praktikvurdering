package dk.ek.praktikvurdering.service;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@SuppressWarnings("unchecked")
public class EvaluationService {

    private static final Pattern SAFE_FILENAME = Pattern.compile("^[\\w\\-]+\\.md$");

    @Value("${app.data-dir}")
    private String dataDirStr;

    @Value("${app.prompts-dir}")
    private String promptsDirStr;

    @Value("${app.output-dir}")
    private String outputDirStr;

    private Path dataDir;
    private Path promptsDir;
    private Path outputDir;
    private RestClient restClient;
    private String model;

    @PostConstruct
    public void init() {
        this.dataDir    = Paths.get(dataDirStr).toAbsolutePath();
        this.promptsDir = Paths.get(promptsDirStr).toAbsolutePath();
        this.outputDir  = Paths.get(outputDirStr).toAbsolutePath();

        Dotenv dotenv = Dotenv.configure()
                .directory(Paths.get("").toAbsolutePath().toString())
                .ignoreIfMissing()
                .load();

        String apiKey = dotenv.get("OPENAI_API_KEY", System.getenv("OPENAI_API_KEY"));
        this.model = dotenv.get("OPENAI_MODEL", "gpt-4o");

        System.out.println("data-dir:    " + dataDir);
        System.out.println("prompts-dir: " + promptsDir);
        System.out.println("output-dir:  " + outputDir);

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public List<String> listReports() {
        try (Stream<Path> files = Files.list(dataDir)) {
            return files
                    .filter(f -> f.toString().endsWith(".md"))
                    .map(f -> f.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Kunne ikke læse data-mappe: " + dataDir);
        }
    }

    public Map<String, String> evaluate(String filename, String notes) {
        validateFilename(filename);

        Path reportPath = dataDir.resolve(filename);
        if (!Files.exists(reportPath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rapport ikke fundet");
        }

        try {
            String systemPrompt  = Files.readString(promptsDir.resolve("system-prompt.md"));
            String reportContent = Files.readString(reportPath);

            String stem           = filename.replace(".md", "");
            String outputFilename = stem + "-vurdering.md";

            StringBuilder userMessage = new StringBuilder("Vurdér følgende praktikrapport ud fra rubricen.\n\n");
            if (notes != null && !notes.isBlank()) {
                userMessage.append("**Noter til eksaminator:**\n").append(notes.strip()).append("\n\n");
            }
            userMessage.append("## Rapport\n\n").append(reportContent).append("\n\n");
            userMessage.append("Gem din vurdering i filen `output/").append(outputFilename).append("`.");

            Map<String, Object> request = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage.toString())
                    )
            );

            Map<?, ?> response = restClient.post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            List<Map<?, ?>> choices = (List<Map<?, ?>>) response.get("choices");
            Map<?, ?> message = (Map<?, ?>) choices.get(0).get("message");
            String result = (String) message.get("content");

            Files.createDirectories(outputDir);
            Files.writeString(outputDir.resolve(outputFilename), result);

            return Map.of("result", result, "output_file", outputFilename);

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fejl ved filhåndtering");
        }
    }

    public String getOutput(String filename) {
        validateFilename(filename);
        Path path = outputDir.resolve(filename);
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fil ikke fundet");
        }
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fejl ved fillæsning");
        }
    }

    private void validateFilename(String filename) {
        if (filename == null || !SAFE_FILENAME.matcher(filename).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ugyldigt filnavn");
        }
    }
}
