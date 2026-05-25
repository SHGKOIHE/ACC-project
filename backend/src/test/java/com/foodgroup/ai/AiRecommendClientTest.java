package com.foodgroup.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class AiRecommendClientTest {

    private MockWebServer mockServer;
    private AiRecommendClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();

        String baseUrl = mockServer.url("/").toString();
        client = new AiRecommendClient(
                WebClient.builder(),
                baseUrl,
                "test-internal-key"
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    @Test
    void recommend_정상_응답_파싱() throws Exception {
        AiRecommendResponse expected = new AiRecommendResponse(
                List.of(new AiRecommendResponse.RecommendationItem(1, "치킨집", 80, "")),
                "치킨이 1위입니다."
        );
        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(expected))
        );

        AiRecommendRequest request = new AiRecommendRequest(
                1L,
                List.of(new AiRecommendRequest.ParticipantInfo("짱구",
                        List.of(new AiRecommendRequest.OrderItemInfo("치킨", 15000)))),
                new AiRecommendRequest.FilterInfo(null, 3000)
        );

        AiRecommendResponse result = client.recommend(request).block();

        assertThat(result).isNotNull();
        assertThat(result.recommendations()).hasSize(1);
        assertThat(result.recommendations().get(0).restaurantName()).isEqualTo("치킨집");
        assertThat(result.explanation()).isEqualTo("치킨이 1위입니다.");
    }

    @Test
    void recommend_X_Internal_Key_헤더_전송_확인() throws Exception {
        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(AiRecommendResponse.empty()))
        );

        client.recommend(new AiRecommendRequest(1L, List.of(),
                new AiRecommendRequest.FilterInfo(null, null))).block();

        RecordedRequest recorded = mockServer.takeRequest();
        assertThat(recorded.getHeader("X-Internal-Key")).isEqualTo("test-internal-key");
    }

    @Test
    void recommend_Lambda_오류_시_fallback_빈_응답_반환() {
        mockServer.enqueue(new MockResponse().setResponseCode(500));

        AiRecommendRequest request = new AiRecommendRequest(1L, List.of(),
                new AiRecommendRequest.FilterInfo(null, null));

        AiRecommendResponse result = client.recommend(request).block();

        assertThat(result).isNotNull();
        assertThat(result.recommendations()).isEmpty();
        assertThat(result.explanation()).isEmpty();
    }

    @Test
    void recommend_연결_실패_시_fallback_빈_응답_반환() throws IOException {
        mockServer.shutdown();

        AiRecommendRequest request = new AiRecommendRequest(1L, List.of(),
                new AiRecommendRequest.FilterInfo(null, null));

        AiRecommendResponse result = client.recommend(request).block();

        assertThat(result).isNotNull();
        assertThat(result.recommendations()).isEmpty();
    }
}
