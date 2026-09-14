package org.jabref.gui;

import java.io.IOException;
import java.util.List;
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

@ExtendWith(JavaFxExtension.class)
@NullMarked
class NotificationsTest {

    private static final Callable<Void> FAILING = () -> {
        throw new IOException("connection refused");
    };
    private static final Callable<Void> SUCCEEDING = () -> null;

    private final NotificationGroup<Task<?>, Notifications.TaskNotification> group = new NotificationGroup<>("Tasks");

    @Test
    void failedTaskShowingFailureItselfIsRemoved() {
        runToEnd(BackgroundTask.wrap(FAILING).setTitle("Task").showsFailureToUser());

        assertEquals(List.of(), group.getNotifications());
    }

    @Test
    void failedTaskNotShowingFailureShowsErrorWithoutExceptionMessage() {
        Notifications.TaskNotification notification = runToEnd(BackgroundTask.wrap(FAILING).setTitle("Task"));

        assertEquals(List.of(notification), group.getNotifications());
        assertEquals(Notification.Type.ERROR, notification.getType());
        assertEquals("", notification.getSummary());
    }

    @Test
    void failedUntitledTaskIsRemoved() {
        runToEnd(BackgroundTask.wrap(FAILING).setTitle(""));

        assertEquals(List.of(), group.getNotifications());
    }

    @Test
    void cancelledTaskIsRemoved() {
        BackgroundTask<Void> backgroundTask = BackgroundTask.wrap(SUCCEEDING).setTitle("Task");
        Task<Void> task = UiTaskExecutor.getJavaFXTask(backgroundTask);
        addNotification(task, backgroundTask);

        task.cancel();
        flushJavaFXThread();

        assertEquals(List.of(), group.getNotifications());
    }

    @Test
    void succeededTaskStays() {
        Notifications.TaskNotification notification = runToEnd(BackgroundTask.wrap(SUCCEEDING).setTitle("Task"));

        assertEquals(List.of(notification), group.getNotifications());
    }

    @Test
    void succeededUntitledTaskIsRemoved() {
        runToEnd(BackgroundTask.wrap(SUCCEEDING).setTitle(""));

        assertEquals(List.of(), group.getNotifications());
    }

    private Notifications.TaskNotification runToEnd(BackgroundTask<Void> backgroundTask) {
        Task<Void> task = UiTaskExecutor.getJavaFXTask(backgroundTask);
        Notifications.TaskNotification notification = addNotification(task, backgroundTask);
        task.run();
        flushJavaFXThread();
        return notification;
    }

    private Notifications.TaskNotification addNotification(Task<Void> task, BackgroundTask<Void> backgroundTask) {
        Notifications.TaskNotification[] notification = new Notifications.TaskNotification[1];
        UiTaskExecutor.runAndWaitInJavaFXThread(() -> {
            notification[0] = new Notifications.TaskNotification(task, backgroundTask);
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
