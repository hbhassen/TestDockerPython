import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

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

    public FileMetadata uploadFileStreamed(String targetUrl, byte[] fileContent) {
        Flux<DataBuffer> dataBufferFlux = Flux.generate(() -> new AtomicInteger(0), (state, sink) -> {
            int index = state.get();
            int start = index * CHUNK_SIZE;
            if (start >= fileContent.length) {
                sink.complete();
            } else {
                int end = Math.min(start + CHUNK_SIZE, fileContent.length);
                byte[] chunk = new byte[end - start];
                System.arraycopy(fileContent, start, chunk, 0, chunk.length);

                DataBuffer buffer = new DefaultDataBufferFactory().wrap(ByteBuffer.wrap(chunk));
                sink.next(buffer);

                state.incrementAndGet();
            }
            return state;
        });

        return webClient.post()
                .uri(targetUrl)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.TRANSFER_ENCODING, "chunked")
                .body(BodyInserters.fromDataBuffers(dataBufferFlux))
                .retrieve()
                .bodyToMono(FileMetadata.class)
                .block(Duration.ofMinutes(10));
    }
}