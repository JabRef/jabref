---
nav_order: 82
parent: Decision Records
status: proposed
date: 2026-11-15
---
# Choose a library for rate limiting and retries of provider requests

`adr~fetchers.resilience-library~1`

Needs: impl

## Context and Problem Statement

JabRef has to stay below the request limits of the providers it queries (`req~fetchers.rate-limiting~1`) and retries requests rejected with HTTP 429 (`req~fetchers.identifier-rate-limit-retries~1`).
Today it uses Guava's `RateLimiter` (wrapped in `FetcherRateLimiter`) and its own `FetcherRetry`.

Providers need more than a smooth rate:

* limits on parallel requests (Crossref: 1 or 3, depending on the pool, see [Crossref's announcement](https://www.crossref.org/blog/announcing-changes-to-rest-api-rate-limits/)),
* limits per longer interval (Software Heritage, queried by `SwhidFetcher`: 120 per hour),
* rates that change at runtime (`DoiFetcher.updateCrossrefAPIRate` reads `X-Rate-Limit-*` headers and calls `setRate`),
* retry delays taken from the response (`Retry-After`).

Which library should JabRef use for these policies?
Where the policies are applied is decided in [ADR 0083](0083-place-provider-rules-in-provider-policies.md).

## Decision Drivers

* Covers rate limits per interval, parallel request limits, retries with a delay computed from the failure, and rate changes at runtime.
* Can bound the total time a request may take including retries, so that interactive use does not hang.
* Fits JabRef's module build, which rejects automatic modules (`failOnAutomaticModules = true` in `build-logic`).
* Small dependency footprint and an actively maintained project.

## Considered Options

* Resilience4j
* Failsafe
* Extend JabRef's own code (Guava `RateLimiter`, `Semaphore`, `FetcherRetry`)

## Decision Outcome

To be decided in the maintainer call. The assessment depends on two questions:

* **Is a rate that adapts to response headers still needed?** `DoiFetcher` does this today. Crossref now documents its limits, so fixed limits may be enough. If adapting is needed, Failsafe is the weakest option, because its `RateLimiter` cannot change its rate.
* **Is a hard limit on the total time needed?** The requirement only asks for bounded backoff. If a hard total limit is needed, Resilience4j is the weakest option, because its `Retry` has none.

If neither is needed, the choice is between maintenance (Resilience4j) and footprint and build fit (Failsafe).
Extending JabRef's own code is reasonable as long as the set of policies stays small (rate, parallel requests, retry on 429).

### Confirmation

An ArchUnit rule forbids the rate limiting and retry classes that are replaced, once the migration is complete.

## Pros and Cons of the Options

### Resilience4j

[Resilience4j](https://resilience4j.readme.io) 2.4.0, modules `resilience4j-ratelimiter`, `-retry`, `-bulkhead` and `-core`. `resilience4j-all` is not needed and would pull in Micrometer and the JCache API.

```java
RateLimiter limiter = RateLimiter.of("crossref", RateLimiterConfig.custom()
        .limitForPeriod(3).limitRefreshPeriod(Duration.ofSeconds(1)).build());
limiter.changeLimitForPeriod(5);   // adapt at runtime
```

* Good, because it covers rate limits per period, parallel request limits (`Bulkhead`) and retries with a delay computed from the failure (`intervalBiFunction`).
* Good, because the rate can change at runtime (`changeLimitForPeriod`).
* Good, because it is actively maintained (releases in 2023, 2025 and 2026; commits in October 2026).
* Bad, because `Retry` has no total time limit. The time is bounded only by the number of attempts, the backoff, and the maximum wait for a permit on each attempt.
* Bad, because the four modules only have an `Automatic-Module-Name`, so each needs an entry in JabRef's `extraJavaModuleInfo` configuration.
* Bad, because it adds four artifacts (about 250 KB).

### Failsafe

[Failsafe](https://failsafe.dev) 3.3.2, one artifact.

```java
RateLimiter<Object> limiter = RateLimiter.burstyBuilder(3, Duration.ofSeconds(1)).build();
Failsafe.with(retryPolicy, bulkhead, limiter).get(() -> download());
```

* Good, because it covers rate limits per period (`burstyBuilder`) or smooth (`smoothBuilder`), parallel request limits (`Bulkhead`) and retries with a delay computed from the failure (`withDelayFn`).
* Good, because `RetryPolicy.withMaxDuration` limits the total time, like today's `FetcherRetryPolicy`.
* Good, because it is one artifact (144 KB) without dependencies and with a real `module-info` (`dev.failsafe.core`).
* Bad, because a `RateLimiter` cannot change its rate. Adapting to response headers means replacing the limiter instance.
* Bad, because the last release is from June 2023. Since then there were only CI and test commits.

### Extend JabRef's own code (Guava `RateLimiter`, `Semaphore`, `FetcherRetry`)

```java
CROSSREF_RATE.acquire(url.toString());          // Guava RateLimiter, smooth rate
CROSSREF_PARALLEL.acquire();                     // java.util.concurrent.Semaphore
FetcherRetry.executeWithRetryPolicy(policy, () -> download());
```

* Good, because no new dependency is needed and the existing code is tested.
* Good, because `FetcherRetryPolicy` already limits the total time, and Guava's `RateLimiter` can change its rate (`setRate`).
* Good, because a parallel request limit is a `Semaphore`, and reading `Retry-After` is a small addition to `FetcherRetry`.
* Neutral, because Guava's `RateLimiter` only spreads requests evenly. It never exceeds a limit. With 120 per hour, a single request is sent at once, but several requests in a row are sent 30 s apart, where a limit per interval would send them at once.
* Bad, because every further policy (e.g. a circuit breaker) has to be written, tested and maintained by JabRef.
* Bad, because Guava's `RateLimiter` is marked `@Beta`.

## More Information

* `SimpleHttpResponse` does not carry response headers today. Reading `Retry-After` needs this change with every option.
