package org.jabref.gui.push;

import org.jabref.gui.actions.Action;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.logic.push.PushToApplication;
import org.jabref.logic.push.PushToApplicationPreferences;

public interface GUIPushToApplication extends PushToApplication {

    /**
     * Gets a tooltip for the push operation.
     */
    String getTooltip();

    /**
     * Gets the icon associated with the application.
     *
     * @return The icon for the application.
     */
    JabRefIcon getApplicationIcon();

    Action getAction();

    PushToApplicationSettings getSettings(PushToApplication application, PushToApplicationPreferences pushToApplicationPreferences);

    void sendErrorNotification(String title, String message);
}
