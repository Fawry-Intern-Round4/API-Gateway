package com.example.apigateway;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

@Component
public class JwtAuthGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthGatewayFilterFactory.Config> {
    private final WebClient.Builder webClient;

    public JwtAuthGatewayFilterFactory(WebClient.Builder webClient) {
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
            String token;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            } else {
                token = "";
            }
            return webClient.build().get()
                    .uri("lb://user-api/token/validation")
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                    .exchangeToMono(clientResponse -> {
                        if (clientResponse.statusCode().equals(HttpStatus.OK)) {
                            return chain.filter(exchange);
                        }
                        Function<String, Mono<Void>> errorFun = error -> {
                            ServerHttpResponse response = exchange.getResponse();
                            response.setStatusCode(clientResponse.statusCode());
                            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                            byte[] errorBytes = error.getBytes();
                            return response.writeWith(Mono.just(response.bufferFactory().wrap(errorBytes)));
                        };
                        return clientResponse.bodyToMono(String.class).flatMap(errorFun);
                    });
        };

    }

    @Validated
    public static class Config {

    }
}