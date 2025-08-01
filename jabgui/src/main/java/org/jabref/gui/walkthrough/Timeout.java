package org.jabref.gui.walkthrough;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

/// Executes a runnable after a specified duration. It uses a [PauseTransition] to
/// ensure the runnable is executed on the JavaFX Application Thread.
public class Timeout {
    private final PauseTransition transition;

    public Timeout(Duration duration, Runnable onFinished) {
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
