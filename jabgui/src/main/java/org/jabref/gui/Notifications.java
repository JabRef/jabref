package org.jabref.gui;

import java.nio.file.Path;
import java.util.Optional;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.DelayedExecution;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

import com.dlsc.gemsfx.infocenter.InfoCenterEvent;
import com.dlsc.gemsfx.infocenter.Notification;
import com.dlsc.gemsfx.infocenter.Notification.OnClickBehaviour;
import com.dlsc.gemsfx.infocenter.NotificationAction;
import com.dlsc.gemsfx.infocenter.NotificationView;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Notifications {
    private static final Logger LOGGER = LoggerFactory.getLogger(Notifications.class);

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
        private final Runnable onDismissForever;

        public DonationNotification(String title, String description, Runnable onDismissForever) {
            super(title, description);
            this.onDismissForever = onDismissForever;
            setOnClick(_ -> OnClickBehaviour.NONE);
        }

        public Runnable getOnDismissForever() {
            return onDismissForever;
        }
    }

    @NullMarked
    public static class DonationNotificationView extends NotificationView<Object, DonationNotification> {
        public DonationNotificationView(DonationNotification notification) {
            super(notification);
            getStyleClass().add("donation-notification");
            setGraphic(IconTheme.JabRefIcons.DONATE.getGraphicNode());

            // GemsFX renders all actions as equally sized buttons; "Dismiss forever" should be less prominent,
            // so it is added as a link below the actions box instead of being a NotificationAction.
            Hyperlink dismissForever = new Hyperlink(Localization.lang("Dismiss forever"));
            dismissForever.getStyleClass().add("dismiss-forever-link");
            dismissForever.visibleProperty().bind(notification.expandedProperty());
            dismissForever.managedProperty().bind(notification.expandedProperty());
            dismissForever.setOnAction(_ -> {
                fireEvent(new InfoCenterEvent(InfoCenterEvent.HIDE, notification));
                notification.getOnDismissForever().run();
                notification.remove();
            });
            if (lookup(".text-container") instanceof VBox textContainer) {
                textContainer.getChildren().add(dismissForever);
            } else {
                // GemsFX internals changed; keep the option reachable as a regular action button
                LOGGER.warn("Could not find GemsFX notification text container; showing 'Dismiss forever' as button");
                notification.getActions().addFirst(new NotificationAction<>(Localization.lang("Dismiss forever"), _ -> {
                    notification.getOnDismissForever().run();
                    return OnClickBehaviour.HIDE_AND_REMOVE;
                }));
            }
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
