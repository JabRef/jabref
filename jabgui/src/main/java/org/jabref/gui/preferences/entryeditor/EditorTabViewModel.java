package org.jabref.gui.preferences.entryeditor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.entryeditor.EntryEditorTabModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Mutable working copy of one entry editor tab ([EntryEditorTabModel]) while it is edited in the
/// preferences dialog; converted back via [#toModel()] on store.
@NullMarked
public class EditorTabViewModel {

    /// `null` for custom tabs.
    private final EntryEditorTabModel.@Nullable BuiltIn builtIn;
    private final String customName;
    private final BooleanProperty visible = new SimpleBooleanProperty(true);
    private final ObservableList<String> fieldPatterns = FXCollections.observableArrayList();
    private final Set<String> extractedPatterns = new HashSet<>();

    private EditorTabViewModel(EntryEditorTabModel.@Nullable BuiltIn builtIn, String customName) {
        this.builtIn = builtIn;
        this.customName = customName;
    }

    public static EditorTabViewModel fromModel(EntryEditorTabModel model) {
        return switch (model) {
            case EntryEditorTabModel.BuiltInTab(
                    EntryEditorTabModel.BuiltIn type,
                    boolean visible
            ) -> {
                EditorTabViewModel tab = new EditorTabViewModel(type, "");
                tab.visible.set(visible);
                yield tab;
            }
            case EntryEditorTabModel.CustomizedFieldsTab customTab -> {
                EditorTabViewModel tab = new EditorTabViewModel(null, customTab.name());
                tab.fieldPatterns.setAll(customTab.fieldPatterns());
                tab.extractedPatterns.addAll(customTab.extractedFieldPatterns());
                yield tab;
            }
        };
    }

    public static EditorTabViewModel newCustomTab(String name) {
        return new EditorTabViewModel(null, name);
    }

    public EntryEditorTabModel toModel() {
        if (builtIn != null) {
            return new EntryEditorTabModel.BuiltInTab(builtIn, visible.get());
        }
        // Extracted flags of removed patterns are dropped, so re-adding a pattern later
        // starts with the default (not extracted) again.
        return new EntryEditorTabModel.CustomizedFieldsTab(
                customName,
                List.copyOf(fieldPatterns),
                fieldPatterns.stream().filter(extractedPatterns::contains).collect(Collectors.toSet()));
    }

    public boolean isCustom() {
        return builtIn == null;
    }

    public String getDisplayName() {
        return builtIn != null ? builtIn.displayName() : customName;
    }

    public BooleanProperty visibleProperty() {
        return visible;
    }

    /// The tab's ordered field patterns; only ever non-empty for custom tabs.
    public ObservableList<String> getFieldPatterns() {
        return fieldPatterns;
    }

    public boolean isExtracted(String fieldPattern) {
        return extractedPatterns.contains(fieldPattern);
    }

    /// Marks the pattern's fields for extraction: they leave the Main tab.
    public void extractFromMainTab(String fieldPattern) {
        extractedPatterns.add(fieldPattern);
    }

    /// Reverts [#extractFromMainTab]: the pattern's fields are shown on the Main tab again.
    public void keepOnMainTab(String fieldPattern) {
        extractedPatterns.remove(fieldPattern);
    }
}
