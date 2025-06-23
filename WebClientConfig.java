import io.netty.channel.ChannelOption;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import javax.net.ssl.SSLException;
import java.io.File;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager) throws SSLException {

        // === 1. SSL OpenSSL : cert client (.pem), clé privée, CA serveur ===
        File clientCert = new File("certs/client-cert.pem");
        File privateKey = new File("certs/client-key.pem");
        File caCert = new File("certs/ca-cert.pem");

        SslContext sslContext = SslContextBuilder.forClient()
                .keyManager(clientCert, privateKey) // Certificat client
                .trustManager(caCert)               // CA de confiance
                .build();

        // === 2. Configuration du proxy HTTP ===
        String proxyHost = "192.168.100.10";
        int proxyPort = 3128;
        String proxyUser = "proxyUser"; // facultatif
        String proxyPass = "proxyPass"; // facultatif

        TcpClient tcpClient = TcpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .proxy(proxy -> proxy
                        .type(ProxyHandler.ProxyType.HTTP)
                        .address(new InetSocketAddress(proxyHost, proxyPort))
                        .username(proxyUser)
                        .password(s -> proxyPass)
                )
                .secure(sslSpec -> sslSpec.sslContext(sslContext)) // SSL avec OpenSSL
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(600, TimeUnit.SECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(600, TimeUnit.SECONDS))
                );

        HttpClient httpClient = HttpClient.from(tcpClient)
                .responseTimeout(Duration.ofMinutes(10));

        // === 3. OAuth2 support via filtre WebClient ===
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

        // === 4. Construction finale du WebClient ===
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(config -> config.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                .apply(oauth2.oauth2Configuration())
                .build();
    }
}