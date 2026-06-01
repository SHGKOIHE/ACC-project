#!/bin/bash
# Creates all DynamoDB tables for the foodgroup backend.
# Usage: ./scripts/create-dynamodb-tables.sh [--region ap-northeast-2]
set -e

REGION="${AWS_REGION:-ap-northeast-2}"
while [[ $# -gt 0 ]]; do
  case $1 in
    --region) REGION="$2"; shift 2 ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

COMMON_ARGS="--region $REGION --billing-mode PAY_PER_REQUEST"

echo "Creating DynamoDB tables in region: $REGION"
echo ""

# ─── EmailVerificationCodes ──────────────────────────────────────────────────
echo "[1/8] Creating EmailVerificationCodes table..."
aws dynamodb create-table $COMMON_ARGS \
  --table-name EmailVerificationCodes \
  --attribute-definitions \
    AttributeName=email,AttributeType=S \
  --key-schema \
    AttributeName=email,KeyType=HASH \
  --output text --query 'TableDescription.TableName' \
  && echo "  -> EmailVerificationCodes created" \
  || echo "  -> EmailVerificationCodes already exists or error (continuing)"

# Enable TTL on EmailVerificationCodes
aws dynamodb update-time-to-live \
  --region "$REGION" \
  --table-name EmailVerificationCodes \
  --time-to-live-specification "Enabled=true,AttributeName=ttl" \
  --output text --query 'TimeToLiveSpecification.TimeToLiveStatus' \
  && echo "  -> TTL enabled" \
  || echo "  -> TTL enable failed (may already be enabled)"

# ─── Members ────────────────────────────────────────────────────────────────
echo "[2/8] Creating Members table..."
aws dynamodb create-table $COMMON_ARGS \
  --table-name Members \
  --attribute-definitions \
    AttributeName=id,AttributeType=S \
    AttributeName=deviceToken,AttributeType=S \
  --key-schema \
    AttributeName=id,KeyType=HASH \
  --global-secondary-indexes '[
    {
      "IndexName": "deviceToken-index",
      "KeySchema": [{"AttributeName": "deviceToken", "KeyType": "HASH"}],
      "Projection": {"ProjectionType": "ALL"}
    }
  ]' \
  --output text --query 'TableDescription.TableName' \
  && echo "  -> Members created" \
  || echo "  -> Members already exists or error (continuing)"

# ─── Rooms ───────────────────────────────────────────────────────────────────
echo "[3/8] Creating Rooms table..."
aws dynamodb create-table $COMMON_ARGS \
  --table-name Rooms \
  --attribute-definitions \
    AttributeName=id,AttributeType=S \
  --key-schema \
    AttributeName=id,KeyType=HASH \
  --output text --query 'TableDescription.TableName' \
  && echo "  -> Rooms created" \
  || echo "  -> Rooms already exists or error (continuing)"

# ─── RoomParticipants ────────────────────────────────────────────────────────
# PK is composite id = roomId#memberId (matches DynamoRoomParticipantAdapter)
echo "[4/8] Creating RoomParticipants table..."
aws dynamodb create-table $COMMON_ARGS \
  --table-name RoomParticipants \
  --attribute-definitions \
    AttributeName=id,AttributeType=S \
    AttributeName=roomId,AttributeType=S \
  --key-schema \
    AttributeName=id,KeyType=HASH \
  --global-secondary-indexes '[
    {
      "IndexName": "roomId-index",
      "KeySchema": [{"AttributeName": "roomId", "KeyType": "HASH"}],
      "Projection": {"ProjectionType": "ALL"}
    }
  ]' \
  --output text --query 'TableDescription.TableName' \
  && echo "  -> RoomParticipants created" \
  || echo "  -> RoomParticipants already exists or error (continuing)"

# ─── OrderItems ──────────────────────────────────────────────────────────────
echo "[5/8] Creating OrderItems table..."
aws dynamodb create-table $COMMON_ARGS \
  --table-name OrderItems \
  --attribute-definitions \
    AttributeName=id,AttributeType=S \
    AttributeName=roomId,AttributeType=S \
  --key-schema \
    AttributeName=id,KeyType=HASH \
  --global-secondary-indexes '[
    {
      "IndexName": "roomId-index",
      "KeySchema": [{"AttributeName": "roomId", "KeyType": "HASH"}],
      "Projection": {"ProjectionType": "ALL"}
    }
  ]' \
  --output text --query 'TableDescription.TableName' \
  && echo "  -> OrderItems created" \
  || echo "  -> OrderItems already exists or error (continuing)"

# ─── Settlements ─────────────────────────────────────────────────────────────
echo "[6/8] Creating Settlements table..."
aws dynamodb create-table $COMMON_ARGS \
  --table-name Settlements \
  --attribute-definitions \
    AttributeName=roomId,AttributeType=S \
  --key-schema \
    AttributeName=roomId,KeyType=HASH \
  --output text --query 'TableDescription.TableName' \
  && echo "  -> Settlements created" \
  || echo "  -> Settlements already exists or error (continuing)"

# ─── MemberSettlements ───────────────────────────────────────────────────────
echo "[7/8] Creating MemberSettlements table..."
aws dynamodb create-table $COMMON_ARGS \
  --table-name MemberSettlements \
  --attribute-definitions \
    AttributeName=settlementId,AttributeType=S \
    AttributeName=memberId,AttributeType=S \
  --key-schema \
    AttributeName=settlementId,KeyType=HASH \
    AttributeName=memberId,KeyType=RANGE \
  --output text --query 'TableDescription.TableName' \
  && echo "  -> MemberSettlements created" \
  || echo "  -> MemberSettlements already exists or error (continuing)"

# ─── ChatMessages ────────────────────────────────────────────────────────────
echo "[8/8] Creating ChatMessages table..."
aws dynamodb create-table $COMMON_ARGS \
  --table-name ChatMessages \
  --attribute-definitions \
    AttributeName=roomId,AttributeType=S \
    AttributeName=createdAtId,AttributeType=S \
  --key-schema \
    AttributeName=roomId,KeyType=HASH \
    AttributeName=createdAtId,KeyType=RANGE \
  --output text --query 'TableDescription.TableName' \
  && echo "  -> ChatMessages created" \
  || echo "  -> ChatMessages already exists or error (continuing)"

# Enable TTL on ChatMessages
echo ""
echo "Enabling TTL on ChatMessages (expiresAt)..."
aws dynamodb update-time-to-live \
  --region "$REGION" \
  --table-name ChatMessages \
  --time-to-live-specification "Enabled=true,AttributeName=expiresAt" \
  --output text --query 'TimeToLiveSpecification.TimeToLiveStatus' \
  && echo "  -> TTL enabled" \
  || echo "  -> TTL enable failed (may already be enabled)"

echo ""
echo "Waiting for all tables to become ACTIVE..."
for table in EmailVerificationCodes Members Rooms RoomParticipants OrderItems Settlements MemberSettlements ChatMessages; do
  aws dynamodb wait table-exists --region "$REGION" --table-name "$table" \
    && echo "  ✓ $table is ACTIVE" \
    || echo "  ✗ $table wait failed"
done

echo ""
echo "All tables ready."
