package org.jabref.gui;

import java.nio.file.Path;

import javafx.concurrent.Task;
import javafx.scene.control.ProgressBar;

import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.util.strings.StringUtil;

import com.dlsc.gemsfx.infocenter.Notification;
import com.dlsc.gemsfx.infocenter.NotificationAction;
import com.dlsc.gemsfx.infocenter.NotificationView;

public class Notifications {
    private Notifications() {
    }

    public static class FileNotification extends Notification<Path> {
        public FileNotification(String title, String description) {
            super(title, description);
            setOnClick(_ -> OnClickBehaviour.NONE);
        }
    }

    public static class PreviewNotification extends Notification<PreviewLayout> {
        public PreviewNotification(PreviewLayout previewLayout) {
            super(Localization.lang("Preview style changed"), previewLayout.getDisplayName());
            setOnClick(_ -> OnClickBehaviour.REMOVE);
        }
    }

    public static class UndefinedNotification extends Notification<Object> {
        public UndefinedNotification(String title, String description) {
            super(title, description);
            setOnClick(_ -> OnClickBehaviour.REMOVE);
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
            task.setOnSucceeded(_ -> finishTask());
            task.setOnFailed(_ -> finishTask());
            task.setOnCancelled(_ -> finishTask());
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
            setContent(progressBar);
            setShowContent(true);
        }
    }
}
