---
nav_order: 72
parent: Decision Records
status: proposed
date: 2026-09-03
---

# Schedule the browser-extension fulltext fetcher with a short head start

## Context and Problem Statement

`FulltextFetchers` runs the direct (HTTP) fetchers first and consults the browser-extension companion fetcher (`BrowserExtensionFulltextFetcher`, a `FallbackFulltextFetcher`) only when the direct fetchers all return nothing — a fallback, per [ADR-0071](0071-separate-native-messaging-hosts-for-import-and-fulltext.md). The direct fetchers run as one batch (`HeadlessExecutorService.executeAll`): the phase ends only when the *slowest* fetcher finishes.

For sources the direct fetchers cannot reach — e.g. IEEE Xplore, which is anti-bot / paywalled and is exactly what the extension exists to handle — the slowest direct fetcher runs to its timeout before the extension even starts. `DoiResolution` alone waits 30 s (`Jsoup … timeout(30_000)`) following the DOI into `ieeexplore.ieee.org`. So the user waits up to ~30 s *before* the extension begins, on top of the extension's own (unavoidable) tab-open-and-download time.

How should the extension fetcher be scheduled so that extension-only sources are fast, without reopening a browser tab on every search for content the direct fetchers already handle?

## Decision Drivers

* Time-to-PDF for sources only the extension can fetch (IEEE, other paywalled/anti-bot publishers).
* No browser tab and no wasted extension work for content the direct fetchers already return — the reason the extension is a fallback at all ([ADR-0071](0071-separate-native-messaging-hosts-for-import-and-fulltext.md)).
* Preserve trust-based result selection: a higher-trust direct result must still win over a lower-trust extension result.
* Simplicity and statelessness — avoid per-user learned state, cold starts, and staleness where a simpler mechanism suffices.
* Protocol neutrality: the change is JabRef-internal; the wire protocol (`req~bxf.*`) is untouched.

## Considered Options

* Race the extension as an equal source (the pre-[ADR-0071] behaviour).
* Keep the strict fallback (status quo).
* Learn per-registrant which sources the extension handles best and route eagerly for them.
* Give the direct fetchers a short head start, then run the extension in parallel.

## Decision Outcome

Chosen option: **"Head start"**, because it removes the slow-fetcher latency for extension-only sources while keeping the tab-free behaviour for content the direct fetchers handle, and does so statelessly with a single tunable and no cold start.

Concretely: launch the direct fetchers; wait a short grace period (proposed 4 s) for a usable result; if one arrives, return it and never touch the extension; otherwise launch the extension in parallel and take the first/most-trusted result, cancelling the loser by closing its connection.

### Consequences

* Good: extension-only sources resolve in ≈ *head start + extension time* instead of *slowest-primary timeout + extension time* (dropping ~30 s for IEEE).
* Good: content the direct fetchers return within the head start never starts the extension → no tab, exactly as today.
* Good: cancelling the loser by closing the connection finally exercises the extension's `req~bxf.cancellation~1` path in the cross-fetcher case (a gap raised in review), aborting the tab when a direct fetcher wins after the extension started.
* Neutral: a source slightly slower than the head start briefly opens then closes a tab; the head start is a heuristic constant to tune.
* Neutral: the mechanism does not learn — every extension-only fetch pays the (small, bounded) head start. "Learned per-registrant routing" can be layered on later if that cost proves material.

### Confirmation

Unit tests in `FulltextFetchersTest`: a fast direct-fetcher result is returned without the fallback ever being invoked; when the direct fetchers return nothing within the head start, the fallback result is returned; a higher-trust direct result still wins over a concurrent extension result. Manual: an IEEE DOI resolves markedly faster than before, and an open-access DOI still opens no browser tab.

## Pros and Cons of the Options

### Race the extension as an equal source

* Good, because there is no added latency — the extension runs concurrently from the start.
* Bad, because it opens a browser tab on *every* fulltext search, including for open-access content the direct fetchers fetch silently — the noise and wasted work that motivated the fallback.

### Keep the strict fallback (status quo)

* Good, because it is the simplest possible rule and never opens a tab for direct-fetchable content.
* Bad, because it serialises the extension behind the slowest direct fetcher, adding that fetcher's full timeout (~30 s for IEEE) to every extension-only fetch.

### Learn per-registrant which sources the extension handles best

* Good, because after the first paper it can route IEEE-like DOIs straight to the extension, saving even the head start.
* Good, because the routing is data-driven rather than a fixed heuristic.
* Bad, because it needs persistent per-user state (a cache keyed by DOI registrant, e.g. `10.1109`), with a refresh/TTL policy and invalidation when a publisher's access changes.
* Bad, because the first paper from each such source still pays the full price (cold start), and a stale "prefer extension" entry can open an unnecessary tab after access changes.
* Bad, because it is materially more code and more failure modes than the head start, for a marginal gain over it (saving a few seconds).

### Give the direct fetchers a short head start, then run the extension in parallel

* Good, because it removes the slow-primary tax for extension-only sources while keeping direct-fetchable content tab-free.
* Good, because it is stateless: one grace-period constant, no cache, no cold start, no staleness.
* Good, because it preserves trust-based selection and exercises the cancellation path.
* Neutral, because the head start is a heuristic; too short reopens tabs for slightly-slow direct sources, too long adds latency for extension-only sources.
* Bad, because it requires restructuring the current two-phase `race()` into a single staggered, cancellable race.

## More Information

Supersedes the scheduling half of [ADR-0071](0071-separate-native-messaging-hosts-for-import-and-fulltext.md)'s consequences (the fallback ordering); the two-host separation itself is unchanged. The "learned per-registrant routing" option is recorded as a deliberate future refinement, not a rejection.
