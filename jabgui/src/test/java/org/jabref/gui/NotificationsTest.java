package org.jabref.gui;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

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

// [utest->req~ux.notifications.finished-task~1]
@ExtendWith(JavaFxExtension.class)
@NullMarked
class NotificationsTest {

    private static final Callable<Void> FAILING = () -> {
        throw new IOException("connection refused");
    };
    private static final Runnable SUCCEEDING = () -> {
    };

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
        AtomicReference<Optional<Notifications.TaskNotification>> notification = new AtomicReference<>(Optional.empty());
        UiTaskExecutor.runAndWaitInJavaFXThread(() -> {
            Notifications.TaskNotification created = new Notifications.TaskNotification(task, backgroundTask);
            group.getNotifications().add(created);
            notification.set(Optional.of(created));
        });
        return notification.get().orElseThrow();
    }

    /// Worker state events are posted to the JavaFX thread; waiting for an empty action lets them run first.
    private static void flushJavaFXThread() {
        UiTaskExecutor.runAndWaitInJavaFXThread(() -> {
        });
    }
}
