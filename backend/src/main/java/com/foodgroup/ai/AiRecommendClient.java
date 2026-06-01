package com.foodgroup.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AiRecommendClient {

    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String functionName;
    private final String internalKey;

    @org.springframework.beans.factory.annotation.Autowired
    public AiRecommendClient(
            @Value("${ai.lambda.function-name:food-recommend-api}") String functionName,
            @Value("${ai.lambda.internal-key:}") String internalKey) {
        this.lambdaClient = LambdaClient.builder()
                .region(Region.AP_NORTHEAST_2)
                .build();
        this.functionName = functionName;
        this.internalKey = internalKey;
    }

    // package-private for testing
    AiRecommendClient(LambdaClient lambdaClient, String functionName, String internalKey) {
        this.lambdaClient = lambdaClient;
        this.functionName = functionName;
        this.internalKey = internalKey;
    }

    public AiRecommendResponse recommend(AiRecommendRequest request) {
        try {
            // Wrap in API Gateway event format that the Lambda function expects
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("headers", Map.of("x-internal-key", internalKey));
            event.put("body", objectMapper.writeValueAsString(request));

            String payload = objectMapper.writeValueAsString(event);
            InvokeResponse response = lambdaClient.invoke(InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromUtf8String(payload))
                    .build());

            String raw = response.payload().asUtf8String();
            JsonNode root = objectMapper.readTree(raw);

            JsonNode bodyNode = root.get("body");
            if (bodyNode == null) {
                return AiRecommendResponse.empty();
            }

            JsonNode body = objectMapper.readTree(bodyNode.asText());
            JsonNode recsNode = body.get("recommendations");
            String explanation = body.path("explanation").asText("");

            if (recsNode == null || !recsNode.isArray() || recsNode.isEmpty()) {
                return AiRecommendResponse.empty();
            }

            List<AiRecommendResponse.RecommendationItem> items = new ArrayList<>();
            for (JsonNode item : recsNode) {
                int score = item.has("matchScore") ? item.path("matchScore").asInt(0) : item.path("score").asInt(0);
                items.add(new AiRecommendResponse.RecommendationItem(
                        item.path("rank").asInt(1),
                        item.path("restaurantName").asText(""),
                        score,
                        item.path("reason").asText("")
                ));
            }
            return new AiRecommendResponse(items, explanation);
        } catch (Exception e) {
            log.warn("AI Lambda call failed: {}", e.getMessage());
            return AiRecommendResponse.empty();
        }
    }
}
