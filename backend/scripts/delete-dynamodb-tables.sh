#!/bin/bash
# Deletes all DynamoDB tables. Use with caution — data is permanently lost.
# Usage: ./scripts/delete-dynamodb-tables.sh [--region ap-northeast-2] [--confirm]
set -e

REGION="${AWS_REGION:-ap-northeast-2}"
CONFIRMED=false
while [[ $# -gt 0 ]]; do
  case $1 in
    --region) REGION="$2"; shift 2 ;;
    --confirm) CONFIRMED=true; shift ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

TABLES="EmailVerificationCodes Members Rooms RoomParticipants OrderItems Settlements MemberSettlements ChatMessages"

if [ "$CONFIRMED" != "true" ]; then
  echo "This will permanently delete the following tables in $REGION:"
  for t in $TABLES; do echo "  - $t"; done
  echo ""
  read -rp "Type 'yes' to confirm: " answer
  [ "$answer" = "yes" ] || { echo "Aborted."; exit 1; }
fi

for table in $TABLES; do
  echo "Deleting $table..."
  aws dynamodb delete-table --region "$REGION" --table-name "$table" \
    --output text --query 'TableDescription.TableName' \
    && echo "  -> $table deleted" \
    || echo "  -> $table not found or error (skipping)"
done

echo ""
echo "Done."
