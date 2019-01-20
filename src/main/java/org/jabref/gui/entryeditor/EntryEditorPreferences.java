package org.jabref.gui.entryeditor;

import java.util.List;
import java.util.Map;

import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.logic.bibtex.LatexFieldFormatterPreferences;
import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;

public class EntryEditorPreferences {

    private final Map<String, List<String>> entryEditorTabList;
    private final LatexFieldFormatterPreferences latexFieldFormatterPreferences;
    private final ImportFormatPreferences importFormatPreferences;
    private final BibtexKeyPatternPreferences bibtexKeyPatternPreferences;
    private final List<String> customTabFieldNames;
    private final boolean shouldShowRecommendationsTab;
    private final boolean isMrdlibAccepted;
    private boolean showSourceTabByDefault;
    private final KeyBindingRepository keyBindings;
    private boolean avoidOverwritingCiteKey;

    public EntryEditorPreferences(Map<String, List<String>> entryEditorTabList, LatexFieldFormatterPreferences latexFieldFormatterPreferences, ImportFormatPreferences importFormatPreferences, List<String> customTabFieldNames, boolean shouldShowRecommendationsTab, boolean isMrdlibAccepted, boolean showSourceTabByDefault, BibtexKeyPatternPreferences bibtexKeyPatternPreferences, KeyBindingRepository keyBindings, boolean avoidOverwritingCiteKey) {
        this.entryEditorTabList = entryEditorTabList;
        this.latexFieldFormatterPreferences = latexFieldFormatterPreferences;
        this.importFormatPreferences = importFormatPreferences;
        this.customTabFieldNames = customTabFieldNames;
        this.shouldShowRecommendationsTab = shouldShowRecommendationsTab;
        this.isMrdlibAccepted = isMrdlibAccepted;
        this.showSourceTabByDefault = showSourceTabByDefault;
        this.bibtexKeyPatternPreferences = bibtexKeyPatternPreferences;
        this.keyBindings = keyBindings;
        this.avoidOverwritingCiteKey = avoidOverwritingCiteKey;
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

    public boolean isMrdlibAccepted() {
        return isMrdlibAccepted;
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

    public boolean avoidOverwritingCiteKey() {
        return avoidOverwritingCiteKey;
    }
}
