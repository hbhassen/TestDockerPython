@Bean
public WebClient secureWebClient(OAuth2AuthorizedClientManager authorizedClientManager) throws Exception {
    // ... (SSL + proxy comme déjà configuré)

    ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Filter =
            new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

    return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .apply(oauth2Filter.oauth2Configuration())
            .filter((request, next) -> {
                System.out.println("====== Headers envoyés ======");
                System.out.println("Méthode : " + request.method() + ", URI : " + request.url());
                request.headers().forEach((key, values) ->
                        values.forEach(value -> System.out.println(key + ": " + value))
                );
                System.out.println("=============================");
                return next.exchange(request);
            })
            .build();
}