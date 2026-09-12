---
parent: Requirements
---
# What's new

## Show a developer what landed in the checkout since the last look
`req~whats-new.checkout-news~1`

When JabRef runs out of a git checkout, a toolbar button lists the `CHANGELOG.md` entries not yet shown, grouped by who wrote them: every other author by name, the developer's own entries pushed from another machine, and the developer's own entries in this checkout.
Every five minutes the checkout is fetched; once it is behind its upstream, the button's glyph is coloured and its tooltip carries the pending entries and the commit count.
Clicking the button fetches again, lists what that fetch found and only then offers *Restart to update*, which quits JabRef the ordinary way and leaves a marker for the `just run-loop` recipe to pull, rebuild and start it again.
Entries shown once are remembered in the checkout's git directory, so nothing is shown twice and nothing is missed while JabRef is closed; a fresh checkout is not greeted with the whole changelog.
A packaged JabRef, which has no checkout to update from, shows no such button.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
