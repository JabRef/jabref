---
parent: Code Howtos
---
# Requirements

In JabRef, we use [OpenFastTrace](https://github.com/itsallcode/openfasttrace) to track requirements. This is a handy tool that allows us to express our ideas and link them to the code. It supports both backward and forward tracing. For example, questions such as "How is the requirement implemented?" (forward trace) or "Which requirement led to this implementation?" (backward trace) are easy to answer.

## Example

Imagine you are developing Git features in JabRef and want to ensure that a PAT (Personal Access Token) is verified and has sufficient access rights. This is a very useful feature. With OFT, you can write a human-readable requirement in Markdown like this:

```markdown
## GitHub personal access token verification
`req~git.share.personal-access-token-verification~1`

The GitHub sharing dialog must allow users to verify that their personal access token has push access to the configured GitHub repository before sharing a library.

Needs: impl
```

The power of OFT is that it actually **tracks** requirements and can report which requirements were not implemented.

To link the requirement to the code, you write this in the code:

```java
    public void checkGitHubAccess() {
        // [impl->req~git.share.personal-access-token-verification~1]
        BackgroundTask
                .wrap(() -> gitHubRepositoryAccessChecker.check(repositoryUrlProperty.get().trim(), gitPreferences.getUsername(), gitPreferences.getPat()))
                .onSuccess(this::showGitHubAccessResult)
                .onFailure(e -> {
                    LOGGER.debug("Could not check GitHub repository access", e);
                    dialogService.showErrorDialogAndWait(
                            Localization.lang("GitHub access"),
                            Localization.lang("Could not connect to GitHub. Please check your network connection and try again."));
                })
                .executeWith(taskExecutor);
    }
```

After that, running `traceRequirements` will succeed.

Tracing requirements is useful because it helps us:

- link ideas and human-oriented descriptions to the code;
- keep implementation and tests (and other artifacts) aligned;
- track requirement revisions so that updates in Markdown also require updates to the relevant code;
- add context about why the code was written or why a requirement is needed, including links to issues;
- provide a useful aid for AI agents;
- prevent people from forgetting or deleting important requirements or code as the project evolves.

## How to use OFT

### Tooling

OFT is available on GitHub at <https://github.com/itsallcode/openfasttrace/releases>, where you can use the CLI.

It is highly recommended to use the LSP server and related extensions (not official and not affiliated with OFT): <https://github.com/fgorke/openfasttrace-language-server/releases> (with VS Code and IntelliJ integrations).

We also integrate it with Gradle. You can run the `traceRequirements` task, which produces `build/reports/tracing.txt` with a detailed report.

We also have automated CI checks to verify requirement coverage; uncovered requirements are treated as an error.

We recommend using VS Code with the `markdownlint` extension to edit requirement files rather than IntelliJ, because VS Code understands `markdownlintdisable` directives.

### Requirement syntax

Requirements should be written in the main JabRef repository under `docs/requirements`. They are grouped by the feature they relate to. Universal requirements can go in cross-cutting categories such as UX.

You write a requirement identifier directly below a Markdown heading.

Example:

```markdown
### Example
`req~ai.example~1`
```

The requirement ID (`req~ai.example~1`) consists of several parts separated by a tilde:

- an artifact type: `req`;
- the main part: `ai.example`;
- the revision: `1`.

It is important that there is no empty line directly after the heading.

{: note}
You need to add `<!-- markdownlint-disable-file MD022 -->` at the end of the file because the ID of the requirement must follow the heading directly.

After adding a heading and identifier, you write the description of the requirement. At the end of the requirement, you list the artifact types it needs.

```markdown
Needs: impl, utest
```

## Linking implementations

After writing the requirement, you add a comment at the implementation site to indicate that it is covered:

Java code:

```java
// [impl->req~ai.example~1]
```

Markdown:

```markdown
<!-- [dsn->req~ai.summarization.general.storage~1] -->
```

## Conventions used in JabRef

For requirement IDs, we follow the OFT standard artifact types, with the addition of `adr`. For the main part, we separate the path with `.`, and separate words with a hyphen, as in the example above.

We use these artifact types:

- `feat`: general features or ideas, primarily user-facing, unrefined requirements of varying size;
- `req`: a specific nuance, cross-cutting requirement, or bug fix;
- `impl`: a code implementation (typically Java, but can also be GitHub CI/CD code, etc.);
- `utest`: a unit test;
- `itest`: a test involving external services;
- `dsn`: a design document for specifications;
- `arch`: a high-level design requirement;
- `uman`: a user manual page or section;
- `adr`: an Architectural Decision Record.

Throughout development, you will mainly work with `feat`, `req`, `impl`, and `utest`. Please try to use these four types, as the others are rarely needed and we do not have good examples for them.

We would really like to use the `uman` artifact type, as new features often also need to be explained to users. At the time of writing, we do not use this type in the requirements because we do not have a cross-repository setup for OFT.

The boundary between `feat` and `req` can sometimes be debatable, because from a software engineering perspective they refer to the same thing. However, we assign them based on the outcome: a new fetcher is a feature, while special handling of a field is a requirement. This gives us an overview of what we have in JabRef.

## How to write a requirement

### Requirement style

**General ideas**:

- `req~` = a constraint the system **must** satisfy. `feat~` = a capability offered to the user.
- Title grammar should match the tag: `req~` → **"Subject must verb"**, `feat~` → **"User can verb"**.
- One item, one requirement. If the title needs "and," split it into two items.
- The title should carry the full normative statement; the description should only add what the title could not fit.

**Title style**:

- Put the subject first, then the modal verb.
- For `req~`, always use **must** — never "should," "needs to," or "is required to." Use one modal verb only.
- Avoid nominalizations ("must be verified," not "verification").
- Avoid system-as-narrator phrasing ("allows the user to," "offers to") — state what must happen.

**Description style**:

Use it only for what the title cannot carry:

- the triggering condition;
- edge cases or boundary behavior;
- a brief rationale if needed;
- the GitHub issue;
- other relevant context.

Do not repeat the subject + verb from the title, do not smuggle in a second requirement, and do not write marketing copy.

### Example of a requirement

**Bad**:

```markdown
## GitHub personal access token verification
`req~git.share.personal-access-token-verification~1`

The GitHub sharing dialog must allow users to verify that their personal
access token has push access to the configured GitHub repository before
sharing a library.
```

There is no modal verb in the title, and the subject-first structure is missing. The normative content is buried in the paragraph instead of the title.

**Good**:

```markdown
## GitHub personal access token push access must be verifiable before sharing
`req~git.share.personal-access-token-verification~1`

Verification happens in the GitHub sharing dialog before the library is shared.
```

The title carries the full constraint, and the description adds only the missing detail about where it happens.

### Linking requirements

The syntax for linking a requirement to code was already shown above. However, you should still follow the principle of **linking the requirement to the most specific code location**. Only if there is no specific place, or if too many components are involved, can you link it at the method or class level.

In extreme cases, you can write the linking comment and add a second comment below it explaining how the implementation works.

All comments should be placed before the annotations.

### Unimplemented requirements

Sometimes you have a lot of useful ideas but no time to write or implement them. In that case, you can write the requirements and mark them as `draft` by adding:

```markdown
Status: draft
```

This way they will be filtered out when running `traceRequirements`, while still preserving the ideas.

## More information

We use OFT mainly to trace small ideas, notes, and wishes rather than to manage the full software engineering requirements process. The concepts and terminology of software requirements engineering are still useful for structuring and writing requirements, and they help us express them clearly. However, we do not apply all of its principles in full.

For example, INVEST is useful for writing better requirement descriptions, but we do not treat our requirements as fully negotiable items because they are more like concrete instructions or orders than open-ended, negotiable agreements. We also do not explicitly record a separate value dimension for each requirement in this workflow.

It is better to write more small requirements than one large one. This makes coverage and tracking more detailed.

- [General reading on traceability](https://www.sodiuswillert.com/en/blog/implementing-requirements-traceability-in-systems-software-engineering)
- [OFT User guide](https://github.com/itsallcode/openfasttrace/blob/main/doc/user_guide/user_guide.md)
- [OFT artifacts](https://github.com/itsallcode/openfasttrace/blob/main/doc/user_guide/user_guide.md#specification-item-artifact-type)
