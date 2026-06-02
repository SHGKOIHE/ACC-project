package com.foodgroup.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

@Configuration
@ConditionalOnProperty(name = "aws.dynamodb.enabled", havingValue = "true")
public class DynamoDbConfig {

    @Value("${aws.region:ap-northeast-2}")
    private String region;

    @Value("${aws.dynamodb.endpoint:}")
    private String endpoint;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
