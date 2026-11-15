---
nav_order: 83
parent: Decision Records
status: proposed
date: 2026-11-15
---
# Place rate limiting, retries and provider identification in provider policies

`adr~fetchers.provider-policies~1`

Needs: impl

## Context and Problem Statement

Three concerns apply to requests to an external provider: rate limiting (`req~fetchers.rate-limiting~1`), retrying HTTP 429 (`req~fetchers.identifier-rate-limit-retries~1`), and identification with an API key or contact email (e.g. `req~fetchers.crossref-polite-pool~1`).
Today each fetcher handles them itself: a `static` rate limiter per fetcher class, `FetcherRetry` in `IdParserFetcher` and `SemanticScholarCitationFetcher`, and keys added in URL builders or as headers.

This has two problems:

* Limits are shared per fetcher class, not per provider. Three fetchers call Crossref with three independent limiters of 50 requests/s each, while Crossref documents 1–10 requests/s and 1–3 parallel requests ([Crossref's announcement](https://www.crossref.org/blog/announcing-changes-to-rest-api-rate-limits/)).
* The concerns depend on each other, and they are properties of the provider. For Crossref, the `mailto` parameter selects the "polite" pool and therefore the limit. Semantic Scholar needs more retries because it answers 429 without `Retry-After`.

Where should these provider rules live?

## Decision Drivers

* A provider's rules hold for every request to that provider, regardless of which fetcher sends it.
* Identification and the limit it selects are decided in one place.
* Writing a fetcher does not require knowledge of limits, retries or identification.
* Every attempt takes a new permit, and a status code is retried on one level only.

## Considered Options

* Per fetcher, with limiters shared per provider
* Provider policy chosen by the caller and applied by `URLDownload`
* Provider policy looked up by URL in `URLDownload`

## Decision Outcome

To be decided in the maintainer call. "Per fetcher" leaves every rule to each fetcher author, which is how the current inconsistencies arose. Between the other two options, the assessment depends on:

* **Should requests be covered without the caller doing anything?** Only the lookup by URL covers requests that a fetcher author forgets to configure. Example: `CrossRefCitationFetcher` sends no `mailto` today and therefore uses Crossref's public pool. For redirects to another provider (e.g. `doi.org` to `data.crossref.org`), this only holds if `URLDownload` follows redirects itself (see Details).
* **Is visibility at the call site important?** The chosen policy is visible in the fetcher; the lookup by URL is not.

### Details

These apply to both policy options.

A provider policy bundles the provider's rate limits and parallel request limit, its retry rules, and its identification.
Identification belongs into the policy, because for Crossref it selects the limit. Split across fetcher and policy, the two could diverge.

`URLDownload` applies the policy:

* Retries wrap opening the connection. HTTP 429 is known before the body is read, so parsing never has to be retried.
* The rate limit is taken on every attempt.
* The parallel request permit is held while the connection is opened, i.e. until the response headers arrive. Providers count the whole transfer, but for small API responses most of the time passes before the headers, and a remaining 429 is retried. Holding the permit until the body is read would need a hook in every method of `URLDownload` that reads the body.
* Policies that adapt to responses (like `DoiFetcher` adapting to `X-Rate-Limit-*` headers) see the response.
* Identification added to the URL is redacted in log messages and exceptions.
* Requests without a policy behave as today.

Flows that need more than identification on each request, such as OAuth with token refresh (CiteDrive), stay separate clients.

### Consequences

* Good, because all requests to a provider share one set of limits and one identification.
* Good, because `FetcherRetry` calls, per-fetcher limiters and identification code are removed from the fetchers.
* Bad, because code that does not use `URLDownload` gets no policy until it moves to `URLDownload`: `CrossRef.isValidKey` (opens the `URLConnection` directly), `JournalInformationFetcher` (Unirest), `ArXivFetcher` (own `HttpURLConnection`), and `SemanticScholar` (Jsoup).
* Bad, because during migration a request must not be retried both by `IdParserFetcher`'s `FetcherRetry` and by a policy, otherwise attempts multiply.
* Bad, because `req~fetchers.identifier-rate-limit-retries~1` applies to identifier fetchers only. Retries per provider apply to every request to that provider, so the requirement has to be revised.

### Confirmation

* The provider policies carry `[impl->req~fetchers.rate-limiting~1]`, `[impl->req~fetchers.identifier-rate-limit-retries~1]` and `[impl->req~fetchers.crossref-polite-pool~1]`.
* A test checks that a policy adds no credentials to URLs of other hosts.

## Pros and Cons of the Options

### Per fetcher, with limiters shared per provider

The current approach, with one limiter instance per provider (e.g. `ProviderLimits.CROSSREF`) instead of one per fetcher class.

* Good, because each fetcher's behaviour is visible in its code, and no global state is needed.
* Good, because it needs no change to `URLDownload`.
* Bad, because every fetcher author has to take a permit before every attempt, apply the provider's retry rules and add the identification, consistently with all other fetchers of the provider.
* Bad, because the parallel request limit has to be released by each fetcher after reading the body.

### Provider policy chosen by the caller and applied by `URLDownload`

```java
new URLDownload(url).withPolicy(ProviderPolicies.CROSSREF)
```

Typically set once in a fetcher's `getUrlDownload(URL)`.

* Good, because the rules are applied correctly on every attempt, without the fetcher author implementing them.
* Good, because the policy is visible at the call site.
* Bad, because a caller can forget to set the policy.
* Bad, because a policy is chosen before the request, so a redirect to another provider keeps the policy of the first one.

### Provider policy looked up by URL in `URLDownload`

```java
new URLDownload(url)   // URLDownload finds the policy for api.crossref.org
```

The registry matches the provider's API host and, where needed, a path (Crossref has different limits for `/works?query=…` and `/works/{doi}`). Hosts without a policy, e.g. for PDF downloads, are not throttled.

* Good, because every request through `URLDownload` gets its provider's rules, without any action by the caller.
* Good, because a redirect can use the policy of its target. This needs `URLDownload` to follow redirects itself: today `HttpURLConnection` follows redirects within the same protocol internally (`setInstanceFollowRedirects(true)`), so `URLDownload` only sees redirects that change the protocol.
* Neutral, because the policies read the API keys from the same `ImporterPreferences` instance the fetchers use today. It is registered once (like `ProxyRegisterer`) instead of passed to each fetcher.
* Bad, because the applied rules are not visible at the call site. A debug log has to show which policy was applied.
* Neutral, because a caller that needs different rules for the same endpoint needs an override (`withPolicy`, `withoutPolicy`).

## More Information

The draft PR contains a proof of concept of the lookup option with Resilience4j for Crossref: one hook in `URLDownload.openConnection()`, a Crossref policy that adds `mailto` and selects the limits from the resulting URL, and the API keys registered in `Launcher` and `JabKitLauncher`.
