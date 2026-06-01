package com.foodgroup.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiRecommendClientTest {

    @Mock
    private LambdaClient lambdaClient;

    private AiRecommendClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        client = new AiRecommendClient(lambdaClient, "test-fn", "test-internal-key");
    }

    private InvokeResponse mockResponse(Object responseBody) throws Exception {
        String bodyJson = objectMapper.writeValueAsString(responseBody);
        String lambdaPayload = objectMapper.writeValueAsString(Map.of(
                "statusCode", 200,
                "body", bodyJson
        ));
        return InvokeResponse.builder()
                .payload(SdkBytes.fromUtf8String(lambdaPayload))
                .build();
    }

    @Test
    void recommend_정상_응답_파싱() throws Exception {
        Map<String, Object> responseBody = Map.of(
                "recommendations", List.of(Map.of(
                        "rank", 1,
                        "restaurantName", "치킨집",
                        "matchScore", 80,
                        "reason", "많이 시킨 메뉴"
                )),
                "explanation", "치킨이 1위입니다."
        );
        when(lambdaClient.invoke(any(InvokeRequest.class))).thenReturn(mockResponse(responseBody));

        AiRecommendRequest request = new AiRecommendRequest(
                "room-1",
                List.of(new AiRecommendRequest.ParticipantInfo("짱구",
                        List.of(new AiRecommendRequest.OrderItemInfo("치킨", 15000)))),
                new AiRecommendRequest.FilterInfo(null, 3000, null, null, null)
        );

        AiRecommendResponse result = client.recommend(request);

        assertThat(result).isNotNull();
        assertThat(result.recommendations()).hasSize(1);
        assertThat(result.recommendations().get(0).restaurantName()).isEqualTo("치킨집");
        assertThat(result.explanation()).isEqualTo("치킨이 1위입니다.");
    }

    @Test
    void recommend_X_Internal_Key_헤더_페이로드_포함_확인() throws Exception {
        when(lambdaClient.invoke(any(InvokeRequest.class)))
                .thenReturn(mockResponse(Map.of("recommendations", List.of(), "explanation", "")));

        client.recommend(new AiRecommendRequest("room-1", List.of(),
                new AiRecommendRequest.FilterInfo(null, null, null, null, null)));

        var captor = org.mockito.ArgumentCaptor.forClass(InvokeRequest.class);
        verify(lambdaClient).invoke(captor.capture());

        String sentPayload = captor.getValue().payload().asUtf8String();
        assertThat(sentPayload).contains("\"x-internal-key\"");
        assertThat(sentPayload).contains("test-internal-key");
    }

    @Test
    void recommend_Lambda_오류_시_빈_응답_반환() throws Exception {
        String errorPayload = objectMapper.writeValueAsString(Map.of("statusCode", 500, "body", "{}"));
        when(lambdaClient.invoke(any(InvokeRequest.class)))
                .thenReturn(InvokeResponse.builder()
                        .payload(SdkBytes.fromUtf8String(errorPayload))
                        .build());

        AiRecommendResponse result = client.recommend(new AiRecommendRequest("room-1", List.of(),
                new AiRecommendRequest.FilterInfo(null, null, null, null, null)));

        assertThat(result.recommendations()).isEmpty();
        assertThat(result.explanation()).isEmpty();
    }

    @Test
    void recommend_예외_발생_시_빈_응답_반환() {
        when(lambdaClient.invoke(any(InvokeRequest.class))).thenThrow(new RuntimeException("connection error"));

        AiRecommendResponse result = client.recommend(new AiRecommendRequest("room-1", List.of(),
                new AiRecommendRequest.FilterInfo(null, null, null, null, null)));

        assertThat(result.recommendations()).isEmpty();
    }
}
