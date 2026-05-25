#!/bin/bash
# pg_dump → S3 자동 백업

set -euo pipefail

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="/tmp/foodgroup_${TIMESTAMP}.sql"
S3_PATH="s3://${S3_BACKUP_BUCKET}/postgres/${TIMESTAMP}.sql"

echo "[backup] 시작: ${TIMESTAMP}"

PGPASSWORD="${POSTGRES_PASSWORD}" pg_dump \
  -h "${POSTGRES_HOST:-localhost}" \
  -p "${POSTGRES_PORT:-5432}" \
  -U "${POSTGRES_USER}" \
  -d "${POSTGRES_DB}" \
  -f "${BACKUP_FILE}"

aws s3 cp "${BACKUP_FILE}" "${S3_PATH}" \
  --region "${AWS_REGION:-ap-northeast-2}"

rm -f "${BACKUP_FILE}"
echo "[backup] 완료: ${S3_PATH}"
