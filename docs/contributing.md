---
nav_order: 2
---
# Contributing

Please head to our [contributing guide in the main repository](https://github.com/JabRef/jabref/blob/main/CONTRIBUTING.md#contributing).

## Branching strategy

JabRef has two long-lived branches in two repositories (see [ADR-0073](decisions/0073-single-stable-branch-with-automatic-ports.md)):

- `main` is the development branch. Every pull request targets `main`, unless the change only makes sense for the released version.
- `black-forest` in [JabRef/jabref-black-forest](https://github.com/JabRef/jabref-black-forest) is the last regular release plus the fixes ported to it. Releases are tagged on `black-forest`. It is a [long-lived release branch](https://martinfowler.com/articles/branching-patterns.html#long-lived-release-branch) in the terms of Martin Fowler's branching patterns; fixes are made on `main` and cherry-picked, as the [hotfix branch](https://martinfowler.com/articles/branching-patterns.html#hotfix-branch) pattern recommends.

### Getting a fix into the released version

Open the pull request against `main` as usual. CI adds the label `dev: send-to-black-forest` when the pull request links an issue of type "bug": at creation, or when an edit of the description newly links one. Maintainers add or remove the label by hand when the fix should or should not reach users before the next regular release; CI never overrides that decision (a push does not re-add the label).
The label triggers two things:

1. The check "Would merge into the other branch" simulates the port. If it fails, the fix conflicts with `black-forest`; resolve that before or after the merge, as described below.
2. After the merge, CI ([korthout/backport-action](https://github.com/korthout/backport-action)) cherry-picks the change into a pull request `[Port to black-forest] ...` in JabRef/jabref-black-forest from the branch `port-<number>-to-black-forest`. A clean port carries the `automerge` label and merges once CI passes. A conflicting port is opened as a draft whose commit contains the conflicting files with Git's conflict markers (`<<<<<<<`, `=======`, `>>>>>>>`) left in, so the diff shows exactly what needs resolving; the workflow run of the original pull request fails.

A pull request against `black-forest` is always ported to `main` the same way, so `main` never lacks a fix that users have.

Ports are done for the change as merged, so a port pull request must never be labeled `dev: send-to-black-forest` again.

### Resolving a conflicting port

```bash
git remote add black-forest https://github.com/JabRef/jabref-black-forest.git  # once
git fetch black-forest
git switch -c port-<number>-to-black-forest black-forest/port-<number>-to-black-forest
# resolve the conflict markers, then
git add -A && git commit
git push black-forest HEAD
gh pr ready --repo JabRef/jabref-black-forest <port pull request number>
```

`CHANGELOG.md` rarely conflicts: the port merges the added and removed entries of `## [Unreleased]` on entry level.
Add the changelog entry once, in the branch the pull request targets; do not add it to the port.

### Releases (maintainers)

- Regular release: prepare `CHANGELOG.md` on `main` as before (rename `## [Unreleased]` to the version and add a fresh `## [Unreleased]`), then merge `main` into `black-forest` and tag on `black-forest`. Always take `main`'s `CHANGELOG.md` in that merge; the union merge attribute would otherwise combine both unreleased blocks silently instead of reporting a conflict:

  ```bash
  git fetch black-forest
  git switch -c black-forest black-forest/black-forest
  git merge --no-commit origin/main
  git checkout origin/main -- CHANGELOG.md
  git commit
  git push black-forest black-forest
  ```

- Hotfix release: on `black-forest`, rename `## [Unreleased]` to the version, tag, and re-add an empty `## [Unreleased]`. The port of that commit to `main` conflicts by design: insert the version section below `main`'s `## [Unreleased]` and remove the ported entries from it.
