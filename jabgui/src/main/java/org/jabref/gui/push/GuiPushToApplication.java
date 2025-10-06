package org.jabref.gui.push;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.Action;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.push.PushToApplication;
import org.jabref.logic.push.PushToApplicationPreferences;

public interface GuiPushToApplication extends PushToApplication {

    /**
     * Gets a tooltip for the push operation.
     */
    default String getTooltip() {
        return Localization.lang("Push entries to external application (%0)", getDisplayName());
    }

    /**
     * Gets the icon associated with the application.
     *
     * @return The icon for the application.
     */
    JabRefIcon getApplicationIcon();

    default Action getAction() {
        return new GuiPushToApplicationAction(getDisplayName(), getApplicationIcon());
    }

    default GuiPushToApplicationSettings getSettings(PushToApplication application, DialogService dialogService, FilePreferences filePreferences, PushToApplicationPreferences pushToApplicationPreferences) {
        return new GuiPushToApplicationSettings(application, dialogService, filePreferences, pushToApplicationPreferences);
    }
}
