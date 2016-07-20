package net.sf.jabref.logic.layout;

import java.util.Map;

import net.sf.jabref.logic.journals.JournalAbbreviationPreferences;
import net.sf.jabref.logic.layout.format.FileLinkPreferences;
import net.sf.jabref.logic.layout.format.NameFormatterPreferences;
import net.sf.jabref.preferences.JabRefPreferences;

public class LayoutFormatterPreferences {

    private final NameFormatterPreferences nameFormatterPreferences;
    private final JournalAbbreviationPreferences journalAbbreviationPreferences;
    private final FileLinkPreferences fileLinkPreferences;
    private final Map<String, String> customExportNameFormatters;

    public LayoutFormatterPreferences(NameFormatterPreferences nameFormatterPreferences,
            JournalAbbreviationPreferences journalAbbreviationPreferences, FileLinkPreferences fileLinkPreferences,
            Map<String, String> customExportNameFormatters) {
        this.nameFormatterPreferences = nameFormatterPreferences;
        this.journalAbbreviationPreferences = journalAbbreviationPreferences;
        this.customExportNameFormatters = customExportNameFormatters;
        this.fileLinkPreferences = fileLinkPreferences;
    }

    public static LayoutFormatterPreferences fromPreferences(JabRefPreferences jabRefPreferences) {
        return new LayoutFormatterPreferences(NameFormatterPreferences.fromPreferences(jabRefPreferences),
                JournalAbbreviationPreferences.fromPreferences(jabRefPreferences),
                FileLinkPreferences.fromPreferences(jabRefPreferences),
                jabRefPreferences.customExportNameFormatters);
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
}
