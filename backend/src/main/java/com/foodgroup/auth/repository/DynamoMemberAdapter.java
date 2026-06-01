package com.foodgroup.auth.repository;

import com.foodgroup.auth.domain.DynamoMember;
import com.foodgroup.auth.domain.Gender;
import com.foodgroup.auth.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "aws.dynamodb.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DynamoMemberAdapter implements MemberPort {

    private static final String TABLE_NAME = "Members";

    private final DynamoDbEnhancedClient enhancedClient;

    private DynamoDbTable<DynamoMember> table() {
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(DynamoMember.class));
    }

    @Override
    public Member save(Member member) {
        DynamoMember item = toItem(member);
        table().putItem(item);
        return member;
    }

    @Override
    public Optional<Member> findById(String id) {
        DynamoMember item = table().getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(item).map(this::toDomain);
    }

    @Override
    public Optional<Member> findByDeviceToken(String deviceToken) {
        DynamoDbIndex<DynamoMember> gsi = table().index("deviceToken-index");
        QueryEnhancedRequest req = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(deviceToken).build()))
                .limit(1)
                .build();
        return gsi.query(req).stream()
                .flatMap(p -> p.items().stream())
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        Expression filter = Expression.builder()
                .expression("nickname = :nickname")
                .expressionValues(Map.of(":nickname", AttributeValue.builder().s(nickname).build()))
                .build();
        ScanEnhancedRequest req = ScanEnhancedRequest.builder()
                .filterExpression(filter)
                .limit(1)
                .build();
        return table().scan(req).items().stream().findAny().isPresent();
    }

    @Override
    public void deleteById(String id) {
        table().deleteItem(Key.builder().partitionValue(id).build());
    }

    private DynamoMember toItem(Member m) {
        DynamoMember item = new DynamoMember();
        item.setId(m.getId());
        item.setNickname(m.getNickname());
        item.setDeviceToken(m.getDeviceToken());
        item.setFcmToken(m.getFcmToken());
        item.setGender(m.getGender() != null ? m.getGender().name() : null);
        item.setAddress(m.getAddress());
        item.setEmail(m.getEmail());
        item.setEmailVerified(m.isEmailVerified() ? Boolean.TRUE : null);
        item.setCreatedAt(m.getCreatedAt() != null ? m.getCreatedAt().toString() : LocalDateTime.now().toString());
        return item;
    }

    private Member toDomain(DynamoMember item) {
        return Member.builder()
                .id(item.getId())
                .nickname(item.getNickname())
                .deviceToken(item.getDeviceToken())
                .fcmToken(item.getFcmToken())
                .gender(item.getGender() != null ? Gender.valueOf(item.getGender()) : null)
                .address(item.getAddress())
                .email(item.getEmail())
                .emailVerified(Boolean.TRUE.equals(item.getEmailVerified()))
                .createdAt(item.getCreatedAt() != null ? LocalDateTime.parse(item.getCreatedAt()) : null)
                .build();
    }
}
