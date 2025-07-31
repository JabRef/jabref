package org.jabref.gui.walkthrough;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.scene.Node;
import javafx.util.Duration;

import com.sun.javafx.scene.NodeHelper;
import org.jspecify.annotations.Nullable;

public class WalkthroughUtils {
    public static boolean isNodeVisible(@Nullable Node node) {
        return node != null && NodeHelper.isTreeVisible(node);
    }

    public static boolean cannotPositionNode(@Nullable Node node) {
        return node == null || node.getScene() == null || !isNodeVisible(node) || node.getBoundsInLocal().isEmpty();
    }

    /// Creates a runnable that executes only once, preventing subsequent executions.
    /// Thread-safe implementation using atomic operations.
    ///
    /// @param runnable the runnable to execute once
    /// @return a runnable that will only execute the original runnable once
   public static Runnable once(Runnable runnable) {
        CountDownLatch latch = new CountDownLatch(1);
        return () -> {
            if (latch.getCount() > 0) {
                latch.countDown();
                runnable.run();
            }
        };
    }

    /// Creates a debounced InvalidationListener that limits execution to at most once
    /// per interval. Uses JavaFX Timeline to ensure execution on JavaFX Application
    /// Thread.
    ///
    /// @param listener   the listener to debounce
    /// @param intervalMs the minimum interval between executions in milliseconds
    /// @return a debounced listener
    public static InvalidationListener debounced(InvalidationListener listener, long intervalMs) {
        AtomicBoolean pending = new AtomicBoolean(false);
        Timeline timeline = new Timeline();

        return observable -> {
            if (!pending.compareAndSet(false, true)) {
                return;
            }

            timeline.stop();
            timeline.getKeyFrames().clear();

            KeyFrame keyFrame = new KeyFrame(
                    Duration.millis(intervalMs),
                    _ -> {
                        try {
                            listener.invalidated(observable);
                        } finally {
                            pending.set(false);
                        }
                    }
            );

            timeline.getKeyFrames().add(keyFrame);
            timeline.play();
        };
    }

    /// Creates a debounced Runnable that limits execution to at most once per
    /// interval.
    ///
    /// @param runnable   the runnable to debounce
    /// @param intervalMs the minimum interval between executions in milliseconds
    /// @return a debounced runnable
    public static Runnable debounced(Runnable runnable, long intervalMs) {
        AtomicBoolean pending = new AtomicBoolean(false);
        Timeline timeline = new Timeline();

        return () -> {
            if (!pending.compareAndSet(false, true)) {
                return;
            }

            timeline.stop();
            timeline.getKeyFrames().clear();

            KeyFrame keyFrame = new KeyFrame(
                    Duration.millis(intervalMs),
                    _ -> {
                        try {
                            runnable.run();
                        } finally {
                            pending.set(false);
                        }
                    }
            );

            timeline.getKeyFrames().add(keyFrame);
            timeline.play();
        };
    }

    /// Creates a runnable that prevents concurrent execution but allows retries until
    /// success. Once the task succeeds, all future executions are blocked permanently.
    /// Thread-safe implementation using atomic operations.
    ///
    /// @param task          the task to execute
    /// @param wasSuccessful supplier that returns true if the task succeeded and no
    ///                      more executions should be allowed
    /// @return a runnable that can be retried until success, preventing concurrent
    ///         executions
    public static Runnable retryableOnce(Runnable task, Supplier<Boolean> wasSuccessful) {
        AtomicBoolean noExecution = new AtomicBoolean(false);

        return () -> {
            if (!noExecution.compareAndSet(false, true)) {
                return; // No concurrent executions
            }

            task.run();

            if (!wasSuccessful.get()) {
                noExecution.set(false); // Reset for next execution
            }
        };
    }
}
