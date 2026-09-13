package org.jabref.gui;

import java.io.IOException;
import java.util.concurrent.Callable;

import javafx.concurrent.Task;

import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.util.BackgroundTask;

import com.dlsc.gemsfx.infocenter.Notification;
import com.dlsc.gemsfx.infocenter.NotificationGroup;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(JavaFxExtension.class)
@NullMarked
class NotificationsTest {

    private static final Callable<Void> FAILING = () -> {
        throw new IOException("connection refused");
    };

    private final NotificationGroup<Task<?>, Notifications.TaskNotification> group = new NotificationGroup<>("Tasks");

    @Test
    void failedTaskWithReportedFailureIsRemoved() {
        Notifications.TaskNotification notification = runToEnd(BackgroundTask.wrap(FAILING).setTitle("Task").onFailure(_ -> {
        }));

        assertFalse(group.getNotifications().contains(notification));
    }

    @Test
    void failedTaskWithOnlyOnFinishedShowsError() {
        Notifications.TaskNotification notification = runToEnd(BackgroundTask.wrap(FAILING).setTitle("Task").onFinished(() -> {
        }));

        assertTrue(group.getNotifications().contains(notification));
        assertEquals(Notification.Type.ERROR, notification.getType());
        assertEquals("connection refused", notification.getSummary());
    }

    @Test
    void cancelledTaskIsRemoved() {
        Task<Void> task = UiTaskExecutor.getJavaFXTask(BackgroundTask.wrap(() -> (Void) null).setTitle("Task"));
        Notifications.TaskNotification notification = addNotification(task);

        task.cancel();
        flushJavaFXThread();

        assertFalse(group.getNotifications().contains(notification));
    }

    @Test
    void succeededTaskStays() {
        Notifications.TaskNotification notification = runToEnd(BackgroundTask.wrap(() -> (Void) null).setTitle("Task"));

        assertTrue(group.getNotifications().contains(notification));
    }

    private Notifications.TaskNotification runToEnd(BackgroundTask<Void> backgroundTask) {
        Task<Void> task = UiTaskExecutor.getJavaFXTask(backgroundTask);
        Notifications.TaskNotification notification = addNotification(task);
        task.run();
        flushJavaFXThread();
        return notification;
    }

    private Notifications.TaskNotification addNotification(Task<Void> task) {
        Notifications.TaskNotification[] notification = new Notifications.TaskNotification[1];
        UiTaskExecutor.runAndWaitInJavaFXThread(() -> {
            notification[0] = new Notifications.TaskNotification(task);
            group.getNotifications().add(notification[0]);
        });
        return notification[0];
    }

    /// Worker state events and the removal are each posted to the JavaFX thread; waiting twice lets both run.
    private static void flushJavaFXThread() {
        UiTaskExecutor.runAndWaitInJavaFXThread(() -> {
        });
        UiTaskExecutor.runAndWaitInJavaFXThread(() -> {
        });
    }
}
