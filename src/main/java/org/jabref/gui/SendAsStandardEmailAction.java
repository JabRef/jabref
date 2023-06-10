package org.jabref.gui;

import java.io.StringWriter;

import org.jabref.gui.actions.ActionHelper;
import org.jabref.preferences.PreferencesService;

/**
 * Sends the selected entries to any specifiable email
 * by populating the email body
 */
public class SendAsStandardEmailAction extends SendAsEMailAction {
    private final PreferencesService preferencesService;

    public SendAsStandardEmailAction(DialogService dialogService, PreferencesService preferencesService, StateManager stateManager) {
        super(dialogService, preferencesService, stateManager);
        this.preferencesService = preferencesService;
        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    protected String getEmailAddress() {
        return "";
    }

    @Override
    protected String getSubject() {
        return preferencesService.getExternalApplicationsPreferences().getEmailSubject();
    }

    @Override
    protected String getBody(StringWriter rawEntries) {
        return rawEntries.toString();
    }
}
