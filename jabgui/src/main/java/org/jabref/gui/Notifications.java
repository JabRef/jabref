package org.jabref.gui;

import java.nio.file.Path;
import java.util.Optional;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

import org.jabref.gui.util.DelayedExecution;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

import com.dlsc.gemsfx.infocenter.Notification;
import com.dlsc.gemsfx.infocenter.NotificationAction;
import com.dlsc.gemsfx.infocenter.NotificationView;

public class Notifications {
    private Notifications() {
    }

    public static class UndefinedNotification extends Notification<Object> {
        public UndefinedNotification(String title, String description) {
            super(title, description);
            setOnClick(_ -> OnClickBehaviour.REMOVE);
        }
    }

    public static class FileNotification extends Notification<Path> {

        public FileNotification(String title, String description) {
            super(title, description);
            setOnClick(_ -> OnClickBehaviour.NONE);
        }
    }

    public static class UiNotification extends Notification<Object> {
        public UiNotification(String title, String description) {
            super(title, description);
            setOnClick(_ -> OnClickBehaviour.REMOVE);
        }

        public UiNotification(String title, String description, Duration duration) {
            super(title, description);
            new DelayedExecution(duration, this::remove).start();
        }

        public UiNotification withAutoClose(Duration duration) {
            new DelayedExecution(duration, this::remove).start();
            return this;
        }
    }

    public static class TaskNotification extends Notification<Task<?>> {
        boolean undefinedTask = false;

        public TaskNotification(Task<?> task) {
            super(task.getTitle(), task.getMessage());
            setUserObject(task);
            if (StringUtil.isBlank(task.getTitle())) {
                setTitle(Localization.lang("Background task"));
                undefinedTask = true;
            }
            setOnClick(_ -> OnClickBehaviour.NONE);
            getActions().add(new NotificationAction<>(Localization.lang("Cancel"), _ -> {
                task.cancel();
                return OnClickBehaviour.REMOVE;
            }));

            // Do not overwrite existing handlers
            Optional<EventHandler<WorkerStateEvent>> onSucceeded = Optional.ofNullable(task.getOnSucceeded());
            task.setOnSucceeded(event -> {
                onSucceeded.ifPresent(handler -> handler.handle(event));
                finishTask();
            });
            Optional<EventHandler<WorkerStateEvent>> onFailed = Optional.ofNullable(task.getOnFailed());
            task.setOnFailed(event -> {
                onFailed.ifPresent(handler -> handler.handle(event));
                finishTask();
            });
            Optional<EventHandler<WorkerStateEvent>> onCancelled = Optional.ofNullable(task.getOnCancelled());
            task.setOnCancelled(event -> {
                onCancelled.ifPresent(handler -> handler.handle(event));
                finishTask();
            });
        }

        private void finishTask() {
            if (undefinedTask) {
                UiTaskExecutor.runInJavaFXThread(this::remove);
            }
            setOnClick(_ -> OnClickBehaviour.REMOVE);
            getActions().clear();
        }
    }

    public static class TaskNotificationView extends NotificationView<Task<?>, TaskNotification> {
        ProgressBar progressBar = new ProgressBar();

        public TaskNotificationView(TaskNotification notification) {
            super(notification);
            progressBar.progressProperty().bind(notification.getUserObject().progressProperty());
            HBox.setHgrow(progressBar, Priority.ALWAYS);
            setContent(progressBar);
            setShowContent(true);
        }
    }
}
