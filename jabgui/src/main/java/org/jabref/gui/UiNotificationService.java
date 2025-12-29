package org.jabref.gui;

import org.jabref.logic.util.NotificationService;

import com.dlsc.gemsfx.infocenter.Notification;

public interface UiNotificationService extends NotificationService {

    /**
     * Notify the user in a non-blocking way (e.g. a toast).
     *
     * @param notification the message to show.
     */
    void notify(Notification<Object> notification);
}
