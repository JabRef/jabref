package org.jabref.gui.preferences.entryeditor;

import java.util.List;

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
            case EntryEditorTabModel.CustomizedFieldsTab(
                    String name,
                    List<String> fieldPatterns
            ) -> {
                EditorTabViewModel tab = new EditorTabViewModel(null, name);
                tab.fieldPatterns.setAll(fieldPatterns);
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
        return new EntryEditorTabModel.CustomizedFieldsTab(customName, List.copyOf(fieldPatterns));
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
}
