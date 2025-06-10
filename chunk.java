import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@Service
public class ChunkedFileUploadService {

    private final WebClient webClient;
    private static final int CHUNK_SIZE = 10 * 1024 * 1024; // 10 Mo

    public ChunkedFileUploadService() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
                .maxConnections(50)
                .pendingAcquireMaxCount(1000)
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .responseTimeout(Duration.ofMinutes(10));

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public void uploadLargeFileInChunks(String targetUrl, File file) throws IOException {
        long totalSize = file.length();
        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            int chunkNumber = 1;
            long bytesUploaded = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] chunkData = (bytesRead == CHUNK_SIZE) ? buffer : copyChunk(buffer, bytesRead);

                InputStreamResource chunkResource = new InputStreamResource(new java.io.ByteArrayInputStream(chunkData)) {
                    @Override
                    public long contentLength() {
                        return bytesRead;
                    }
                };

                System.out.println("Uploading chunk " + chunkNumber + " (" + bytesRead + " bytes)");

                String response = webClient.post()
                        .uri(targetUrl)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData("file", chunkResource)
                                .with("chunkNumber", String.valueOf(chunkNumber))
                                .with("totalChunks", String.valueOf((totalSize + CHUNK_SIZE - 1) / CHUNK_SIZE)))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(Duration.ofMinutes(5));

                System.out.println("Chunk " + chunkNumber + " uploaded. Server response: " + response);

                chunkNumber++;
                bytesUploaded += bytesRead;
            }
        }
    }

    private byte[] copyChunk(byte[] buffer, int length) {
        byte[] chunk = new byte[length];
        System.arraycopy(buffer, 0, chunk, 0, length);
        return chunk;
    }
}