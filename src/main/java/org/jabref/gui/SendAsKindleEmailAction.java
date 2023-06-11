package org.jabref.gui;

import org.jabref.gui.actions.ActionHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

/**
 * Sends attachments for selected entries to the
 * configured Kindle email
 */
public class SendAsKindleEmailAction extends SendAsEMailAction {
    private final PreferencesService preferencesService;

    public SendAsKindleEmailAction(DialogService dialogService, PreferencesService preferencesService, StateManager stateManager) {
        super(dialogService, preferencesService, stateManager);
        this.preferencesService = preferencesService;
        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager).and(ActionHelper.hasLinkedFileForSelectedEntries(stateManager)));
    }

    @Override
    protected String getEmailAddress() {
        return preferencesService.getExternalApplicationsPreferences().getKindleEmail();
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
