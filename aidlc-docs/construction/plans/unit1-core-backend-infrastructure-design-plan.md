# Infrastructure Design Plan — Unit 1: Core Backend
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24
**단계**: CONSTRUCTION — Infrastructure Design

## 생성 체크리스트
- [x] Step 1: infrastructure-design.md 생성
- [x] Step 2: deployment-architecture.md 생성

## 확정 답변

| Q | 답변 |
|---|------|
| Q1 외부접근 | A — CloudFront → 미니PC 공인 IP 직접 연결 |
| Q2 환경분리 | B — 로컬 개발 + 미니PC 운영 분리 |
| Q3 CI/CD | A — GitHub Actions → SSH 자동 배포 |
| Q4 DB백업 | A — S3 자동 백업 (pg_dump + cron) |
| Q5 환경변수 | A — .env 파일 (미니PC 로컬, git 제외) |

> 주의: Q1 에이전트 권장은 B(Cloudflare Tunnel). 학교 네트워크 방화벽 정책에 따라 공인 IP 노출이 제한될 수 있음.
