package org.jabref.gui.externalfiles;

import java.util.HashSet;
import java.util.Set;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.cleanup.RenamePdfCleanup;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.logic.citationkeypattern.BracketedPattern.expandBrackets;

public class AutoRenameFileOnEntryChange {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoRenameFileOnEntryChange.class);

    private final GuiPreferences preferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final RenamePdfCleanup renamePdfCleanup;

    public AutoRenameFileOnEntryChange(BibDatabaseContext bibDatabaseContext, GuiPreferences preferences) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.preferences = preferences;
        bibDatabaseContext.getDatabase().registerListener(this);
        renamePdfCleanup = new RenamePdfCleanup(false, () -> bibDatabaseContext, preferences.getFilePreferences());
    }

    @Subscribe
    public void listen(FieldChangedEvent event) {
        FilePreferences filePreferences = preferences.getFilePreferences();

        if (!filePreferences.shouldAutoRenameFilesOnChange()
                || filePreferences.getFileNamePattern().isEmpty()
                || filePreferences.getFileNamePattern() == null
                || !relatesToFilePattern(filePreferences.getFileNamePattern(), event)) {
            return;
        }

        BibEntry entry = event.getBibEntry();
        if (entry.getFiles().isEmpty()) {
            return;
        }
        new CitationKeyGenerator(bibDatabaseContext, preferences.getCitationKeyPatternPreferences()).generateAndSetKey(entry);
        renamePdfCleanup.cleanup(entry);

        LOGGER.info("Field changed for entry {}: {}", entry.getCitationKey().orElse("defaultCitationKey"), event.getField().getName());
    }

    private boolean relatesToFilePattern(String fileNamePattern, FieldChangedEvent event) {
        Set<String> extractedFields = new HashSet<>();

        expandBrackets(fileNamePattern, bracketContent -> {
            extractedFields.add(bracketContent);
            return bracketContent;
        });

        return extractedFields.contains("bibtexkey")
                || extractedFields.contains(event.getField().getName());
    }
}
