package org.jabref.gui;

import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

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
    protected URI getUriMailTo(StringWriter rawEntries, List<String> attachments) throws URISyntaxException {
        StringBuilder mailTo = new StringBuilder();
        mailTo.append(preferencesService.getExternalApplicationsPreferences().getKindleEmail());
        mailTo.append("?Body=").append(Localization.lang("Send to Kindle"));
        mailTo.append("&Subject=").append(Localization.lang("Send to Kindle"));

        for (String path : attachments) {
            mailTo.append("&Attachment=\"").append(path);
            mailTo.append("\"");
        }

        return new URI("mailto", mailTo.toString(), null);
    }
}
