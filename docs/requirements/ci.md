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

## Port merged pull requests between `main` and `stable`
`req~ci.port-to-other-branch.label-driven-ports~1`

A pull request into `main` that carries the label `dev: into-stable`, and every pull request into `stable`, is checked before the merge for whether its squashed change applies to the other branch; a conflict fails the check. After the merge, the change is cherry-picked into a port pull request against the other branch. A clean port is auto-merged once CI passes; a conflicting port is opened as a draft with the conflict committed, and the run fails. Port pull requests themselves are never ported back. The label is added automatically when a pull request into `main` newly links an issue of type "bug" (at creation or by a description edit); it is never added again for a link that was already there, so a manual removal or addition of the label is kept.

<!-- markdownlint-disable-file MD022 -->
