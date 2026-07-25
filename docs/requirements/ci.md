---
parent: Requirements
---
# CI

## Protect privileged workflow outputs from untrusted PR content
`req~ci.on-pr-closed.output-injection~1`

Workflows that run in `pull_request_target` context must not write attacker-controlled pull request content to `GITHUB_OUTPUT` with a fixed delimiter, and must not pass untrusted PR metadata into privileged shell commands without validation and quoting.

<!-- markdownlint-disable-file MD022 -->
