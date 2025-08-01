package org.jabref.gui.walkthrough;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.stage.Window;
import javafx.util.Duration;

import com.sun.javafx.scene.NodeHelper;
import org.jspecify.annotations.NonNull;
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

    /// Attaches a listener to the global window list that fires on every window change
    /// until a stop condition is met.
    ///
    /// @param onEvent       The runnable to execute when a window change is detected.
    /// @param stopCondition A supplier that should return true when the listener should be detached.
    /// @return A runnable that can be used to detach the listener prematurely.
    public static Runnable onWindowChangedUntil(@NonNull Runnable onEvent, @NonNull Supplier<Boolean> stopCondition) {
        ListChangeListener<Window> listener = new ListChangeListener<>() {
            @Override
            public void onChanged(Change<? extends Window> change) {
                while (change.next()) {
                    if (change.wasAdded() || change.wasRemoved()) {
                        onEvent.run();
                        if (stopCondition.get()) {
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

    /// Attaches a listener to the global window list that fires once when a window is
    /// added or removed, then immediately detaches itself.
    ///
    /// @param onNavigate The runnable to execute when a window change is detected.
    /// @return A runnable that can be used to detach the listener prematurely.
    public static Runnable onWindowChangedOnce(Runnable onNavigate) {
        AtomicBoolean navigated = new AtomicBoolean(false);
        Runnable onNavigateOnce = () -> {
            if (navigated.compareAndSet(false, true)) {
                onNavigate.run();
            }
        };
        return onWindowChangedUntil(onNavigateOnce, navigated::get);
    }
}
