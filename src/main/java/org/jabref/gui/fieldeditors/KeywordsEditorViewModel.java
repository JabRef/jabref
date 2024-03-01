package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.util.StringConverter;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

public class KeywordsEditorViewModel extends AbstractEditorViewModel {

    private final ListProperty<Keyword> keywordListProperty;

    public KeywordsEditorViewModel(Field field,
                                   SuggestionProvider<?> suggestionProvider,
                                   FieldCheckers fieldCheckers,
                                   PreferencesService preferencesService,
                                   UndoManager undoManager) {

        super(field, suggestionProvider, fieldCheckers, undoManager);

        Character keywordSeparator = preferencesService.getBibEntryPreferences().getKeywordSeparator();
        keywordListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    }

    public ListProperty<Keyword> keywordListProperty() {
        return keywordListProperty;
    }

    public StringConverter<Keyword> getStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Keyword keyword) {
                if (keyword != null) {
                    return keyword.get();
                }
                return "";
            }

            @Override
            public Keyword fromString(String keywordString) {
                return new Keyword(keywordString);
            }
        };
    }
}
