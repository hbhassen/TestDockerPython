import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.BodyExtractors;

Mono<SpFile> spFileMono = exchangeFunction.exchange(request)
        .flatMap(response -> {
            if (response.statusCode().is2xxSuccessful()) {
                // ✅ Méthode fiable : BodyExtractor explicite
                return response.body(BodyExtractors.toMono(new ParameterizedTypeReference<SpFile>() {}));
            } else {
                return response.bodyToMono(String.class)
                        .flatMap(errorBody -> Mono.error(new RuntimeException("Erreur HTTP : " + errorBody)));
            }
        });

SpFile result = spFileMono.block();
System.out.println("✅ Fichier reçu : " + result);


ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(config -> {
            config.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder());
            config.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder());
            config.defaultCodecs().maxInMemorySize(16 * 1024 * 1024);
        })
        .build();

WebClient client = WebClient.builder()
        .exchangeStrategies(strategies)
        .build();

ExchangeFunction exchangeFunction = client.exchangeFunction();


exchangeFunction.exchange(request)
    .flatMap(response -> {
        // 📌 Récupération des headers
        HttpHeaders headers = response.headers().asHttpHeaders();

        // 🔍 Affichage en console
        headers.forEach((key, values) -> 
            System.out.println("🔸 " + key + ": " + String.join(", ", values))
        );

        // Ensuite, traitement normal
        return response.bodyToMono(User.class);
    });


