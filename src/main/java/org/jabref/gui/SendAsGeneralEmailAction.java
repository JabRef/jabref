package org.jabref.gui;

import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.jabref.gui.actions.ActionHelper;
import org.jabref.preferences.PreferencesService;

/**
 * Sends the selected entries to any specifiable email
 * by populating the email body
 */
public class SendAsGeneralEmailAction extends SendAsEMailAction {
    private final PreferencesService preferencesService;

    public SendAsGeneralEmailAction(DialogService dialogService, PreferencesService preferencesService, StateManager stateManager) {
        super(dialogService, preferencesService, stateManager);
        this.preferencesService = preferencesService;
        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    protected URI getUriMailTo(StringWriter rawEntries, List<String> attachments) throws URISyntaxException {
        StringBuilder mailTo = new StringBuilder();
        mailTo.append("?Body=").append(rawEntries.toString());
        mailTo.append("&Subject=");
        mailTo.append(preferencesService.getExternalApplicationsPreferences().getEmailSubject());

        for (String path : attachments) {
            mailTo.append("&Attachment=\"").append(path);
            mailTo.append("\"");
        }

        return new URI("mailto", mailTo.toString(), null);
    }
}
