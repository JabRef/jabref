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
    private static final Callable<Void> SUCCEEDING = () -> null;

    private final NotificationGroup<Task<?>, Notifications.TaskNotification> group = new NotificationGroup<>("Tasks");

    @Test
    void failedTaskReportedByCallerIsRemoved() {
        Notifications.TaskNotification notification = runToEnd(FAILING, "Task", true);

        assertFalse(group.getNotifications().contains(notification));
    }

    @Test
    void failedTaskNotReportedByCallerShowsErrorWithoutExceptionMessage() {
        Notifications.TaskNotification notification = runToEnd(FAILING, "Task", false);

        assertTrue(group.getNotifications().contains(notification));
        assertEquals(Notification.Type.ERROR, notification.getType());
        assertEquals("", notification.getSummary());
    }

    @Test
    void failedUntitledTaskIsRemoved() {
        Notifications.TaskNotification notification = runToEnd(FAILING, "", false);

        assertFalse(group.getNotifications().contains(notification));
    }

    @Test
    void cancelledTaskIsRemoved() {
        Task<Void> task = UiTaskExecutor.getJavaFXTask(BackgroundTask.wrap(SUCCEEDING).setTitle("Task"));
        Notifications.TaskNotification notification = addNotification(task, false);

        task.cancel();
        flushJavaFXThread();

        assertFalse(group.getNotifications().contains(notification));
    }

    @Test
    void succeededTaskStays() {
        Notifications.TaskNotification notification = runToEnd(SUCCEEDING, "Task", false);

        assertTrue(group.getNotifications().contains(notification));
    }

    @Test
    void succeededUntitledTaskIsRemoved() {
        Notifications.TaskNotification notification = runToEnd(SUCCEEDING, "", false);

        assertFalse(group.getNotifications().contains(notification));
    }

    private Notifications.TaskNotification runToEnd(Callable<Void> callable, String title, boolean failureReportedByCaller) {
        Task<Void> task = UiTaskExecutor.getJavaFXTask(BackgroundTask.wrap(callable).setTitle(title));
        Notifications.TaskNotification notification = addNotification(task, failureReportedByCaller);
        task.run();
        flushJavaFXThread();
        return notification;
    }

    private Notifications.TaskNotification addNotification(Task<Void> task, boolean failureReportedByCaller) {
        Notifications.TaskNotification[] notification = new Notifications.TaskNotification[1];
        UiTaskExecutor.runAndWaitInJavaFXThread(() -> {
            notification[0] = new Notifications.TaskNotification(task, failureReportedByCaller);
            group.getNotifications().add(notification[0]);
        });
        return notification[0];
    }

    /// Worker state events are posted to the JavaFX thread; waiting for an empty action lets them run first.
    private static void flushJavaFXThread() {
        UiTaskExecutor.runAndWaitInJavaFXThread(() -> {
        });
    }
}
