import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import java.time.Duration;

@Service
public class FileUploadService {

    private final WebClient webClient;

    public FileUploadService() {
        // Configurer un ConnectionProvider pour gérer un grand nombre de connexions et éviter les timeouts
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
                .maxConnections(50)
                .pendingAcquireMaxCount(1000)
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .responseTimeout(Duration.ofMinutes(10));  // Timeout généreux pour les gros fichiers

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public String uploadLargeFile(String targetUrl, ByteArrayResource fileResource) {
        // IMPORTANT : Nom du champ form-data (ici "file")
        return webClient.post()
                .uri(targetUrl)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", fileResource))
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofMinutes(15));  // Bloque jusqu'à 15 min (à ajuster selon le contexte)
    }
}