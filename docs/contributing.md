---
nav_order: 2
---
# Contributing

Please head to our [contributing guide in the main repository](https://github.com/JabRef/jabref/blob/main/CONTRIBUTING.md#contributing).

## Branching strategy

JabRef has two long-lived branches (see [ADR-0072](decisions/0073-single-stable-branch-with-automatic-ports.md)):

- `main` is the development branch. Every pull request targets `main`, unless the change only makes sense for the released version.
- `stable` is the last regular release plus the fixes ported to it. Releases are tagged on `stable`. It is a [long-lived release branch](https://martinfowler.com/articles/branching-patterns.html#long-lived-release-branch) in the terms of Martin Fowler's branching patterns; fixes are made on `main` and cherry-picked, as the [hotfix branch](https://martinfowler.com/articles/branching-patterns.html#hotfix-branch) pattern recommends.

### Getting a fix into the released version

Open the pull request against `main` as usual. CI adds the label `dev: into-stable` when the pull request links an issue of type "bug": at creation, or when an edit of the description newly links one. Maintainers add or remove the label by hand when the fix should or should not reach users before the next regular release; CI never overrides that decision (a push does not re-add the label).
The label triggers two things:

1. The check "Would merge into the other branch" simulates the port. If it fails, the fix conflicts with `stable`; resolve that before or after the merge, as described below.
2. After the merge, CI ([korthout/backport-action](https://github.com/korthout/backport-action)) cherry-picks the change into a pull request `[Port to stable] ...` from the branch `port-<number>-to-stable`. A clean port carries the `automerge` label and merges once CI passes. A conflicting port is opened as a draft whose commit contains the conflicting files with Git's conflict markers (`<<<<<<<`, `=======`, `>>>>>>>`) left in, so the diff shows exactly what needs resolving; the workflow run of the original pull request fails.

A pull request against `stable` is always ported to `main` the same way, so `main` never lacks a fix that users have.

Ports are done for the change as merged, so a port pull request must never be labeled `dev: into-stable` again.

### Resolving a conflicting port

```bash
git fetch origin
git switch port-<number>-to-stable
# resolve the conflict markers, then
git add -A && git commit
git push
gh pr ready <port pull request number>
```

`CHANGELOG.md` rarely conflicts: the port merges the added and removed entries of `## [Unreleased]` on entry level.
Add the changelog entry once, in the branch the pull request targets; do not add it to the port.

### Releases (maintainers)

- Regular release: prepare `CHANGELOG.md` on `main` as before (rename `## [Unreleased]` to the version and add a fresh `## [Unreleased]`), then merge `main` into `stable` and tag on `stable`. Always take `main`'s `CHANGELOG.md` in that merge; the union merge attribute would otherwise combine both unreleased blocks silently instead of reporting a conflict:

  ```bash
  git switch stable
  git merge --no-commit main
  git checkout main -- CHANGELOG.md
  git commit
  ```

- Hotfix release: on `stable`, rename `## [Unreleased]` to the version, tag, and re-add an empty `## [Unreleased]`. The port of that commit to `main` conflicts by design: insert the version section below `main`'s `## [Unreleased]` and remove the ported entries from it.
