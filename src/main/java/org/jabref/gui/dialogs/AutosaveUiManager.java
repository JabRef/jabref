package org.jabref.gui.dialogs;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.model.database.event.AutosaveEvent;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.preferences.PreferencesService;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has an abstract UI role as it listens for an {@link AutosaveEvent} and saves the bib file associated with
 * the given {@link LibraryTab}.
 */
public class AutosaveUiManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutosaveUiManager.class);

    private final SaveDatabaseAction saveDatabaseAction;

    public AutosaveUiManager(LibraryTab libraryTab, DialogService dialogService, PreferencesService preferencesService, BibEntryTypesManager entryTypesManager) {
        this.saveDatabaseAction = new SaveDatabaseAction(libraryTab, dialogService, preferencesService, entryTypesManager);
    }

    @Subscribe
    public void listen(AutosaveEvent event) {
        try {
            this.saveDatabaseAction.save(SaveDatabaseAction.SaveDatabaseMode.SILENT);
        } catch (Throwable e) {
            LOGGER.error("Problem occurred while saving.", e);
        }
    }
}
