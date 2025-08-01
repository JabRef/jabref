package org.jabref.gui.util;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

import org.jspecify.annotations.NonNull;

/// Executes a runnable after a specified duration. It uses a [PauseTransition] to
/// ensure the runnable is executed on the JavaFX Application Thread.
public class DelayedExecution {
    private final PauseTransition transition;

    public DelayedExecution(@NonNull Duration duration, @NonNull Runnable onFinished) {
        this.transition = new PauseTransition(duration);
        this.transition.setOnFinished(_ -> onFinished.run());
    }

    public void start() {
        transition.playFromStart();
    }

    public void cancel() {
        transition.stop();
    }
}
