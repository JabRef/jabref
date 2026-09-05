package org.jabref.gui.testutils;

import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(JavaFxExtension.class)
@NullMarked
class JavaFxExtensionTest {

    @Test
    void cleansUpMultipleWindowsAfterTest() {
        JavaFxExtension.invokeAndWait(() -> {
            new Stage().show();
            new Stage().show();

            assertEquals(2, Window.getWindows().size());
        });
    }

    @Test
    void awaitEventsProcessesChainedEvents() {
        AtomicInteger completedEvents = new AtomicInteger();
        Platform.runLater(() -> queueEvents(completedEvents, 5));

        JavaFxExtension.awaitEvents();

        assertEquals(5, completedEvents.get());
    }

    @Test
    void awaitEventsReportsAsynchronousFailures() {
        Platform.runLater(() -> {
            throw new AssertionError("Expected JavaFX failure");
        });

        AssertionError exception = assertThrows(AssertionError.class, JavaFxExtension::awaitEvents);

        assertEquals("JavaFX asynchronous action failed", exception.getMessage());
    }

    private static void queueEvents(AtomicInteger completedEvents, int remainingEvents) {
        completedEvents.incrementAndGet();
        if (remainingEvents > 1) {
            Platform.runLater(() -> queueEvents(completedEvents, remainingEvents - 1));
        }
    }
}
