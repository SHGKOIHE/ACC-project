# 컴포넌트 스펙 (Component Spec)

**작성자**: ui (UI/UX 디자이너)
**작성일**: 2026-05-24
**대상**: 배달비 절약 음식 공동구매 앱 (React Native / Expo)
**위치**: `src/components/`

---

## 컴포넌트 목록 개요

| 카테고리 | 컴포넌트 | 사용 화면 |
|----------|----------|-----------|
| 방 탐색 | RoomCard | 홈(목록), 방 탐색 |
| 방 탐색 | RoomMapMarker | 홈(지도) |
| 방 상세 | ParticipantList | 방 상세 |
| 방 상세 | DeliveryFeePreview | 방 상세, 방 개설 |
| 방 상세 | RoomTypeBadge | 방 카드, 방 상세 |
| 주문 | OrderItemRow | 메뉴 선택, 정산 |
| 주문 | OrderSummaryFooter | 메뉴 선택 |
| 정산 | SettlementSummary | 정산 화면 |
| 정산 | AccountCopyBlock | 정산 화면 |
| 채팅 | ChatBubble | 채팅 화면 |
| 채팅 | SystemMessage | 채팅 화면 |
| 공통 | PrimaryButton | 전체 |
| 공통 | CountdownTimer | 방 카드, 방 상세 |
| 공통 | EmptyState | 홈(목록) |

---

## RoomCard

**역할**: 방 목록에서 개별 방을 카드 형태로 요약 표시

**위치**: `src/components/RoomCard.tsx`

**사용 화면**: 홈 목록 탭, 방 탐색 결과

```
+--------------------------------------+
| [RoomTypeBadge]    마감까지 00:23 ⏱ |
| 홍콩반점 치킨마요 덮밥               |
| 배달 주소: 기숙사 1동 앞             |
| 참여자: ●●●○○  3/5명               |
| 1인 배달비: 지금 1,667원 → 만석 1,000원 |
+--------------------------------------+
```

**Props**

| prop | 타입 | 필수 | 설명 |
|------|------|------|------|
| `roomId` | `string` | ✅ | 방 고유 ID |
| `title` | `string` | ✅ | 방 제목 (식당명 기반) |
| `restaurantName` | `string` | ✅ | 식당명 |
| `deliveryAddress` | `string` | ✅ | 배달 주소 |
| `roomType` | `'delivery' \| 'gather' \| 'dine-out'` | ✅ | 모임 유형 |
| `currentParticipants` | `number` | ✅ | 현재 참여자 수 |
| `maxParticipants` | `number` | ✅ | 최대 참여자 수 |
| `totalDeliveryFee` | `number` | ✅ | 총 배달비 (원) |
| `closingAt` | `Date` | ✅ | 마감 시간 |
| `onPress` | `() => void` | ✅ | 카드 탭 핸들러 |
| `style` | `ViewStyle` | ❌ | 외부 스타일 오버라이드 |

**파생 값 (내부 계산)**
- `currentDeliveryPerPerson` = `Math.ceil(totalDeliveryFee / currentParticipants)`
- `fullDeliveryPerPerson` = `Math.ceil(totalDeliveryFee / maxParticipants)`

---

## RoomMapMarker

**역할**: 지도 위에 방 위치를 유형별 아이콘으로 표시

**위치**: `src/components/RoomMapMarker.tsx`

**사용 화면**: 홈 지도 탭

**Props**

| prop | 타입 | 필수 | 설명 |
|------|------|------|------|
| `roomId` | `string` | ✅ | 방 고유 ID |
| `coordinate` | `{ latitude: number; longitude: number }` | ✅ | 지도 좌표 |
| `roomType` | `'delivery' \| 'gather' \| 'dine-out'` | ✅ | 모임 유형 |
| `isUrgent` | `boolean` | ❌ | 마감 30분 미만 시 강조 표시 |
| `onPress` | `(roomId: string) => void` | ✅ | 마커 탭 핸들러 |

**유형별 마커 디자인**

| 유형 | 레이블 | 색상 |
|------|--------|------|
| `delivery` | [D] | `#FF6B35` (주황) |
| `gather` | [G] | `#2D9E5A` (초록) |
| `dine-out` | [R] | `#3B82F6` (파랑) |

---

## ParticipantList

**역할**: 방의 참여자 아바타를 빈 슬롯과 함께 나열하여 현황 시각화

**위치**: `src/components/ParticipantList.tsx`

**사용 화면**: 방 상세 화면

```
참여자 (3/5)
● 민준이  ● 지은이  ● 승현이  ○ ○
```

**Props**

| prop | 타입 | 필수 | 설명 |
|------|------|------|------|
| `participants` | `Participant[]` | ✅ | 참여자 목록 |
| `maxParticipants` | `number` | ✅ | 최대 인원 |
| `showNicknames` | `boolean` | ❌ | 닉네임 표시 여부 (기본: false) |
| `size` | `'sm' \| 'md' \| 'lg'` | ❌ | 아바타 크기 (기본: md) |

**Participant 타입**

```typescript
type Participant = {
  userId: string;
  nickname: string;
  avatarColor: string; // 닉네임 기반 자동 생성 색상
  isHost: boolean;
};
```

---

## DeliveryFeePreview

**역할**: 총 배달비를 현재 인원/만석 인원 기준으로 1인당 배달비를 미리 보여줌

**위치**: `src/components/DeliveryFeePreview.tsx`

**사용 화면**: 방 상세, 방 개설 화면

```
배달비 정보
총 배달비: 5,000원
현재 1인당: 1,667원  →  만석 시: 1,000원
```

**Props**

| prop | 타입 | 필수 | 설명 |
|------|------|------|------|
| `totalDeliveryFee` | `number` | ✅ | 총 배달비 (원) |
| `currentParticipants` | `number` | ✅ | 현재 참여자 수 |
| `maxParticipants` | `number` | ✅ | 최대 참여자 수 |
| `highlightSaving` | `boolean` | ❌ | 절약 금액 강조 표시 (기본: false) |

---

## RoomTypeBadge

**역할**: 방의 모임 유형을 색상 뱃지로 표시

**위치**: `src/components/RoomTypeBadge.tsx`

**사용 화면**: RoomCard, 방 상세 화면

**Props**

| prop | 타입 | 필수 | 설명 |
|------|------|------|------|
| `type` | `'delivery' \| 'gather' \| 'dine-out'` | ✅ | 모임 유형 |
| `size` | `'sm' \| 'md'` | ❌ | 뱃지 크기 (기본: md) |

**표시 텍스트**

| type | 표시 | 색상 |
|------|------|------|
| `delivery` | 공동배달 | 주황 |
| `gather` | 배달+모임 | 초록 |
| `dine-out` | 직접외식 | 파랑 |

---

## OrderItemRow

**역할**: 주문 메뉴 1개 항목 표시 — 수량 조절(편집 모드) 또는 읽기 전용

**위치**: `src/components/OrderItemRow.tsx`

**사용 화면**: 메뉴 선택 화면 (편집), 정산 화면 (읽기 전용)

```
[편집 모드]
치킨마요 덮밥       8,500원    [-] 1 [+]

[읽기 전용]
치킨마요 덮밥 x1    8,500원
```

**Props**

| prop | 타입 | 필수 | 설명 |
|------|------|------|------|
| `itemId` | `string` | ✅ | 메뉴 항목 ID |
| `name` | `string` | ✅ | 메뉴명 |
| `price` | `number` | ✅ | 단가 (원) |
| `quantity` | `number` | ✅ | 수량 |
| `readonly` | `boolean` | ❌ | 읽기 전용 모드 (기본: false) |
| `onQuantityChange` | `(itemId: string, delta: number) => void` | ❌ | 수량 변경 핸들러 (편집 모드 필수) |

**편집 모드 제약**
- 수량 최솟값: 1 (0이 되면 항목 제거 확인 팝업)
- 수량 최댓값: 10

---

## OrderSummaryFooter

**역할**: 메뉴 선택 화면 하단에 고정되어 소계/배달비/합계 실시간 표시

**위치**: `src/components/OrderSummaryFooter.tsx`

**사용 화면**: 메뉴 선택 화면

```
소계:           9,000원
+ 1인 배달비:   1,000원
──────────────────────
예상 합계:     10,000원

[주문 완료]
```

**Props**

| prop | 타입 | 필수 | 설명 |
|------|------|------|------|
| `subtotal` | `number` | ✅ | 메뉴 소계 (원) |
| `deliveryFeePerPerson` | `number` | ✅ | 1인당 배달비 (원) |
| `onConfirm` | `() => void` | ✅ | 주문 완료 버튼 핸들러 |
| `isConfirmEnabled` | `boolean` | ✅ | 완료 버튼 활성화 여부 |

---

## SettlementSummary

**역할**: 정산 화면에서 전체 참여자의 송금 완료 현황 표시 (방장 전용 전체 보기)

**위치**: `src/components/SettlementSummary.tsx`

**사용 화면**: 정산 화면

```
정산 현황 (2/3 완료)
민준이 (방장)  ✅ 완료
지은이         ✅ 완료
승현이         ⏳ 대기
```

**Props**

| prop | 타입 | 필수 | 설명 |
|------|------|------|------|
| `settlements` | `Settlement[]` | ✅ | 참여자별 정산 상태 |
| `isHost` | `boolean` | ✅ | 방장 여부 (전체 현황 노출 제어) |

**Settlement 타입**

```typescript
type Settlement = {
  userId: string;
  nickname: string;
  amount: number;       // 해당 참여자가 낼 금액
  isPaid: boolean;      // 송금 완료 여부
  isHost: boolean;
};
```

---

## AccountCopyBlock

**역할**: 방장의 계좌 정보를 표시하고 클립보드 복사 기능 제공

**위치**: `src/components/AccountCopyBlock.tsx`

**사용 화면**: 정산 화면

```
송금 계좌 (방장)
카카오뱅크  1234-5678-9012  예금주: 김민준
[계좌번호 복사]
```

**Props**

| prop | 타입 | 필수 | 설명 |
|------|------|------|------|
| `bankName` | `string` | ✅ | 은행명 |
| `accountNumber` | `string` | ✅ | 계좌번호 |
| `accountHolder` | `string` | ✅ | 예금주 |

**동작**
- 복사 버튼 탭 → 클립보드 복사 + 토스트 메시지 "계좌번호가 복사됐어요"

---

## ChatBubble

**역할**: 채팅 메시지 1개를 말풍선 형태로 표시 — 본인/상대 방향 구분

**위치**: `src/components/ChatBubble.tsx`

**사용 화면**: 채팅 화면

**Props**

| prop | 타입 | 필수 | 설명 |
|------|------|------|------|
| `message` | `string` | ✅ | 메시지 본문 |
| `nickname` | `string` | ✅ | 발신자 닉네임 |
| `sentAt` | `Date` | ✅ | 전송 시간 |
| `isMine` | `boolean` | ✅ | 본인 메시지 여부 |
| `isHost` | `boolean` | ❌ | 방장 표시 (왕관 아이콘) |

**스타일 규칙**
- `isMine: true` → 우측 정렬, 브랜드 컬러 배경
- `isMine: false` → 좌측 정렬, 회색 배경 + 상단 닉네임 표시
- `isHost: true` → 닉네임 옆 왕관 아이콘(👑)

---

## SystemMessage

**역할**: 채팅 화면에서 시스템 자동 발송 메시지를 중앙 정렬로 표시

**위치**: `src/components/SystemMessage.tsx`

**사용 화면**: 채팅 화면

**Props**

| prop | 타입 | 필수 | 설명 |
|------|------|------|------|
| `message` | `string` | ✅ | 시스템 메시지 본문 |
| `sentAt` | `Date` | ✅ | 발생 시간 |

**스타일**: 중앙 정렬, 작은 폰트(12px), 회색 텍스트, 배경 없음

---

## PrimaryButton

**역할**: 앱 전반의 주요 액션에 사용하는 CTA 버튼

**위치**: `src/components/PrimaryButton.tsx`

**사용 화면**: 전체 화면

**Props**

| prop | 타입 | 필수 | 설명 |
|------|------|------|------|
| `label` | `string` | ✅ | 버튼 텍스트 |
| `onPress` | `() => void` | ✅ | 탭 핸들러 |
| `disabled` | `boolean` | ❌ | 비활성화 여부 (기본: false) |
| `loading` | `boolean` | ❌ | 로딩 스피너 표시 (기본: false) |
| `variant` | `'primary' \| 'secondary' \| 'danger'` | ❌ | 버튼 스타일 (기본: primary) |
| `style` | `ViewStyle` | ❌ | 외부 스타일 오버라이드 |

**스타일 규칙**

| variant | 배경 | 텍스트 |
|---------|------|--------|
| `primary` | 브랜드 컬러 | 흰색 |
| `secondary` | 흰색 | 브랜드 컬러 (테두리) |
| `danger` | 빨간색 | 흰색 |
| `disabled` | 회색 | 연회색 |

---

## CountdownTimer

**역할**: 방 마감까지 남은 시간을 실시간으로 카운트다운 표시

**위치**: `src/components/CountdownTimer.tsx`

**사용 화면**: RoomCard, 방 상세 화면

**Props**

| prop | 타입 | 필수 | 설명 |
|------|------|------|------|
| `closingAt` | `Date` | ✅ | 마감 시각 |
| `onExpire` | `() => void` | ❌ | 만료 시 콜백 |
| `urgentThresholdMinutes` | `number` | ❌ | 경고 임계값 분 (기본: 30) |

**표시 형식**

| 남은 시간 | 표시 | 색상 |
|-----------|------|------|
| 1시간 이상 | `01:23:45` | 기본 텍스트 |
| 30분 미만 | `00:23:41` | 빨간색 + 진동 애니메이션 |
| 만료 | `마감됨` | 회색 |

---

## EmptyState

**역할**: 방 목록이 비어 있을 때 안내 메시지와 액션 버튼 표시

**위치**: `src/components/EmptyState.tsx`

**사용 화면**: 홈 목록 탭

**Props**

| prop | 타입 | 필수 | 설명 |
|------|------|------|------|
| `title` | `string` | ✅ | 주 안내 문구 |
| `description` | `string` | ❌ | 보조 설명 |
| `actionLabel` | `string` | ❌ | 액션 버튼 텍스트 |
| `onAction` | `() => void` | ❌ | 액션 버튼 핸들러 |

**기본 사용 예시**
```
title="근처에 열린 방이 없어요"
description="직접 방을 만들어 배달비를 아껴보세요!"
actionLabel="방 개설하기"
```
