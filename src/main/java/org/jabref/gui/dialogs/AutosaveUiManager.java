package org.jabref.gui.dialogs;

import org.jabref.gui.Globals;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.model.database.event.AutosaveEvent;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class has an abstract UI role as it listens for an {@link AutosaveEvent} and saves the bib file associated with
 * the given {@link LibraryTab}.
 */
public class AutosaveUiManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutosaveUiManager.class);

    private SaveDatabaseAction saveDatabaseAction;

    public AutosaveUiManager(LibraryTab libraryTab) {
        this.saveDatabaseAction = new SaveDatabaseAction(libraryTab, Globals.prefs, Globals.entryTypesManager);
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
