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
import org.jabref.logic.push.PushToTeXworks;

public class GUIPushToTeXworks extends PushToTeXworks implements GUIPushToApplication {

    private final DialogService dialogService;

    public GUIPushToTeXworks(DialogService dialogService, CliPreferences preferences) {
        super(dialogService, preferences);
        this.dialogService = dialogService;
    }

    @Override
    public PushToApplicationSettings getSettings(PushToApplication application, PushToApplicationPreferences preferences) {
        return new PushToApplicationSettings(application, dialogService, this.preferences.getFilePreferences(), preferences);
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
        return IconTheme.JabRefIcons.APPLICATION_TEXWORS;
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
