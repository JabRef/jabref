package org.jabref.gui.walkthrough.utils;

import java.util.function.BooleanSupplier;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.stage.Window;
import javafx.util.Duration;

import com.sun.javafx.scene.NodeHelper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class WalkthroughUtils {
    public static final long DEFAULT_DEBOUNCE = 100L;

    public static boolean isNodeVisible(@Nullable Node node) {
        return node != null && NodeHelper.isTreeVisible(node);
    }

    public static boolean cannotPositionNode(@Nullable Node node) {
        return node == null || node.getScene() == null || !isNodeVisible(node) || node.getBoundsInLocal().isEmpty();
    }

    public interface DebouncedInvalidationListener extends InvalidationListener {
        /// Cancel any debounced InvalidationListener that's scheduled to run in the
        /// future.
        ///
        /// @implNote In other words, the implementation of debounced is NOT actually
        /// putting a counter of how many times the methods have executed, but rather to
        /// always delay the execution to specified intervals later, for which, only if
        /// during the specified interval no other tasks are scheduled, the operation
        /// proceeds. Therefore, you are likely interested in calling this method to
        /// prevent unwanted execution.
        void cancel();
    }

    /// Creates a debounced InvalidationListener that limits execution to at most once
    /// per interval. Uses JavaFX Timeline to ensure execution on JavaFX Application
    /// Thread.
    ///
    /// You are probably interested in calling the
    /// [DebouncedInvalidationListener#cancel()] methods when you clean up. Otherwise,
    /// there is a chance that this listener will run your methods after you removed it
    /// from all the properties that it is attached to.
    ///
    /// @param listener the listener to debounce
    /// @return a debounced listener
    public static DebouncedInvalidationListener debounced(InvalidationListener listener) {
        return debounced(listener, DEFAULT_DEBOUNCE);
    }

    /// Creates a debounced InvalidationListener that limits execution to at most once
    /// per interval. Uses JavaFX Timeline to ensure execution on JavaFX Application
    /// Thread.
    ///
    /// You are probably interested in calling the
    /// [DebouncedInvalidationListener#cancel()] methods when you clean up. Otherwise,
    /// there is a chance that this listener will run your methods after you removed it
    /// from all the properties that it is attached to.
    ///
    /// @param listener   the listener to debounce
    /// @param intervalMs the minimum interval between executions in milliseconds
    /// @return a debounced listener
    public static DebouncedInvalidationListener debounced(InvalidationListener listener, long intervalMs) {
        Timeline timeline = new Timeline();

        return new DebouncedInvalidationListener() {
            @Override
            public void cancel() {
                timeline.stop();
                timeline.getKeyFrames().clear();
            }

            @Override
            public void invalidated(Observable observable) {
                Runnable action = () -> listener.invalidated(observable);
                scheduleExecution(timeline, intervalMs, action);
            }
        };
    }

    public interface DebouncedRunnable extends Runnable {
        /// Cancel any debounced Runnable that's scheduled to run in the future.
        ///
        /// @implNote In other words, the implementation of debounced is NOT actually
        /// putting a counter of how many times the methods have executed, but rather to
        /// always delay the execution to specified intervals later, for which, only if
        /// during the specified interval no other tasks are scheduled, the operation
        /// proceeds. Therefore, you are likely interested in calling this method to
        /// prevent unwanted execution.
        void cancel();
    }

    /// Creates a debounced Runnable that limits execution to at most once per interval.
    /// Uses JavaFX Timeline to ensure execution on JavaFX Application Thread.
    ///
    /// You are probably interested in calling the [DebouncedRunnable#cancel()] methods
    /// when you clean up. Otherwise, there is a chance that this listener will run your
    /// methods after you removed it from all the properties that it is attached to.
    ///
    /// @param runnable the runnable to debounce
    /// @return a debounced runnable
    public static DebouncedRunnable debounced(Runnable runnable) {
        return debounced(runnable, DEFAULT_DEBOUNCE);
    }

    /// Creates a debounced Runnable that limits execution to at most once per interval.
    /// Uses JavaFX Timeline to ensure execution on JavaFX Application Thread.
    ///
    /// You are probably interested in calling the [DebouncedRunnable#cancel()] methods
    /// when you clean up. Otherwise, there is a chance that this listener will run your
    /// methods after you removed it from all the properties that it is attached to.
    ///
    /// @param runnable   the runnable to debounce
    /// @param intervalMs the minimum interval between executions in milliseconds
    /// @return a debounced runnable
    public static DebouncedRunnable debounced(Runnable runnable, long intervalMs) {
        Timeline timeline = new Timeline();
        return new DebouncedRunnable() {
            @Override
            public void cancel() {
                timeline.stop();
                timeline.getKeyFrames().clear();
            }

            @Override
            public void run() {
                scheduleExecution(timeline, intervalMs, runnable);
            }
        };
    }

    private static void scheduleExecution(Timeline timeline, long intervalMs, Runnable action) {
        timeline.stop();
        timeline.getKeyFrames().setAll(new KeyFrame(Duration.millis(intervalMs), _ -> action.run()));
        timeline.play();
    }

    /// Attaches a listener to the global window list that fires on every window change
    /// until a stop condition is met.
    ///
    /// @param stopCondition A supplier that should return true when the listener should
    ///                                           be detached (as well as run anything interesting for the
    ///                                           actual callee).
    /// @return A runnable that can be used to detach the listener prematurely.
    public static Runnable onWindowChangedUntil(@NonNull BooleanSupplier stopCondition) {
        ListChangeListener<Window> listener = new ListChangeListener<>() {
            @Override
            public void onChanged(Change<? extends Window> change) {
                while (change.next()) {
                    if (change.wasAdded() || change.wasRemoved()) {
                        if (stopCondition.getAsBoolean()) {
                            Window.getWindows().removeListener(this);
                            break;
                        }
                    }
                }
            }
        };
        Window.getWindows().addListener(listener);
        return () -> Window.getWindows().removeListener(listener);
    }
}
