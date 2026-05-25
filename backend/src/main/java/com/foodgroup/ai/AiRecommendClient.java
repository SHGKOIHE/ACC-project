package com.foodgroup.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
public class AiRecommendClient {

    private final WebClient webClient;
    private final String internalKey;

    public AiRecommendClient(
            WebClient.Builder builder,
            @Value("${ai.lambda.function-url}") String functionUrl,
            @Value("${ai.lambda.internal-key}") String internalKey) {
        this.webClient = builder.baseUrl(functionUrl).build();
        this.internalKey = internalKey;
    }

    public Mono<AiRecommendResponse> recommend(AiRecommendRequest request) {
        return webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Internal-Key", internalKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiRecommendResponse.class)
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(e -> {
                    log.warn("AI Lambda call failed: {}", e.getMessage());
                    return Mono.just(AiRecommendResponse.empty());
                });
    }
}
