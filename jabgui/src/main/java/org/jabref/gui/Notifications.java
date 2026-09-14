package org.jabref.gui;

import java.nio.file.Path;
import java.util.Optional;

import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.DelayedExecution;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

import com.dlsc.gemsfx.infocenter.Notification;
import com.dlsc.gemsfx.infocenter.NotificationAction;
import com.dlsc.gemsfx.infocenter.NotificationView;
import org.jspecify.annotations.NullMarked;

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

    @NullMarked
    public static class DonationNotification extends Notification<Object> {
        public DonationNotification(String title, String description) {
            super(title, description);
            setOnClick(_ -> OnClickBehaviour.NONE);
        }
    }

    @NullMarked
    public static class DonationNotificationView extends NotificationView<Object, DonationNotification> {
        public DonationNotificationView(DonationNotification notification) {
            super(notification);
            getStyleClass().add("donation-notification");
            setGraphic(IconTheme.JabRefIcons.DONATE.getGraphicNode());
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
        private final boolean untitled;

        /// @param failureReportedByCaller whether the task's failure handler already shows the error, see [org.jabref.logic.util.BackgroundTask#reportsFailureToUser()]
        public TaskNotification(Task<?> task, boolean failureReportedByCaller) {
            super(task.getTitle(), task.getMessage());
            setUserObject(task);
            untitled = StringUtil.isBlank(task.getTitle());
            if (untitled) {
                setTitle(Localization.lang("Background task"));
            }
            setOnClick(_ -> OnClickBehaviour.NONE);
            getActions().add(new NotificationAction<>(Localization.lang("Cancel"), _ -> {
                task.cancel();
                return OnClickBehaviour.REMOVE;
            }));

            // Do not overwrite existing handlers
            // The handlers run on the JavaFX thread after the notification was added: it is created when the task starts running.
            Optional<EventHandler<WorkerStateEvent>> onSucceeded = Optional.ofNullable(task.getOnSucceeded());
            task.setOnSucceeded(event -> {
                onSucceeded.ifPresent(handler -> handler.handle(event));
                if (untitled) {
                    remove();
                } else {
                    markFinished();
                }
            });
            Optional<EventHandler<WorkerStateEvent>> onFailed = Optional.ofNullable(task.getOnFailed());
            task.setOnFailed(event -> {
                onFailed.ifPresent(handler -> handler.handle(event));
                if (untitled || failureReportedByCaller) {
                    remove();
                } else {
                    // Without full progress, the notification would otherwise look like the task is still running
                    setType(Type.ERROR);
                    markFinished();
                }
            });
            Optional<EventHandler<WorkerStateEvent>> onCancelled = Optional.ofNullable(task.getOnCancelled());
            task.setOnCancelled(event -> {
                onCancelled.ifPresent(handler -> handler.handle(event));
                remove();
            });
        }

        private void markFinished() {
            setOnClick(_ -> OnClickBehaviour.REMOVE);
            getActions().clear();
        }
    }

    public static class TaskNotificationView extends NotificationView<Task<?>, TaskNotification> {
        ProgressBar progressBar = new ProgressBar();

        public TaskNotificationView(TaskNotification notification) {
            super(notification);
            progressBar.progressProperty().bind(notification.getUserObject().progressProperty());
            progressBar.visibleProperty().bind(notification.getUserObject().stateProperty().isNotEqualTo(Worker.State.FAILED));
            progressBar.managedProperty().bind(progressBar.visibleProperty());
            HBox.setHgrow(progressBar, Priority.ALWAYS);
            setContent(progressBar);
            setShowContent(true);
        }
    }
}
