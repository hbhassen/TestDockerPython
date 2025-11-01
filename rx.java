package com.example.gitlab.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Service
public class GitLabReactiveService {

    private final WebClient webClient;

    public GitLabReactiveService(
            @Value("${gitlab.url}") String gitlabUrl,
            @Value("${gitlab.token}") String token
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(gitlabUrl + "/api/v4")
                .defaultHeader("PRIVATE-TOKEN", token)
                .build();
    }

    public Flux<Object> getAllProjects() {
        return fetchPage(1)
                .expand(response -> {
                    Optional<String> nextPage = Optional.ofNullable(response.headers().header("X-Next-Page"))
                                                       .map(list -> list.isEmpty() ? null : list.get(0));

                    return nextPage.filter(s -> !s.isBlank())
                            .map(Integer::valueOf)
                            .map(this::fetchPage)
                            .orElse(Mono.empty());
                })
                .flatMap(resp -> Flux.fromIterable(resp.body()));
    }

    private Mono<ProjectPageResponse> fetchPage(int page) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/projects")
                        .queryParam("page", page)
                        .queryParam("per_page", 100)
                        .queryParam("simple", true) // plus rapide
                        .build()
                )
                .exchangeToMono(response -> {
                    HttpHeaders headers = response.headers().asHttpHeaders();

                    return response.bodyToFlux(Object.class)
                            .collectList()
                            .map(body -> new ProjectPageResponse(headers, body));
                });
    }

    private record ProjectPageResponse(HttpHeaders headers, List<Object> body) {}
}