package org.jabref.gui.entryeditor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.field.Field;

public class EntryEditorPreferences {

    private final Map<String, Set<Field>> entryEditorTabList;
    private final FieldWriterPreferences fieldWriterPreferences;
    private final ImportFormatPreferences importFormatPreferences;
    private final BibtexKeyPatternPreferences bibtexKeyPatternPreferences;
    private final List<Field> customTabFieldNames;
    private final boolean shouldShowRecommendationsTab;
    private final boolean isMrdlibAccepted;
    private boolean showSourceTabByDefault;
    private final KeyBindingRepository keyBindings;
    private boolean avoidOverwritingCiteKey;
    private final boolean shouldShowLatexCitationsTab;

    public EntryEditorPreferences(Map<String, Set<Field>> entryEditorTabList, FieldWriterPreferences fieldWriterPreferences, ImportFormatPreferences importFormatPreferences, List<Field> customTabFieldNames, boolean shouldShowRecommendationsTab, boolean isMrdlibAccepted, boolean shouldShowLatexCitationsTab, boolean showSourceTabByDefault, BibtexKeyPatternPreferences bibtexKeyPatternPreferences, KeyBindingRepository keyBindings, boolean avoidOverwritingCiteKey) {
        this.entryEditorTabList = entryEditorTabList;
        this.fieldWriterPreferences = fieldWriterPreferences;
        this.importFormatPreferences = importFormatPreferences;
        this.customTabFieldNames = customTabFieldNames;
        this.shouldShowRecommendationsTab = shouldShowRecommendationsTab;
        this.isMrdlibAccepted = isMrdlibAccepted;
        this.showSourceTabByDefault = showSourceTabByDefault;
        this.bibtexKeyPatternPreferences = bibtexKeyPatternPreferences;
        this.keyBindings = keyBindings;
        this.avoidOverwritingCiteKey = avoidOverwritingCiteKey;
        this.shouldShowLatexCitationsTab = shouldShowLatexCitationsTab;
    }

    public Map<String, Set<Field>> getEntryEditorTabList() {
        return entryEditorTabList;
    }

    public FieldWriterPreferences getFieldWriterPreferences() {
        return fieldWriterPreferences;
    }

    public ImportFormatPreferences getImportFormatPreferences() {
        return importFormatPreferences;
    }

    public List<Field> getCustomTabFieldNames() {
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

    public boolean shouldShowLatexCitationsTab() {
        return shouldShowLatexCitationsTab;
    }

    public boolean avoidOverwritingCiteKey() {
        return avoidOverwritingCiteKey;
    }
}
