package com.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;

import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    // Valeur configurable depuis application.properties/yml
    @Value("${client.max-in-memory-size-mb:16}")
    private int maxInMemorySizeMb;

    /**
     * ObjectMapper centralisé, utilisé pour Jackson + WebClient
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }

    /**
     * WebClient configuré avec Jackson et taille mémoire personnalisée
     */
    @Bean
    public WebClient webClient(ObjectMapper objectMapper) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> {
                configurer.defaultCodecs().maxInMemorySize(maxInMemorySizeMb * 1024 * 1024);
                configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
                configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
            })
            .build();

        return WebClient.builder()
            .exchangeStrategies(strategies)
            .build();
    }

    /**
     * ExchangeFunction basé sur le WebClient custom
     */
    @Bean
    public ExchangeFunction exchangeFunction(WebClient webClient) {
        return request -> webClient
            .method(request.method())
            .uri(request.url())
            .headers(headers -> headers.addAll(request.headers()))
            .body(request.body())
            .exchangeToMono(Mono::just); // retourne ClientResponse
    }
}