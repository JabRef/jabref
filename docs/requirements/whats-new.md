---
parent: Requirements
---
# What's new

## Show a developer what landed in the checkout since the last look
`req~whats-new.checkout-news~1`

When JabRef runs out of a git checkout, a toolbar button lists the `CHANGELOG.md` entries not yet shown, grouped by who wrote them: every other author by name, the developer's own entries pushed from another machine, and the developer's own entries in this checkout.
Every five minutes the checkout is fetched; once it is behind its upstream, the button's glyph is coloured and its tooltip carries the pending entries and the commit count.
Clicking the button fetches again and lists what that fetch found; when JabRef was started by the `just run-loop` recipe, the window offers *Restart to update* while the fetch runs and, once it answered, whenever commits landed upstream, which quits JabRef the ordinary way and leaves a marker for the recipe to pull, rebuild and start it again.
Entries shown once are remembered in the checkout's git directory, shared with the `just whats-new` window shown before a start, so nothing is shown twice and nothing is missed while JabRef is closed; a fresh checkout is not greeted with the whole changelog.
A packaged JabRef, which has no checkout to update from, shows no such button.

Needs: impl, utest

## Attribute an entry to the author of its pull request
`req~whats-new.pull-request-author~1`

An entry whose text links a GitHub pull request or issue is listed under the author of that pull request, or of the merged pull request that fixed the issue, or, while none is merged, of the open pull request referencing the issue, instead of under whoever typed the changelog line; the last link of an entry counts.
The developer's own GitHub login comes from `github.user`, the token's user, or a public profile email matching `user.email`.
Answers are kept in the checkout's git directory and each look asks GitHub only a bounded number of times, so the anonymous rate limit suffices; without a link, without network or beyond that bound, the entry keeps its `git blame` attribution.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
