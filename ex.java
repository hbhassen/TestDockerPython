@Bean
public ExchangeFunction exchangeFunction(WebClient secureWebClient) {
    return clientRequest -> secureWebClient
            .method(clientRequest.method())
            .uri(clientRequest.url())
            .headers(httpHeaders -> httpHeaders.addAll(clientRequest.headers()))
            .body(clientRequest.body())
            .exchangeToMono(response -> Mono.just(response));
}