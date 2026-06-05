#!/usr/bin/env python3
import json
import subprocess
from copy import deepcopy

REGION = "ap-northeast-2"
DASHBOARD_NAME = "k6-FoodGroup-LoadTest"
NAMESPACE = "k6/FoodGroup"


def search(metric_name: str, label: str, expr_id: str):
    return {
        "expression": f'SEARCH('{{{NAMESPACE},Scenario}} MetricName="{metric_name}"', 'Average', 60)',
        "id": expr_id,
        "label": label,
        "period": 60,
    }


def put_dashboard(body: dict) -> None:
    payload = json.dumps(body, ensure_ascii=False)
    subprocess.run([
        "aws", "cloudwatch", "put-dashboard",
        "--dashboard-name", DASHBOARD_NAME,
        "--dashboard-body", payload,
        "--region", REGION,
    ], check=True)


def main() -> None:
    raw = subprocess.check_output([
        "aws", "cloudwatch", "get-dashboard",
        "--dashboard-name", DASHBOARD_NAME,
        "--region", REGION,
        "--query", "DashboardBody",
        "--output", "text",
    ], text=True)
    body = json.loads(raw)
    widgets = body.get("widgets", [])
    if len(widgets) < 8:
        raise SystemExit(f"expected at least 8 widgets, got {len(widgets)}")

    widgets[0] = {
        "type": "text",
        "x": 0,
        "y": 0,
        "width": 24,
        "height": 1,
        "properties": {
            "markdown": "# 🍔 FoodGroup 부하 테스트 대시보드 | UPDATED: all k6 tests",
            "region": REGION,
        },
    }

    widgets[1] = {
        "type": "metric",
        "x": 0,
        "y": 1,
        "width": 12,
        "height": 6,
        "properties": {
            "title": "응답시간 Percentile (k6) - all tests",
            "view": "timeSeries",
            "stat": "Average",
            "period": 60,
            "metrics": [
                [search("http_req_duration_p50_ts", "p50", "e1")],
                [search("http_req_duration_p90_ts", "p90", "e2")],
                [search("http_req_duration_p95_ts", "p95", "e3")],
                [search("http_req_duration_p99_ts", "p99", "e4")],
                [search("http_req_duration_avg_ts", "avg", "e5")],
            ],
            "yAxis": {"left": {"label": "ms", "min": 0}},
            "annotations": {"horizontal": [{"label": "합격 기준 500ms", "value": 500, "color": "#d62728"}]},
            "region": REGION,
        },
    }

    widgets[2] = {
        "type": "metric",
        "x": 12,
        "y": 1,
        "width": 12,
        "height": 6,
        "properties": {
            "title": "오류율 (k6) - all tests",
            "view": "timeSeries",
            "stat": "Average",
            "period": 60,
            "metrics": [[search("http_req_failed_rate_ts", "error-rate", "e6")]],
            "yAxis": {"left": {"label": "%", "min": 0, "max": 10}},
            "annotations": {
                "horizontal": [
                    {"label": "기본 합격 1%", "value": 1, "color": "#ff7f0e"},
                    {"label": "스파이크 합격 5%", "value": 5, "color": "#d62728"},
                ]
            },
            "region": REGION,
        },
    }

    widgets[6] = {
        "type": "metric",
        "x": 0,
        "y": 13,
        "width": 12,
        "height": 6,
        "properties": {
            "title": "총 요청 수 (k6) - all tests",
            "view": "bar",
            "stat": "Sum",
            "period": 60,
            "metrics": [[search("http_reqs_total_ts", "requests", "e7")]],
            "region": REGION,
        },
    }

    widgets[7] = {
        "type": "metric",
        "x": 12,
        "y": 13,
        "width": 12,
        "height": 6,
        "properties": {
            "title": "시나리오별 p95 응답시간 비교 - all tests",
            "view": "bar",
            "stat": "Average",
            "period": 60,
            "metrics": [[search("http_req_duration_p95_ts", "p95", "e8")]],
            "annotations": {"horizontal": [{"label": "500ms 기준선", "value": 500, "color": "#d62728"}]},
            "region": REGION,
        },
    }

    body["widgets"] = widgets
    put_dashboard(body)
    print(f"Updated CloudWatch dashboard: {DASHBOARD_NAME}")


if __name__ == "__main__":
    main()
