package com.foodgroup.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

import java.time.Instant;
import java.util.Optional;

@Repository
@Primary
@ConditionalOnProperty(name = "aws.dynamodb.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DynamoEmailVerificationAdapter implements EmailVerificationPort {

    private static final String TABLE_NAME = "EmailVerificationCodes";
    private static final int TTL_SECONDS = 300;

    private final DynamoDbEnhancedClient enhancedClient;

    private DynamoDbTable<DynamoEmailVerificationItem> table() {
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(DynamoEmailVerificationItem.class));
    }

    @Override
    public void saveCode(String email, String code) {
        DynamoEmailVerificationItem item = new DynamoEmailVerificationItem();
        item.setEmail(email);
        item.setCode(code);
        item.setTtl(Instant.now().getEpochSecond() + TTL_SECONDS);
        table().putItem(item);
    }

    @Override
    public Optional<String> findCode(String email) {
        DynamoEmailVerificationItem item = table().getItem(Key.builder().partitionValue(email).build());
        if (item == null) return Optional.empty();
        if (item.getTtl() != null && item.getTtl() < Instant.now().getEpochSecond()) {
            return Optional.empty();
        }
        return Optional.ofNullable(item.getCode());
    }

    @Override
    public void deleteCode(String email) {
        table().deleteItem(Key.builder().partitionValue(email).build());
    }
}
