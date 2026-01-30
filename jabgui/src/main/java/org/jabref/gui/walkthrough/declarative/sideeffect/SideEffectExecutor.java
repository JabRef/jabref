package org.jabref.gui.walkthrough.declarative.sideeffect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javafx.animation.PauseTransition;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.util.Duration;

import org.jabref.gui.DialogService;
import org.jabref.gui.walkthrough.Walkthrough;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SideEffectExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SideEffectExecutor.class);

    private @Nullable PauseTransition timeoutTransition;
    private @Nullable InvalidationListener dependencyListener;
    private final List<Observable> currentDependencies = new ArrayList<>();

    /// Executes a side effect's forward action, waiting for the expected condition if
    /// necessary.
    ///
    /// @param sideEffect  the side effect to execute
    /// @param walkthrough the walkthrough context
    /// @return true if the side effect was executed successfully, false otherwise
    public boolean executeForward(@NonNull WalkthroughSideEffect sideEffect, @NonNull Walkthrough walkthrough) {
        return execute(sideEffect, walkthrough, true);
    }

    /// Executes a side effect's backward action.
    ///
    /// @param sideEffect  the side effect to execute
    /// @param walkthrough the walkthrough context
    /// @return true if the side effect was executed successfully, false otherwise
    public boolean executeBackward(@NonNull WalkthroughSideEffect sideEffect, @NonNull Walkthrough walkthrough) {
        return execute(sideEffect, walkthrough, false);
    }

    private boolean execute(@NonNull WalkthroughSideEffect sideEffect, @NonNull Walkthrough walkthrough, boolean forward) {
        LOGGER.debug("Executing {} effect: {}", forward ? "forward" : "backward", sideEffect.description());

        try {
            if (forward) {
                boolean conditionMet = waitForCondition(sideEffect);
                if (!conditionMet) {
                    LOGGER.warn("Expected condition not met for side effect: {}", sideEffect.description());
                    notifyUser(Localization.lang("Side effect timeout"),
                            Localization.lang("The condition for '%0' was not met within the timeout period.", sideEffect.description()));
                    return false;
                }
                return sideEffect.forward(walkthrough);
            } else {
                return sideEffect.backward(walkthrough);
            }
        } catch (Exception e) {
            LOGGER.error("Error executing {} effect: {}", forward ? "forward" : "backward", sideEffect.description(), e);
            notifyUser(Localization.lang("Walkthrough side effect error"),
                    Localization.lang("An error occurred while executing '%0': %1", sideEffect.description(), e.getMessage()));
            return false;
        } finally {
            cleanUp();
        }
    }

    private boolean waitForCondition(@NonNull WalkthroughSideEffect sideEffect) {
        ExpectedCondition condition = sideEffect.expectedCondition();

        if (condition.evaluate()) {
            LOGGER.debug("Expected condition already met for: {}", sideEffect.description());
            return true;
        }

        CompletableFuture<Boolean> conditionFuture = new CompletableFuture<>();
        startTimeout(sideEffect.timeoutMs(), () -> {
            LOGGER.debug("Timeout reached for side effect: {}", sideEffect.description());
            conditionFuture.complete(false);
        });

        setupDependencyMonitoring(sideEffect.dependencies(), () -> {
            if (condition.evaluate()) {
                LOGGER.debug("Expected condition met via dependency change for: {}", sideEffect.description());
                conditionFuture.complete(true);
            }
        });

        try {
            return conditionFuture.get(sideEffect.timeoutMs() + 100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.debug("Error waiting for condition: {}", e.getMessage(), e);
            return false;
        }
    }

    private void setupDependencyMonitoring(@NonNull List<Observable> dependencies, @NonNull Runnable onDependencyChange) {
        cleanUpDependencies();

        dependencyListener = _ -> onDependencyChange.run();

        for (Observable dependency : dependencies) {
            dependency.addListener(dependencyListener);
            currentDependencies.add(dependency);
        }
    }

    private void startTimeout(long timeoutMs, @NonNull Runnable onTimeout) {
        cancelTimeout();

        timeoutTransition = new PauseTransition(Duration.millis(timeoutMs));
        timeoutTransition.setOnFinished(_ -> onTimeout.run());
        timeoutTransition.play();
    }

    private void cancelTimeout() {
        if (timeoutTransition != null) {
            timeoutTransition.stop();
            timeoutTransition = null;
        }
    }

    private void cleanUpDependencies() {
        if (dependencyListener != null) {
            for (Observable dependency : currentDependencies) {
                dependency.removeListener(dependencyListener);
            }
            currentDependencies.clear();
            dependencyListener = null;
        }
    }

    private void cleanUp() {
        cancelTimeout();
        cleanUpDependencies();
    }

    private void notifyUser(@NonNull String title, @NonNull String message) {
        try {
            DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
            dialogService.notify(title + message);
        } catch (Exception e) {
            LOGGER.error("Failed to notify user about side effect issue", e);
        }
    }
}
