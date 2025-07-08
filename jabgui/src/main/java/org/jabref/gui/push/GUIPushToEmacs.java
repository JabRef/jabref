package org.jabref.gui.push;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.Action;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.push.PushToApplication;
import org.jabref.logic.push.PushToApplicationPreferences;
import org.jabref.logic.push.PushToEmacs;

public class GUIPushToEmacs extends PushToEmacs implements GUIPushToApplication {

    private final DialogService dialogService;

    /**
     * @param dialogService
     * @param preferences   getPushToApplicationPreferences(), getExternalApplicationsPreferences(), and getFilePreferences() are used
     */
    public GUIPushToEmacs(DialogService dialogService, CliPreferences preferences) {
        super(dialogService, preferences);
        this.dialogService = dialogService;
    }

    @Override
    public String getTooltip() {
        return Localization.lang("Push entries to external application (%0)", getDisplayName());
    }

    @Override
    public Action getAction() {
        return new PushToApplicationAction();
    }

    @Override
    public JabRefIcon getApplicationIcon() {
        return IconTheme.JabRefIcons.APPLICATION_EMACS;
    }

    @Override
    public PushToApplicationSettings getSettings(PushToApplication application, PushToApplicationPreferences preferences) {
        return new PushToEmacsSettings(application, dialogService, this.preferences.getFilePreferences(), preferences);
    }

    protected class PushToApplicationAction implements Action {
        @Override
        public String getText() {
            return Localization.lang("Push entries to external application (%0)", getDisplayName());
        }

        @Override
        public Optional<JabRefIcon> getIcon() {
            return Optional.of(getApplicationIcon());
        }

        @Override
        public Optional<KeyBinding> getKeyBinding() {
            return Optional.of(KeyBinding.PUSH_TO_APPLICATION);
        }
    }

    @Override
    public void sendErrorNotification(String title, String message) {
        dialogService.showErrorDialogAndWait(title, message);
    }
}
