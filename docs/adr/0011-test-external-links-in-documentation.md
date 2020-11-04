# Test external links in documentation

## Context and Problem Statement

The JabRef repository contains Markdown (`.md`) files documenting the JabRef code.
The documentation contains links to external resources.
For high-quality documentation, external links should be working.

## Decision Drivers

* Checking external links should not cause issues in the normal workflow

## Considered Options

* Check external links once a month
* Check external links in the "checkstyle" task
* Do not check external links

## Decision Outcome

Chosen option: "\[option 1\]", because \[justification. e.g., only option, which meets k.o. criterion decision driver \| which resolves force force \| … \| comes out best \(see below\)\].

### Positive Consequences

* Automatic notification of broken external links

### Negative Consequences

* Some external sites need to [be disabled](https://github.com/JabRef/jabref/pull/6542/files). For instance, GitHub.com always returns "forbidden". A [filter for status is future work of the used tool](https://github.com/tcort/markdown-link-check/issues/94#issuecomment-634947466).

## Pros and Cons of the Options

### Check external links once a month

* Good, because does not interfere with the normal development workflow
* Bad, because an additional workflow is required

### Check external links in the "checkstyle" task

* Good, because no separate workflow is required
* Bad, because checks fail independent of the PR (because external web sites can go down and go up independent of a PR)

### Do not check external links

* Good, because no testing at all is required
* Bad, because external links break without any notice
* Bad, because external links have to be checked manually
