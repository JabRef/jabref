package org.jabref.gui;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends the selected entries to any specifiable email
 * by populating the email body
 */
public class SendAsStandardEmailAction extends SendAsEMailAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendAsStandardEmailAction.class);
    private final PreferencesService preferencesService;
    private final StateManager stateManager;
    private final BibEntryTypesManager entryTypesManager;

    public SendAsStandardEmailAction(DialogService dialogService,
                                     PreferencesService preferencesService,
                                     StateManager stateManager,
                                     BibEntryTypesManager entryTypesManager,
                                     TaskExecutor taskExecutor) {
        super(dialogService, preferencesService, stateManager, taskExecutor);
        this.preferencesService = preferencesService;
        this.stateManager = stateManager;
        this.entryTypesManager = entryTypesManager;
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
    protected String getBody() {
        List<BibEntry> entries = stateManager.getSelectedEntries();
        BibDatabaseContext databaseContext = stateManager.getActiveDatabase().get();
        StringWriter rawEntries = new StringWriter();
        BibWriter bibWriter = new BibWriter(rawEntries, OS.NEWLINE);

        BibEntryWriter bibtexEntryWriter = new BibEntryWriter(new FieldWriter(preferencesService.getFieldPreferences()), entryTypesManager);

        for (BibEntry entry : entries) {
            try {
                bibtexEntryWriter.write(entry, bibWriter, databaseContext.getMode());
            } catch (IOException e) {
                LOGGER.warn("Problem creating BibTeX file for mailing.", e);
            }
        }

        return rawEntries.toString();
    }
}
