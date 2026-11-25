package org.jabref.gui.desktop.os;

import java.io.IOException;

import org.jabref.gui.DialogService;
import org.jabref.gui.Notifications;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.logic.l10n.Localization;

import com.dlsc.gemsfx.infocenter.NotificationAction;
import org.jspecify.annotations.NullMarked;
import org.slf4j.LoggerFactory;

/// Keeps a link reachable after it was handed to the browser: [javafx.application.HostServices] does not report whether a browser started.
/// Offers to copy the link and, optionally, to open it again.
@NullMarked
public class LinkNotification extends Notifications.UiNotification {

    private final String link;
    private final DialogService dialogService;
    private final OnClickBehaviour afterAction;

    /// @param afterAction whether the notification stays after one of its actions was used
    public LinkNotification(String title,
                            String description,
                            String link,
                            DialogService dialogService,
                            ClipBoardManager clipBoardManager,
                            OnClickBehaviour afterAction) {
        super(title, description);
        this.link = link;
        this.dialogService = dialogService;
        this.afterAction = afterAction;
        // Clicking the text must not remove the notification, so that the actions remain available
        setOnClick(_ -> OnClickBehaviour.NONE);
        getActions().add(new NotificationAction<>(Localization.lang("Copy link"), _ -> {
            clipBoardManager.setContent(link);
            dialogService.notify(Localization.lang("The link has been copied to the clipboard."));
            return afterAction;
        }));
    }

    /// Adds an action in front of "Copy link" that opens the link in the browser.
    public LinkNotification withOpenAction(String label, NativeDesktop nativeDesktop) {
        getActions().addFirst(new NotificationAction<>(label, _ -> {
            try {
                nativeDesktop.openBrowser(link);
            } catch (IOException e) {
                LoggerFactory.getLogger(LinkNotification.class).warn("Could not open {}", link, e);
                dialogService.notify(Localization.lang("Could not open browser."));
                // Keep the notification, so that the link can still be copied
                return OnClickBehaviour.NONE;
            }
            return afterAction;
        }));
        return this;
    }
}
