package org.jabref.gui.frame;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends the selected entries to any specifiable email
 * by populating the email body
 */
public class SendAsStandardEmailAction extends SendAsEMailAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendAsStandardEmailAction.class);
    private final GuiPreferences preferences;
    private final StateManager stateManager;
    private final BibEntryTypesManager entryTypesManager;

    public SendAsStandardEmailAction(DialogService dialogService,
                                     GuiPreferences preferences,
                                     StateManager stateManager,
                                     BibEntryTypesManager entryTypesManager,
                                     TaskExecutor taskExecutor) {
        super(dialogService, preferences, stateManager, taskExecutor);
        this.preferences = preferences;
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
        return preferences.getExternalApplicationsPreferences().getEmailSubject();
    }

    @Override
    protected String getBody() {
        List<BibEntry> entries = stateManager.getSelectedEntries();
        BibDatabaseContext databaseContext = stateManager.getActiveDatabase().get();
        StringWriter rawEntries = new StringWriter();
        BibWriter bibWriter = new BibWriter(rawEntries, OS.NEWLINE);

        BibEntryWriter bibtexEntryWriter = new BibEntryWriter(new FieldWriter(preferences.getFieldPreferences()), entryTypesManager);

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
