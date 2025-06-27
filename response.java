return exchangeFunction.exchange(request)
        .timeout(Duration.ofMinutes(5))
        .flatMap(response -> {
            // 📦 Log des infos de la réponse
            System.out.println("📡 Statut reçu : " + response.statusCode());

            System.out.println("📬 Headers reçus :");
            response.headers().asHttpHeaders()
                    .forEach((k, v) -> v.forEach(val -> System.out.println(k + ": " + val)));

            return response.bodyToMono(String.class)
                    .doOnNext(body -> System.out.println("📄 Corps de la réponse :\n" + body))
                    .flatMap(body -> {
                        // ✅ Désérialiser le JSON si 2xx
                        if (response.statusCode().is2xxSuccessful()) {
                            return Mono.just(MftFile.fromJson(body)); // tu dois créer fromJson
                        } else {
                            return Mono.error(new RuntimeException("Erreur HTTP: " + response.statusCode()));
                        }
                    });
        })
        .block();