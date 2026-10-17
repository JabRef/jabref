package org.jabref.logic.citedrive;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Pending OAuth logins, keyed by the OAuth `state` parameter
@NullMarked
public class OAuthSessionRegistry {

    /// Time the user has to finish logging in in the browser
    static final long TIMEOUT_MINUTES = 10;

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthSessionRegistry.class);

    private final Map<String, CompletableFuture<String>> pending = new ConcurrentHashMap<>();

    /// @return the authorization code; fails with a `TimeoutException` if no callback arrives in time
    public CompletableFuture<String> register(String state) {
        return register(state, TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    CompletableFuture<String> register(String state, long timeout, TimeUnit unit) {
        CompletableFuture<String> future = new CompletableFuture<>();
        pending.put(state, future);
        future.orTimeout(timeout, unit)
              .whenComplete((_, _) -> pending.remove(state, future));
        return future;
    }

    /// @return false if no login is waiting for this state (unknown, timed out, or already completed)
    public boolean complete(String state, String code) {
        CompletableFuture<String> future = pending.remove(state);
        if (future == null) {
            LOGGER.warn("No pending OAuth session for the received state");
            return false;
        }
        return future.complete(code);
    }

    public void fail(String state, Throwable t) {
        CompletableFuture<String> future = pending.remove(state);
        if (future != null) {
            future.completeExceptionally(t);
        } else {
            LOGGER.warn("No pending OAuth session for the received state (fail)");
        }
    }

    boolean isPending(String state) {
        return pending.containsKey(state);
    }
}
