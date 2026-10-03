package org.jabref.logic.citedrive;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

// [utest->req~citedrive.login~1]
class OAuthSessionRegistryTest {

    private final OAuthSessionRegistry registry = new OAuthSessionRegistry();

    @Test
    void completeDeliversCodeOfMatchingState() throws Exception {
        CompletableFuture<String> first = registry.register("first");
        CompletableFuture<String> second = registry.register("second");

        registry.complete("second", "code-2");

        assertEquals("code-2", second.get());
        assertFalse(first.isDone());
    }

    @Test
    void abandonedLoginTimesOutAndIsRemoved() {
        CompletableFuture<String> future = registry.register("state", 10, TimeUnit.MILLISECONDS);

        ExecutionException exception = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));

        assertInstanceOf(TimeoutException.class, exception.getCause());
        assertFalse(registry.isPending("state"));
    }
}
