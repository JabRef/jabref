---
nav_order: 72
parent: Decision Records
status: proposed
date: 2026-09-07
decision-makers: Siedlerchr, koppor, calixtus
---

# Use a single `stable` branch with label-driven, automated ports

## Context and Problem Statement

JabRef releases are cut from `main`. A fix for a bug in the released version therefore ships together with everything that landed on `main` since the release: unfinished features, refactorings, and dependency updates that nobody has tested in a release yet. Maintainers hesitate to publish a quick fix because the fix cannot be separated from that state.

How do we ship a fix for the current release without shipping the development state of `main`, while keeping the extra work for contributors and maintainers close to zero?

## Decision Drivers

* A fix for the released version must be publishable without the unreleased changes on `main`.
* A fix is written and reviewed once; nobody re-implements it for a second branch.
* Divergence between the branches must surface before a pull request is merged, not weeks later at release time.
* The existing pull request automation (CI, merge queue, `automerge` label, conflict check) must work unchanged on the second branch.
* The maintainer team is small: one additional branch is manageable, one per release line is not.
* `CHANGELOG.md` stays a single [Keep a Changelog](https://keepachangelog.com/) file per branch that [heylogs](https://github.com/nbbrd/heylogs) accepts.

## Considered Options

* Keep releasing from `main` only
* A single `stable` branch, ports driven by a label and done by CI
* One release branch per version (`v5`, `v6`, ...), ports per branch
* Fix on `stable` first and merge `stable` into `main` regularly

## Decision Outcome

Chosen option: "A single `stable` branch, ports driven by a label and done by CI", because it isolates releases from `main` with one branch and keeps the contributor workflow unchanged: a pull request still targets `main`; a label decides whether the change also reaches the release.

* `main` is the development branch. Every pull request targets `main` unless the change only makes sense for the released version.
* `stable` is the last regular release plus the fixes ported to it. A regular release merges `main` into `stable` and tags on `stable`; a hotfix release tags `stable` as it is. In the terms of Martin Fowler's [branching patterns](https://martinfowler.com/articles/branching-patterns.html), `stable` is a [long-lived release branch](https://martinfowler.com/articles/branching-patterns.html#long-lived-release-branch), and a fix follows the [hotfix branch](https://martinfowler.com/articles/branching-patterns.html#hotfix-branch) recommendation: it is made on the [mainline](https://martinfowler.com/articles/branching-patterns.html#mainline) and cherry-picked to the release branch.
* A pull request into `main` labeled `dev: into-stable` is ported to `stable` after the merge. CI adds the label when the pull request links an issue of type "bug", but only when the link is new (pull request creation, or a description edit that adds it), so a maintainer's manual decision for or against the label is never reverted by a later push. A pull request into `stable` is always ported to `main` after the merge, so `main` never lacks a fix that users have.
* Before the merge, the port is simulated as a required check ("Would merge into stable/main"), so a conflict is visible on the pull request itself. After the merge, CI cherry-picks the squash commit into a port pull request, which auto-merges when CI passes. If the cherry-pick conflicts, the port pull request is opened as a draft with the conflict committed, and the workflow run fails: a maintainer resolves the conflict in that pull request.
* `CHANGELOG.md` keeps one `## [Unreleased]` section per branch. heylogs rejects a second unreleased section on `main` in every spelling (`unique-release` for a duplicate `[Unreleased]`, `date-displayed` for a variant such as `[Unreleased (stable)]`), and it would only mirror what `stable`'s own changelog already shows. A changelog entry is written once, in the branch the pull request targets; the port carries it over with a merge driver that applies the added and removed entries of `## [Unreleased]` on entry level (`.jbang/ChangelogCherryPickMergeDriver.java`). A plain cherry-pick would always conflict there, because the neighbouring entries differ between the branches.
* When a hotfix release renames `## [Unreleased]` to a version on `stable`, the port of that commit conflicts on `main` by design and is resolved by hand: the version section is inserted below `main`'s `## [Unreleased]`, and the ported entries are removed from it.

### Consequences

* Good, because the released version can be fixed and re-released within hours, from a branch whose only changes since the release are the ported fixes.
* Good, because a contributor does nothing new; bug fixes get the label automatically, and maintainers correct it with one click.
* Good, because the conflict check turns "will this fix still apply to the release?" into a question answered before the merge.
* Bad, because a change that conflicts needs a maintainer to resolve the port pull request, and until then `stable` lacks the fix.
* Bad, because `stable` only serves the latest regular release; older releases (JabRef 5 while 6 is current) get no fixes. This is the current situation, so nothing is lost.
* Bad, because the branch protection of `main` (required checks, merge queue, auto-merge) has to be duplicated for `stable` in the repository settings.

### Confirmation

`.github/workflows/on-pr-bug-linked.yml` adds the label for newly linked bugs; `.github/workflows/port-to-other-branch.yml` implements the check and the port; the merge driver ships a self-test that the `JBang check` job runs. Whether a fix reached both branches is visible on the original pull request (comment and port pull request link from the port job).

## Pros and Cons of the Options

### Keep releasing from `main` only

* Good, because there is nothing to maintain.
* Bad, because every hotfix is a full release of `main`, which is exactly what maintainers are afraid of.

### A single `stable` branch, ports driven by a label and done by CI

* Good, because one branch covers the one release line that is actually maintained.
* Good, because a label is the smallest possible interface for contributors and reviewers.
* Neutral, because the port is a separate pull request: it re-runs CI on `stable`, which is wanted, but adds one pull request per ported change.
* Bad, because a port of a change that renames the changelog's unreleased section needs manual resolution.

### One release branch per version (`v5`, `v6`, ...), ports per branch

This is the git-flow / GitLab-flow release-branch model: a [release branch](https://martinfowler.com/articles/branching-patterns.html#release-branch) per release.

* Good, because older releases can be fixed as well.
* Bad, because the team does not maintain older releases; the branches would exist without receiving ports.
* Bad, because every port has to name its target branches, and every branch needs its own protection rules and CI budget.

### Fix on `stable` first and merge `stable` into `main` regularly

The fix is made on the release branch and merged forward, the approach Fowler describes as the common one under time pressure.

* Good, because a fix can never be forgotten on `main`: the merge carries everything.
* Bad, because contributors would need to know where to open a pull request, and most changes are not fixes.
* Bad, because the merge of `stable` into `main` conflicts in `CHANGELOG.md` every time, and a merge commit of a whole branch is harder to review than a cherry-pick of one change.

## More Information

Setting up the branch (once, by an administrator):

1. Create `stable` from the current release tag: `git push origin v6.0-alpha.6:refs/heads/stable`, and re-add an empty `## [Unreleased]` section to its `CHANGELOG.md`.
2. Copy the branch protection of `main` to `stable`: the same required status checks, the merge queue, and "Allow auto-merge". Without it, the `automerge` label on a clean port pull request has nothing to wait for and GitHub refuses to enable auto-merge.
3. Create the label `dev: into-stable`.

The maintainer workflow is documented in [Branching strategy](https://devdocs.jabref.org/contributing.html#branching-strategy).
