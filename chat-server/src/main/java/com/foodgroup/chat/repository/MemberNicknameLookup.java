package com.foodgroup.chat.repository;

import com.foodgroup.chat.domain.DynamoMember;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "aws.dynamodb.enabled", havingValue = "true")
@RequiredArgsConstructor
public class MemberNicknameLookup {

    private static final String TABLE_NAME = "Members";

    private final DynamoDbEnhancedClient enhancedClient;

    private DynamoDbTable<DynamoMember> table() {
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(DynamoMember.class));
    }

    public Optional<String> findNickname(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            return Optional.empty();
        }
        try {
            DynamoMember item = table().getItem(Key.builder().partitionValue(memberId).build());
            return Optional.ofNullable(item).map(DynamoMember::getNickname);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
