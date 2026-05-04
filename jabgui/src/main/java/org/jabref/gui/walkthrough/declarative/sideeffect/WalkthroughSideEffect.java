package org.jabref.gui.walkthrough.declarative.sideeffect;

import java.util.List;

import javafx.beans.Observable;

import org.jabref.gui.walkthrough.Walkthrough;

import org.jspecify.annotations.NonNull;

/// Walkthrough side effects that can be executed between steps.
public interface WalkthroughSideEffect {
    int DEFAULT_TIMEOUT_MS = 2500;

    /// Expected condition that determines when this side effect can be executed.
    @NonNull
    ExpectedCondition expectedCondition();

    /// List of observables that should trigger re-evaluation of the expected
    /// condition.
    @NonNull
    default List<Observable> dependencies() {
        return List.of();
    }

    /// Timeout in milliseconds for waiting for the expected condition to become true.
    /// Defaults to 2500ms if not overridden.
    default long timeoutMs() {
        return DEFAULT_TIMEOUT_MS;
    }

    /// Forward effect (what should be done).
    boolean forward(@NonNull Walkthrough walkthrough);

    /// Backward effect (what should be done when going back).
    boolean backward(@NonNull Walkthrough walkthrough);

    /// Gets a description of what this side effect does (for logging and debugging).
    @NonNull
    String description();
}
