---
parent: Requirements
---
# CI

## Protect privileged workflow outputs from untrusted PR content
`req~ci.on-pr-closed.output-injection~1`

Workflows that run in `pull_request_target` context must not write attacker-controlled pull request content to `GITHUB_OUTPUT` with a fixed delimiter, and must not pass untrusted PR metadata into privileged shell commands without validation and quoting.

## Keep an issue with its assignee when somebody else opens a pull request
`req~ci.link-issue.assignment-check~1`

A pull request referencing an issue pins that issue and assigns its author only if the author is one of the issue's assignees or the issue has none. Otherwise the pull request author is warned once on the pull request, the issue receives one note per pull request, and the incident is recorded per GitHub login in the `STRANGE_USERS` repository secret. The pull request that causes an author's third recorded incident is closed automatically; earlier incidents do not affect pull requests that follow the policy.

<!-- markdownlint-disable-file MD022 -->
