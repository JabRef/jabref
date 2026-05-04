---
nav_order: 48
parent: Decision Records
---

# JBang script modification for testing in CI

## Context and Problem Statement

All JBang scripts on `main` should always compile.

Example:

```terminal
gg.cmd  https://github.com/JabRef/jabref/blob/main/.jbang/JabLsLauncher.java
```

- JBang scripts link to `org.jabref:jablib:6.0-SNAPSHOT`.
- JBang scripts include java files from the respective `-cli` repository and the sources of the respective server project. E.g., `JabLsLauncher.java` includes `ServerCli.java` from `jabls-cli` and all sources from `jabls`.
- Code changes might change a) things inside the server project and b) things in JabRef's logic.

As a consequence, the JBang script might break.

Case a) can be detected when running the JBang check in the respective PR.
Case b) can be detected when running the JBang check in the main branch, because `org.jabref:jablib:6.0-SNAPSHOT` is updated there.

We aim for checking JBang scripts in PRs and on `main`.

## Decision Drivers

- Fast detection of issues at all JBang files
- Easy CI pipeline

## Considered Options

- Have JBang script have all changed classes of `jablib` included directly
- Use jitpack
-Temporary `-SNAPSHOT.jar`

## Decision Outcome

Chosen option: "Have JBang script have all changed classes of `jablib` included directly.", because comes out best (see below).

## Pros and Cons of the Options

### Have JBang script have all changed classes of `jablib` included directly

- `org.jabref:jablib:6.0-SNAPSHOT` provides non-modified classes
- modified classes are included using JBang's `//SOURCES` directive.

[tj-actions/changed-files](https://github.com/marketplace/actions/changed-files) can be used.

- Good, because least effort

### Use jitpack

- Bad, because jitpack is unreliable

### Temporary `-SNAPSHOT.jar`

- Bad, because setting up a temporary maven repository is effort.
