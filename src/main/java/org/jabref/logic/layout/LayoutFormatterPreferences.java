package org.jabref.logic.layout;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.format.FileLinkPreferences;
import org.jabref.logic.layout.format.NameFormatterPreferences;

public class LayoutFormatterPreferences {

    private final NameFormatterPreferences nameFormatterPreferences;
    private final FileLinkPreferences fileLinkPreferences;
    private final Map<String, String> customExportNameFormatters = new HashMap<>();
    private final JournalAbbreviationRepository journalAbbreviationRepository;

    public LayoutFormatterPreferences(NameFormatterPreferences nameFormatterPreferences,
                                      FileLinkPreferences fileLinkPreferences,
                                      JournalAbbreviationRepository journalAbbreviationRepository) {
        this.nameFormatterPreferences = nameFormatterPreferences;
        this.fileLinkPreferences = fileLinkPreferences;
        this.journalAbbreviationRepository = journalAbbreviationRepository;
    }

    public NameFormatterPreferences getNameFormatterPreferences() {
        return nameFormatterPreferences;
    }

    public FileLinkPreferences getFileLinkPreferences() {
        return fileLinkPreferences;
    }

    public JournalAbbreviationRepository getJournalAbbreviationRepository() {
        return journalAbbreviationRepository;
    }

    public void clearCustomExportNameFormatters() {
        customExportNameFormatters.clear();
    }

    public void putCustomExportNameFormatter(String formatterName, String contents) {
        customExportNameFormatters.put(formatterName, contents);
    }

    public Optional<String> getCustomExportNameFormatter(String formatterName) {
        return Optional.ofNullable(customExportNameFormatters.get(formatterName));
    }
}
