package org.jabref.logic.layout;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.layout.format.FileLinkPreferences;
import org.jabref.logic.layout.format.NameFormatterPreferences;

public class LayoutFormatterPreferences {

    private final NameFormatterPreferences nameFormatterPreferences;
    private final JournalAbbreviationPreferences journalAbbreviationPreferences;
    private final FileLinkPreferences fileLinkPreferences;
    private final Map<String, String> customExportNameFormatters = new HashMap<>();
    private final JournalAbbreviationLoader journalAbbreviationLoader;

    public LayoutFormatterPreferences(NameFormatterPreferences nameFormatterPreferences,
            JournalAbbreviationPreferences journalAbbreviationPreferences, FileLinkPreferences fileLinkPreferences,
            JournalAbbreviationLoader journalAbbreviationLoader) {
        this.nameFormatterPreferences = nameFormatterPreferences;
        this.journalAbbreviationPreferences = journalAbbreviationPreferences;
        this.fileLinkPreferences = fileLinkPreferences;
        this.journalAbbreviationLoader = journalAbbreviationLoader;
    }

    public NameFormatterPreferences getNameFormatterPreferences() {
        return nameFormatterPreferences;
    }

    public JournalAbbreviationPreferences getJournalAbbreviationPreferences() {
        return journalAbbreviationPreferences;
    }

    public FileLinkPreferences getFileLinkPreferences() {
        return fileLinkPreferences;
    }

    public JournalAbbreviationLoader getJournalAbbreviationLoader() {
        return journalAbbreviationLoader;
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
