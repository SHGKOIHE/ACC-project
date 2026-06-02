#!/usr/bin/env bash
# k6 JSON 결과를 CloudWatch Custom Metrics로 푸시
#
# 사용법:
#   ./k6/push-to-cloudwatch.sh /tmp/k6_baseline_result.json baseline
#   ./k6/push-to-cloudwatch.sh /tmp/k6_spike_result.json    spike
#
# 필요: jq, aws cli
set -euo pipefail

RESULT_FILE="${1:-/tmp/k6_baseline_result.json}"
SCENARIO="${2:-baseline}"
NAMESPACE="k6/FoodGroup"
REGION="${AWS_REGION:-ap-northeast-2}"

if [ ! -f "$RESULT_FILE" ]; then
  echo "ERROR: 결과 파일 없음: $RESULT_FILE" >&2
  exit 1
fi

echo "=== k6 → CloudWatch 메트릭 푸시 ==="
echo "  파일: $RESULT_FILE"
echo "  시나리오: $SCENARIO"
echo "  네임스페이스: $NAMESPACE"
echo ""

# ── 핵심 지표 추출 (jq) ─────────────────────────────────
# k6 JSON Point 형식: {"type":"Point","metric":"http_req_duration","data":{"value":123.45,...}}

extract_percentile() {
  local metric="$1"
  local pct="$2"   # 예: 0.95
  jq -r --arg m "$metric" --argjson p "$pct" '
    select(.type=="Point" and .metric==$m) | .data.value
  ' "$RESULT_FILE" \
  | sort -n \
  | awk -v p="$pct" 'BEGIN{lines=0} {a[lines++]=$1} END{
      idx=int(lines*p);
      if(idx>=lines) idx=lines-1;
      print a[idx]
    }'
}

extract_avg() {
  local metric="$1"
  jq -r --arg m "$metric" '
    select(.type=="Point" and .metric==$m) | .data.value
  ' "$RESULT_FILE" | awk '{sum+=$1; count++} END{if(count>0) print sum/count; else print 0}'
}

extract_rate() {
  local metric="$1"
  jq -r --arg m "$metric" '
    select(.type=="Point" and .metric==$m) | .data.value
  ' "$RESULT_FILE" | awk '{sum+=$1; count++} END{if(count>0) print sum/count*100; else print 0}'
}

extract_count() {
  local metric="$1"
  jq -r --arg m "$metric" '
    select(.type=="Point" and .metric==$m) | .data.value
  ' "$RESULT_FILE" | awk '{sum+=$1} END{print sum+0}'
}

echo "지표 계산 중..."
P50=$(extract_percentile "http_req_duration" 0.5)
P90=$(extract_percentile "http_req_duration" 0.9)
P95=$(extract_percentile "http_req_duration" 0.95)
P99=$(extract_percentile "http_req_duration" 0.99)
AVG=$(extract_avg "http_req_duration")
ERR_RATE=$(extract_rate "http_req_failed")
TOTAL_REQS=$(extract_count "http_reqs")
REG_COUNT=$(extract_count "baseline_registers" 2>/dev/null || echo 0)

echo "  p50=${P50}ms  p90=${P90}ms  p95=${P95}ms  p99=${P99}ms"
echo "  avg=${AVG}ms  error_rate=${ERR_RATE}%  total=${TOTAL_REQS}"
echo ""

# ── CloudWatch 메트릭 푸시 ───────────────────────────────
push_metric() {
  local name="$1"
  local value="$2"
  local unit="${3:-Milliseconds}"

  aws cloudwatch put-metric-data \
    --namespace "$NAMESPACE" \
    --region "$REGION" \
    --metric-data "[{
      \"MetricName\": \"${name}\",
      \"Value\": ${value},
      \"Unit\": \"${unit}\",
      \"Dimensions\": [{\"Name\": \"Scenario\", \"Value\": \"${SCENARIO}\"}]
    }]" 2>/dev/null && echo "  ✓ ${name}=${value}${unit}" || echo "  ✗ ${name} 실패"
}

echo "CloudWatch에 푸시 중..."
push_metric "http_req_duration_p50" "$P50"
push_metric "http_req_duration_p90" "$P90"
push_metric "http_req_duration_p95" "$P95"
push_metric "http_req_duration_p99" "$P99"
push_metric "http_req_duration_avg" "$AVG"
push_metric "http_req_failed_rate"  "$ERR_RATE"  "Percent"
push_metric "http_reqs_total"       "$TOTAL_REQS" "Count"

echo ""
echo "완료! CloudWatch 콘솔에서 확인:"
echo "  https://ap-northeast-2.console.aws.amazon.com/cloudwatch/home?region=ap-northeast-2#metricsV2:namespace=${NAMESPACE}"
