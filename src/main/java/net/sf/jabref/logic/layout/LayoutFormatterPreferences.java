package net.sf.jabref.logic.layout;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.journals.JournalAbbreviationPreferences;
import net.sf.jabref.logic.layout.format.FileLinkPreferences;
import net.sf.jabref.logic.layout.format.NameFormatterPreferences;
import net.sf.jabref.preferences.JabRefPreferences;

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

    public static LayoutFormatterPreferences fromPreferences(JabRefPreferences jabRefPreferences,
            JournalAbbreviationLoader journalAbbreviationLoader) {
        Objects.requireNonNull(jabRefPreferences);
        Objects.requireNonNull(journalAbbreviationLoader);
        return new LayoutFormatterPreferences(NameFormatterPreferences.fromPreferences(jabRefPreferences),
                JournalAbbreviationPreferences.fromPreferences(jabRefPreferences),
                FileLinkPreferences.fromPreferences(jabRefPreferences),
                journalAbbreviationLoader);
    }

    public NameFormatterPreferences getNameFormatterPreferences() {
        return nameFormatterPreferences;
    }

    public JournalAbbreviationPreferences getJournalAbbreviationPreferences() {
        return journalAbbreviationPreferences;
    }

    public Map<String, String> getCustomExportNameFormatters() {
        return customExportNameFormatters;
    }

    public FileLinkPreferences getFileLinkPreferences() {
        return fileLinkPreferences;
    }

    public JournalAbbreviationLoader getJournalAbbreviationLoader() {
        return journalAbbreviationLoader;
    }
}
