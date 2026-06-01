package com.foodgroup.common.config;

import com.foodgroup.auth.domain.Member;
import com.foodgroup.auth.repository.MemberPort;
import com.foodgroup.chat.domain.ChatMessage;
import com.foodgroup.chat.domain.ChatMessageType;
import com.foodgroup.chat.repository.ChatMessagePort;
import com.foodgroup.order.domain.MemberSettlement;
import com.foodgroup.order.domain.OrderItem;
import com.foodgroup.order.domain.Settlement;
import com.foodgroup.order.repository.MemberSettlementPort;
import com.foodgroup.order.repository.OrderItemPort;
import com.foodgroup.order.repository.SettlementPort;
import com.foodgroup.room.domain.Room;
import com.foodgroup.room.domain.RoomParticipant;
import com.foodgroup.room.domain.RoomStatus;
import com.foodgroup.room.repository.RoomParticipantPort;
import com.foodgroup.room.repository.RoomPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@TestConfiguration
public class TestDynamoConfig {

    @Bean @Primary
    public MemberPort memberPort() {
        return new MemberPort() {
            private final Map<String, Member> byId = new ConcurrentHashMap<>();
            private final Map<String, Member> byToken = new ConcurrentHashMap<>();

            @Override public Member save(Member m) {
                byId.put(m.getId(), m);
                if (m.getDeviceToken() != null) byToken.put(m.getDeviceToken(), m);
                return m;
            }
            @Override public Optional<Member> findById(String id) { return Optional.ofNullable(byId.get(id)); }
            @Override public Optional<Member> findByDeviceToken(String token) { return Optional.ofNullable(byToken.get(token)); }
            @Override public boolean existsByNickname(String nickname) { return byId.values().stream().anyMatch(m -> nickname.equals(m.getNickname())); }
            @Override public void deleteById(String id) { Member m = byId.remove(id); if (m != null && m.getDeviceToken() != null) byToken.remove(m.getDeviceToken()); }
        };
    }

    @Bean @Primary
    public RoomPort roomPort() {
        return new RoomPort() {
            private final Map<String, Room> store = new ConcurrentHashMap<>();

            @Override public Room save(Room r) { store.put(r.getId(), r); return r; }
            @Override public Optional<Room> findById(String id) { return Optional.ofNullable(store.get(id)); }
            @Override public List<Room> scanByStatus(RoomStatus status) {
                return store.values().stream().filter(r -> status == r.getStatus()).toList();
            }
            @Override public List<Room> findOpenClosingBetween(LocalDateTime from, LocalDateTime to) {
                return store.values().stream()
                        .filter(r -> r.getStatus() == RoomStatus.OPEN && r.getClosedAt() != null
                                && !r.getClosedAt().isBefore(from) && !r.getClosedAt().isAfter(to))
                        .toList();
            }
            @Override public int closeExpiredRooms(LocalDateTime now) {
                List<Room> expired = store.values().stream()
                        .filter(r -> r.getStatus() == RoomStatus.OPEN && r.getClosedAt() != null && r.getClosedAt().isBefore(now))
                        .toList();
                expired.forEach(r -> { r.updateStatus(RoomStatus.CLOSED); store.put(r.getId(), r); });
                return expired.size();
            }
        };
    }

    @Bean @Primary
    public RoomParticipantPort roomParticipantPort() {
        return new RoomParticipantPort() {
            private final Map<String, RoomParticipant> store = new ConcurrentHashMap<>();

            private String key(String roomId, String memberId) { return roomId + "#" + memberId; }

            @Override public RoomParticipant save(RoomParticipant p) { store.put(key(p.getRoomId(), p.getMemberId()), p); return p; }
            @Override public boolean existsByRoomIdAndMemberId(String roomId, String memberId) { return store.containsKey(key(roomId, memberId)); }
            @Override public Optional<RoomParticipant> findByRoomIdAndMemberId(String roomId, String memberId) { return Optional.ofNullable(store.get(key(roomId, memberId))); }
            @Override public List<RoomParticipant> findByRoomId(String roomId) {
                return store.values().stream().filter(p -> roomId.equals(p.getRoomId())).toList();
            }
            @Override public List<RoomParticipant> findByMemberId(String memberId) {
                return store.values().stream().filter(p -> memberId.equals(p.getMemberId())).toList();
            }
            @Override public void delete(RoomParticipant p) { store.remove(key(p.getRoomId(), p.getMemberId())); }
        };
    }

    @Bean @Primary
    public OrderItemPort orderItemPort() {
        return new OrderItemPort() {
            private final Map<String, OrderItem> store = new ConcurrentHashMap<>();

            @Override public OrderItem save(OrderItem i) { store.put(i.getId(), i); return i; }
            @Override public Optional<OrderItem> findById(String id) { return Optional.ofNullable(store.get(id)); }
            @Override public List<OrderItem> findByRoomId(String roomId) { return store.values().stream().filter(i -> roomId.equals(i.getRoomId())).toList(); }
            @Override public List<OrderItem> findByRoomIdAndMemberId(String roomId, String memberId) {
                return store.values().stream().filter(i -> roomId.equals(i.getRoomId()) && memberId.equals(i.getMemberId())).toList();
            }
            @Override public boolean existsByRoomId(String roomId) { return store.values().stream().anyMatch(i -> roomId.equals(i.getRoomId())); }
            @Override public boolean existsByRoomIdAndMemberId(String roomId, String memberId) { return findByRoomIdAndMemberId(roomId, memberId).stream().findAny().isPresent(); }
            @Override public void deleteByRoomIdAndMemberId(String roomId, String memberId) {
                store.values().removeIf(i -> roomId.equals(i.getRoomId()) && memberId.equals(i.getMemberId()));
            }
            @Override public int sumAmountByRoomIdAndMemberId(String roomId, String memberId) {
                return findByRoomIdAndMemberId(roomId, memberId).stream().mapToInt(i -> i.getPrice() * i.getQuantity()).sum();
            }
            @Override public void delete(OrderItem item) { store.remove(item.getId()); }
        };
    }

    @Bean @Primary
    public SettlementPort settlementPort() {
        return new SettlementPort() {
            private final Map<String, Settlement> byRoomId = new ConcurrentHashMap<>();
            private final Map<String, Settlement> byId = new ConcurrentHashMap<>();

            @Override public Settlement save(Settlement s) { byRoomId.put(s.getRoomId(), s); byId.put(s.getId(), s); return s; }
            @Override public Optional<Settlement> findById(String id) { return Optional.ofNullable(byId.get(id)); }
            @Override public Optional<Settlement> findByRoomId(String roomId) { return Optional.ofNullable(byRoomId.get(roomId)); }
        };
    }

    @Bean @Primary
    public MemberSettlementPort memberSettlementPort() {
        return new MemberSettlementPort() {
            private final List<MemberSettlement> store = Collections.synchronizedList(new ArrayList<>());

            @Override public List<MemberSettlement> saveAll(List<MemberSettlement> settlements) { store.addAll(settlements); return settlements; }
            @Override public List<MemberSettlement> findBySettlementId(String settlementId) {
                return store.stream().filter(ms -> settlementId.equals(ms.getSettlementId())).toList();
            }
        };
    }

    @Bean @Primary
    public ChatMessagePort chatMessagePort() {
        return new ChatMessagePort() {
            private final List<ChatMessage> store = Collections.synchronizedList(new ArrayList<>());

            @Override
            public ChatMessage save(String roomId, String memberId, ChatMessageType type, String content) {
                ChatMessage msg = ChatMessage.builder()
                        .roomId(roomId).memberId(memberId).type(type).content(content)
                        .createdAt(LocalDateTime.now()).build();
                store.add(msg);
                return msg;
            }
            @Override
            public List<ChatMessage> findTop50ByRoomId(String roomId) {
                return store.stream().filter(m -> roomId.equals(m.getRoomId())).limit(50).toList();
            }
            @Override public void deleteOldMessages(LocalDateTime cutoff) {}
        };
    }
}
