package org.jabref.gui.frame;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;

/**
 * Sends attachments for selected entries to the
 * configured Kindle email
 */
public class SendAsKindleEmailAction extends SendAsEMailAction {
    private final GuiPreferences preferences;

    public SendAsKindleEmailAction(DialogService dialogService, GuiPreferences preferences, StateManager stateManager, TaskExecutor taskExecutor) {
        super(dialogService, preferences, stateManager, taskExecutor);
        this.preferences = preferences;
        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager).and(ActionHelper.hasLinkedFileForSelectedEntries(stateManager)));
    }

    @Override
    protected String getEmailAddress() {
        return preferences.getExternalApplicationsPreferences().getKindleEmail();
    }

    @Override
    protected String getSubject() {
        return Localization.lang("Send to Kindle");
    }

    @Override
    protected String getBody() {
        return Localization.lang("Send to Kindle");
    }
}
