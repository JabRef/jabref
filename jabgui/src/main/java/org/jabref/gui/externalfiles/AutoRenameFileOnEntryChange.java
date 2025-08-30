package org.jabref.gui.externalfiles;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.cleanup.RenamePdfCleanup;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoRenameFileOnEntryChange {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoRenameFileOnEntryChange.class);

    private final FilePreferences filePreferences;
    private final RenamePdfCleanup renamePdfCleanup;

    public AutoRenameFileOnEntryChange(BibDatabaseContext bibDatabaseContext, FilePreferences filePreferences) {
        this.filePreferences = filePreferences;
        renamePdfCleanup = new RenamePdfCleanup(false, () -> bibDatabaseContext, filePreferences);
    }

    @Subscribe
    public void listen(FieldChangedEvent event) {
        if (!filePreferences.shouldAutoRenameFilesOnChange()
                || filePreferences.getFileNamePattern().isEmpty()
                || filePreferences.getFileNamePattern() == null) {
            return;
        }

        if (event.getField().equals(StandardField.FILE)) {
            return;
        }

        BibEntry entry = event.getBibEntry();
        LOGGER.debug("Field changed for entry {}: {}", entry.getCitationKey().orElse("defaultCitationKey"), event.getField().getName());
        if (entry.getFiles().isEmpty()) {
            return;
        }
        renamePdfCleanup.cleanup(entry);
    }
}
