# k6 성능 테스트 제출용 실행 절차

## 0. 전제

- 대상 API: `https://40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com`
- 백엔드 Lambda: `foodgroup-backend`
- 리전: `ap-northeast-2`
- 제출 산출물: k6 콘솔 결과, `reports/k6/*summary.json`, Grafana/CloudWatch 캡처

> 주의: Provisioned Concurrency는 `$LATEST`가 아니라 **버전 또는 alias**에 설정해야 한다. API Gateway도 해당 alias ARN을 호출해야 PC가 적용된다.

## 1. Provisioned Concurrency 50 설정

```bash
export AWS_REGION=ap-northeast-2
export BACKEND_FUNCTION=foodgroup-backend
export BACKEND_ALIAS=prod
export PC=50

VERSION=$(
  aws lambda publish-version \
    --function-name "$BACKEND_FUNCTION" \
    --region "$AWS_REGION" \
    --query Version \
    --output text
)

aws lambda update-alias \
  --function-name "$BACKEND_FUNCTION" \
  --name "$BACKEND_ALIAS" \
  --function-version "$VERSION" \
  --region "$AWS_REGION" \
|| aws lambda create-alias \
  --function-name "$BACKEND_FUNCTION" \
  --name "$BACKEND_ALIAS" \
  --function-version "$VERSION" \
  --region "$AWS_REGION"

aws lambda put-provisioned-concurrency-config \
  --function-name "$BACKEND_FUNCTION" \
  --qualifier "$BACKEND_ALIAS" \
  --provisioned-concurrent-executions "$PC" \
  --region "$AWS_REGION"

aws lambda get-provisioned-concurrency-config \
  --function-name "$BACKEND_FUNCTION" \
  --qualifier "$BACKEND_ALIAS" \
  --region "$AWS_REGION"
```

`Status`가 `READY`가 된 뒤 테스트를 시작한다.

## 2. Grafana 모니터링 실행

```bash
docker compose -f k6/docker-compose.monitoring.yml up -d
mkdir -p reports/k6
```

Grafana: <http://localhost:3000> (`admin` / `admin`)

## 3. 기본 부하 테스트

합격 기준:

- 동시 접속자 50명 / 5분 지속
- 평균 응답시간 500ms 이하
- 오류율 1% 미만

```bash
k6 run \
  -e BASE_URL=https://40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com \
  --summary-export=reports/k6/baseline-summary.json \
  --out json=reports/k6/baseline-raw.json \
  --out influxdb=http://localhost:8086/k6 \
  k6/scenarios/baseline.js
```

## 4. 스파이크 테스트

합격 기준:

- 평상시 10명 → 200명 급증 → 10명 복귀
- 스파이크 발생 후 60초 이내 정상 응답 복귀
- 스파이크 중 오류율 5% 미만

```bash
k6 run \
  -e BASE_URL=https://40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com \
  --summary-export=reports/k6/spike-summary.json \
  --out json=reports/k6/spike-raw.json \
  --out influxdb=http://localhost:8086/k6 \
  k6/scenarios/spike.js
```

## 5. 요즘 뜨는 시나리오: Cold Start 대응

PC 적용 전/후 비교가 가장 제출 목적에 맞다.

```bash
# PC 적용 전
k6 run \
  -e BASE_URL=https://40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com \
  --summary-export=reports/k6/cold-before-summary.json \
  --out json=reports/k6/cold-before-raw.json \
  --out influxdb=http://localhost:8086/k6 \
  k6/scenarios/cold_start.js

# PC=50 READY 확인 후
k6 run \
  -e BASE_URL=https://40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com \
  --summary-export=reports/k6/cold-after-pc50-summary.json \
  --out json=reports/k6/cold-after-pc50-raw.json \
  --out influxdb=http://localhost:8086/k6 \
  k6/scenarios/cold_start.js
```

대체 시나리오로 AI 추론 급증을 선택할 경우:

```bash
k6 run \
  -e BASE_URL=https://40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com \
  --summary-export=reports/k6/ai-surge-summary.json \
  --out json=reports/k6/ai-surge-raw.json \
  --out influxdb=http://localhost:8086/k6 \
  k6/scenarios/ai_surge.js
```

## 6. 제출 캡처 체크리스트

- k6 터미널 summary: `http_req_duration`, `http_req_failed`, 커스텀 threshold 통과 여부
- Grafana: 요청 수, 평균/p95/p99 응답시간, 오류율 그래프
- Lambda CloudWatch: `ConcurrentExecutions`, `ProvisionedConcurrentExecutions`, `ProvisionedConcurrencyUtilization`, `ProvisionedConcurrencySpilloverInvocations`
- PC 설정 화면 또는 `get-provisioned-concurrency-config` 결과

## 7. 테스트 종료 후 비용 정리

PC 50은 켜져 있는 시간 동안 비용이 발생하므로 제출 캡처 후 끈다.

```bash
aws lambda delete-provisioned-concurrency-config \
  --function-name "$BACKEND_FUNCTION" \
  --qualifier "$BACKEND_ALIAS" \
  --region "$AWS_REGION"
```
