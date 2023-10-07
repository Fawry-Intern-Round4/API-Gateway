package com.example.apigateway;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@Component
public class JwtAuthGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthGatewayFilterFactory.Config> {
    private final WebClient webClient;

    public JwtAuthGatewayFilterFactory(WebClient webClient) {
        super(Config.class);
        this.webClient = webClient;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            final String token = (Objects.isNull(AuthHeader) || !AuthHeader.startsWith("Bearer "))
                    ? "" : AuthHeader.substring(7);
            return webClient.get()
                    .uri("http://localhost:8080/token/validation")
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                    .exchangeToMono(clientResponse -> {
                        if (clientResponse.statusCode().isError()) {
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(error -> {
                                        exchange.getResponse().setStatusCode(clientResponse.statusCode());
                                        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                                        return exchange.getResponse().writeWith(
                                                Mono.just(exchange.getResponse().bufferFactory()
                                                        .wrap(error.getBytes())));
                                    });

                        }
                        return chain.filter(exchange);
                    });
        };

    }
    @Validated
    public static class Config {

    }
}