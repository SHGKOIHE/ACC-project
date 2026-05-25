# AGENTS.md
 
## Code style
 
- Markdown 작성 시 `.markdownlint-cli2.yaml` 규칙 따름
- MD040 (fenced code language) 적용 — 코드 펜스에는 항상 언어 명시
- MD060 (table alignment) 적용 — 테이블 파이프 수직 정렬
- 커밋 메시지는 [conventional commits](https://www.conventionalcommits.org/) 형식
  (`feat:`, `fix:`, `docs:`, `chore:`)
## Security scanners
 
자동 검사 단계에 사용할 보안 스캐너:
 
| Scanner  | Detects                | Fails on                    |
|----------|------------------------|-----------------------------|
| Semgrep  | Multi-language SAST    | Any finding (PRs: new only) |
| Grype    | Dependency CVEs        | High/critical CVEs          |
| Gitleaks | Secrets in git history | Any non-baselined secret    |
| Checkov  | IaC misconfigurations  | Any check failure           |
| ClamAV   | Malware                | Any detection               |
 
HIGH·CRITICAL 발견 사항은 머지 전 해결 또는 명시적 위험 수용 기록 필요.
 
Inline suppression patterns:
 
- Semgrep: `# nosemgrep: rule-id — justification`
- Checkov: `# checkov:skip=CKV_ID:justification`
## Dockerized security scans
 
로컬 검증용 (Docker 사용 가능 시):
 
```bash
# Grype (의존성 CVE)
docker run --rm -v "$PWD:/workspace" anchore/grype:latest grype dir:/workspace -o sarif=grype.sarif
 
# Gitleaks (시크릿)
docker run --rm -v "$PWD:/repo" zricethezav/gitleaks:latest detect --source /repo --report-format sarif --report-path gitleaks.sarif
 
# Semgrep (SAST)
docker run --rm -v "$PWD:/src" returntocorp/semgrep semgrep --config=r/all --sarif /src > semgrep.sarif
 
# Checkov (IaC)
docker run --rm -v "$PWD:/src" bridgecrew/checkov --directory /src --output-file-path checkov.sarif --output sarif
 
# ClamAV (멀웨어)
docker run --rm -v "$PWD:/data" mkodockx/docker-clamav clamscan -r /data --log=/data/clamdscan.txt
```
 
결과 SARIF 파일은 프로젝트 루트에 저장되어 CI·에이전트가 소비 가능.
 
## Important constraints
 
- 룰·문서 중복 금지 — 공통 가이드는 한 곳에 두고 참조
- 민감 정보(시크릿·키·자격증명) git 커밋 금지 — Gitleaks가 차단
 
