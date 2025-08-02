package org.jabref.gui.externalfiles;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.cleanup.RenamePdfCleanup;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoRenameFileOnEntryChange {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoRenameFileOnEntryChange.class);

    private final GuiPreferences preferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final RenamePdfCleanup renamePdfCleanup;

    public AutoRenameFileOnEntryChange(BibDatabaseContext bibDatabaseContext, GuiPreferences preferences) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.preferences = preferences;
        renamePdfCleanup = new RenamePdfCleanup(false, () -> bibDatabaseContext, preferences.getFilePreferences());
    }

    public void bindToDatabase() {
        this.bibDatabaseContext.getDatabase().registerListener(this);
    }

    public void unbindFromDatabase() {
        this.bibDatabaseContext.getDatabase().unregisterListener(this);
    }

    @Subscribe
    public void listen(FieldChangedEvent event) {
        FilePreferences filePreferences = preferences.getFilePreferences();

        if (!filePreferences.shouldAutoRenameFilesOnChange()
                || filePreferences.getFileNamePattern().isEmpty()
                || filePreferences.getFileNamePattern() == null) {
            return;
        }

        BibEntry entry = event.getBibEntry();
        if (entry.getFiles().isEmpty()) {
            return;
        }
        renamePdfCleanup.cleanup(entry);

        LOGGER.info("Field changed for entry {}: {}", entry.getCitationKey().orElse("defaultCitationKey"), event.getField().getName());
    }
}
