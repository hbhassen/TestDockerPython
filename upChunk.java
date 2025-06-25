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



package com.example.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.io.File;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    // 1️⃣ OAuth2 Client Manager
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientRepository clientRepository
    ) {
        return new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                registrations,
                new InMemoryOAuth2AuthorizedClientService(registrations)
        );
    }

    // 2️⃣ WebClient avec SSL, proxy et filtre OAuth2
    @Bean
    public WebClient secureWebClient(OAuth2AuthorizedClientManager authorizedClientManager) throws Exception {
        // 📁 Chemins des certificats (modifie si nécessaire)
        File clientCert = new File("certs/client-cert.pem");
        File privateKey = new File("certs/client-key.pem");
        File caCert = new File("certs/ca-cert.pem");

        // 🔐 SSL context (client cert + CA)
        SslContext sslContext = SslContextBuilder.forClient()
                .keyManager(clientCert, privateKey)
                .trustManager(caCert)
                .build();

        // 🌐 Proxy configuration (IP, port, auth facultative)
        String proxyHost = "192.168.100.10";
        int proxyPort = 3128;
        String proxyUser = "proxyUser";
        String proxyPass = "proxyPass";

        TcpClient tcpClient = TcpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .proxy(proxy -> proxy
                        .type(ProxyHandler.ProxyType.HTTP)
                        .address(new InetSocketAddress(proxyHost, proxyPort))
                        .username(proxyUser)
                        .password(s -> proxyPass)
                )
                .secure(ssl -> ssl.sslContext(sslContext))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(600, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(600, TimeUnit.SECONDS))
                );

        HttpClient httpClient = HttpClient.from(tcpClient)
                .responseTimeout(Duration.ofMinutes(10));

        // 🛡️ OAuth2 filter
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Filter =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .apply(oauth2Filter.oauth2Configuration())
                .build();
    }

    // 3️⃣ ExchangeFunction exposé pour les services bas niveau
    @Bean
    public ExchangeFunction exchangeFunction(WebClient secureWebClient) {
        return secureWebClient.mutate().build().exchangeFunction();
    }
}

