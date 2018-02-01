package org.jabref.gui.entryeditor;

import java.util.List;
import java.util.Map;

import org.jabref.Globals;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.logic.bibtex.LatexFieldFormatterPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.preferences.JabRefPreferences;

public class EntryEditorPreferences {
    private Map<String, List<String>> entryEditorTabList;
    private LatexFieldFormatterPreferences latexFieldFormatterPreferences;
    private ImportFormatPreferences importFormatPreferences;
    private List<String> customTabFieldNames;
    private boolean shouldShowRecommendationsTab;
    private boolean showSourceTabByDefault;
    private KeyBindingRepository keyBindings;

    public EntryEditorPreferences(Map<String, List<String>> entryEditorTabList, LatexFieldFormatterPreferences latexFieldFormatterPreferences, ImportFormatPreferences importFormatPreferences, List<String> customTabFieldNames, boolean shouldShowRecommendationsTab, boolean showSourceTabByDefault, KeyBindingRepository keyBindings) {
        this.entryEditorTabList = entryEditorTabList;
        this.latexFieldFormatterPreferences = latexFieldFormatterPreferences;
        this.importFormatPreferences = importFormatPreferences;
        this.customTabFieldNames = customTabFieldNames;
        this.shouldShowRecommendationsTab = shouldShowRecommendationsTab;
        this.showSourceTabByDefault = showSourceTabByDefault;
        this.keyBindings = keyBindings;
    }

    public static EntryEditorPreferences from(JabRefPreferences preferences) {
        return new EntryEditorPreferences(
                preferences.getEntryEditorTabList(),
                preferences.getLatexFieldFormatterPreferences(),
                preferences.getImportFormatPreferences(),
                preferences.getCustomTabFieldNames(),
                preferences.getBoolean(JabRefPreferences.SHOW_RECOMMENDATIONS),
                preferences.getBoolean(JabRefPreferences.DEFAULT_SHOW_SOURCE),
                Globals.getKeyPrefs());
    }

    public Map<String, List<String>> getEntryEditorTabList() {
        return entryEditorTabList;
    }

    public LatexFieldFormatterPreferences getLatexFieldFormatterPreferences() {
        return latexFieldFormatterPreferences;
    }

    public ImportFormatPreferences getImportFormatPreferences() {
        return importFormatPreferences;
    }

    public List<String> getCustomTabFieldNames() {
        return customTabFieldNames;
    }

    public boolean shouldShowRecommendationsTab() {
        return shouldShowRecommendationsTab;
    }

    public boolean showSourceTabByDefault() {
        return showSourceTabByDefault;
    }

    public KeyBindingRepository getKeyBindings() {
        return keyBindings;
    }
}
