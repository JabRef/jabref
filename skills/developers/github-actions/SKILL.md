---
name: github-actions
category: developers
description: How to reference GitHub Actions in JabRef workflows — pin every external action to a full commit SHA with the release tag as a comment, and how to resolve that SHA. Use when adding or editing a `uses:` line under .github/workflows.
license: MIT
---

# GitHub Actions in JabRef workflows

Pin every action from outside the repository to a **full 40-character commit SHA**, with the release tag as a trailing comment:

```yaml
- uses: actions/checkout@08c6903cd8c0fde910a37f88322edcfb5dd907a8 # v5.0.0
```

Tags and branches (`@v5`, `@v1.6.3`, `@main`) are mutable: the action's code can be swapped out under us, and in a `pull_request_target` or `workflow_run` job it runs with write-capable tokens against untrusted pull request content. Dependabot updates the SHA together with its comment, so the pin stays current on its own.

Convert a tag-pinned `uses:` line to a SHA when a change touches it; do not sweep unrelated workflows in the same pull request.

Resolve a tag with:

```bash
gh api repos/<owner>/<repo>/commits/<tag> --jq .sha
```

For a sub-action such as `gittools/actions/gitversion/setup`, query the repository root (`gittools/actions`).
