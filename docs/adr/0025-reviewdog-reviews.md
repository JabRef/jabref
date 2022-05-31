---
parent: Architectural Decisions
nav_order: 24
---
# Reviewdog findings are code reviews

## Context and Problem Statement

JabRef offers [guidelines to setup the local workspace](https://devdocs.jabref.org/getting-into-the-code/guidelines-for-setting-up-a-local-workspace).
There is also a section on [JabRef's code style](https://devdocs.jabref.org/getting-into-the-code/guidelines-for-setting-up-a-local-workspace#using-jabrefs-code-style).
There are pull requests by newcomers, which do not follow that style guide.

How to quickly provide feedback to contributors that checkstyle was not matched?

## Decision Drivers

* Be friendly to newcomers
* Provide fast feedback to contributors
* Lower the workload of maintainers
* Keep maintainers focused on the "real" challanges of the code changes

## Considered Options


* Use [Reviewdog's PullRequest review reporter](https://github.com/reviewdog/reviewdog#reporter-github-pullrequest-review-comment--reportergithub-pr-review)
* Use [Reviewdog's check reporter](https://github.com/reviewdog/reviewdog#reporter-github-checks--reportergithub-check)

## Decision Outcome

Chosen option: "Use Reviewdog's PullRequest review reporter", because resolves force to provide fast feedback.
We accept that newcomers might be annoyed if quick automatic feedback by a bot is given:
We value the time of our maintainers and want to keep them focused on the real challanges of the code changes.
