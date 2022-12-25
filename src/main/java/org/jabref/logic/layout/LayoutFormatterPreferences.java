package org.jabref.logic.layout;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javafx.beans.property.StringProperty;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.format.NameFormatterPreferences;

public class LayoutFormatterPreferences {

    private final NameFormatterPreferences nameFormatterPreferences;
    private final StringProperty mainFileDirectoryProperty;
    private final Map<String, String> customExportNameFormatters = new HashMap<>();
    private final JournalAbbreviationRepository journalAbbreviationRepository;

    public LayoutFormatterPreferences(NameFormatterPreferences nameFormatterPreferences,
                                      StringProperty mainFileDirectoryProperty,
                                      JournalAbbreviationRepository journalAbbreviationRepository) {
        this.nameFormatterPreferences = nameFormatterPreferences;
        this.mainFileDirectoryProperty = mainFileDirectoryProperty;
        this.journalAbbreviationRepository = journalAbbreviationRepository;
    }

    public NameFormatterPreferences getNameFormatterPreferences() {
        return nameFormatterPreferences;
    }

    public String getMainFileDirectory() {
        return mainFileDirectoryProperty.get();
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
