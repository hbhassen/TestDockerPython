import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.time.Duration;

public MyResponse uploadFileForceChunked(InputStream inputStream) {

    Flux<DataBuffer> buffer = DataBufferUtils.readInputStream(
            () -> inputStream,
            new DefaultDataBufferFactory(),
            8192
    );

    // ⚠️ Construire manuellement la requête sans Content-Length
    ClientRequest request = ClientRequest
            .method(HttpMethod.POST, URI.create("https://myserver/api/v1/file"))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .body(BodyInserters.fromDataBuffers(buffer));

    // ⚙️ Accès direct à l’ExchangeFunction (bas-niveau)
    ExchangeFunction exchange = WebClient.builder()
            .exchangeStrategies(ExchangeStrategies.withDefaults())
            .build()
            .mutate()
            .build()
            .exchangeFunction();

    return exchange.exchange(request)
            .flatMap(response -> response.bodyToMono(MyResponse.class))
            .block(Duration.ofMinutes(5));
}

webClient = webClient.mutate()
    .filter((request, next) -> {
        System.out.println("Headers envoyés :");
        request.headers().forEach((k, v) -> System.out.println(k + ": " + v));
        return next.exchange(request);
    })
    .build();

