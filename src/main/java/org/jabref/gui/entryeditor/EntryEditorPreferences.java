package org.jabref.gui.entryeditor;

import java.util.List;
import java.util.Map;

import org.jabref.Globals;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.logic.bibtex.LatexFieldFormatterPreferences;
import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.preferences.JabRefPreferences;

public class EntryEditorPreferences {

    private final Map<String, List<String>> entryEditorTabList;
    private final LatexFieldFormatterPreferences latexFieldFormatterPreferences;
    private final ImportFormatPreferences importFormatPreferences;
    private final BibtexKeyPatternPreferences bibtexKeyPatternPreferences;
    private final List<String> customTabFieldNames;
    private final boolean shouldShowRecommendationsTab;
    private boolean showSourceTabByDefault;
    private final KeyBindingRepository keyBindings;

    public EntryEditorPreferences(Map<String, List<String>> entryEditorTabList, LatexFieldFormatterPreferences latexFieldFormatterPreferences, ImportFormatPreferences importFormatPreferences, List<String> customTabFieldNames, boolean shouldShowRecommendationsTab, boolean showSourceTabByDefault, BibtexKeyPatternPreferences bibtexKeyPatternPreferences, KeyBindingRepository keyBindings) {
        this.entryEditorTabList = entryEditorTabList;
        this.latexFieldFormatterPreferences = latexFieldFormatterPreferences;
        this.importFormatPreferences = importFormatPreferences;
        this.customTabFieldNames = customTabFieldNames;
        this.shouldShowRecommendationsTab = shouldShowRecommendationsTab;
        this.showSourceTabByDefault = showSourceTabByDefault;
        this.bibtexKeyPatternPreferences = bibtexKeyPatternPreferences;
        this.keyBindings = keyBindings;
    }

    public static EntryEditorPreferences from(JabRefPreferences preferences) {
        return new EntryEditorPreferences(preferences.getEntryEditorTabList(),
                                          preferences.getLatexFieldFormatterPreferences(),
                                          preferences.getImportFormatPreferences(),
                                          preferences.getCustomTabFieldNames(),
                                          preferences.getBoolean(JabRefPreferences.SHOW_RECOMMENDATIONS),
                                          preferences.getBoolean(JabRefPreferences.DEFAULT_SHOW_SOURCE),
                                          preferences.getBibtexKeyPatternPreferences(),
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

    public BibtexKeyPatternPreferences getBibtexKeyPatternPreferences() {
        return bibtexKeyPatternPreferences;
    }

    public boolean isShowSourceTabByDefault() {
        return showSourceTabByDefault;
    }

    public void setShowSourceTabByDefault(boolean showSourceTabByDefault) {
        this.showSourceTabByDefault = showSourceTabByDefault;
    }
}
