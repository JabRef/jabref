package org.jabref.gui.testutils;

import javafx.stage.Stage;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.extension.ExtendWith;

/// Base class for tests that need JavaFX-thread interaction and a stage.
@NullMarked
@ExtendWith(JavaFxExtension.class)
public abstract class JavaFxTest {

    public void start(Stage stage) {
    }

    protected final void interact(Runnable action) {
        JavaFxExtension.runAndWait(action);
    }

    protected final void awaitEvents() {
        interact(() -> {
        });
    }
}
