package org.jabref.gui.fieldeditors.optioneditors;

import java.util.Arrays;
import java.util.Collection;

import javax.swing.undo.UndoManager;

import javafx.util.StringConverter;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.Langid;
import org.jabref.model.entry.field.Field;
import org.jabref.model.strings.StringUtil;

public class LangidEditorViewModel extends OptionEditorViewModel<Langid> {
    private BibDatabaseMode databaseMode;

//    private final ObjectProperty<Langid> selectedLangidProperty = new SimpleObjectProperty<>();
    public LangidEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, BibDatabaseMode databaseMode, FieldCheckers fieldCheckers, UndoManager undoManager) {
        super(field, suggestionProvider, fieldCheckers, undoManager);
        this.databaseMode = databaseMode;
    }
//
//    public Langid getValue() {
//        return selectedLangidProperty.get();
//    }
//
//    public void setValue(Langid langid) {
//        selectedLangidProperty.set(langid);
//    }
//
//    // Optionally add a property getter
//    public ObjectProperty<Langid> selectedLangidProperty() {
//        return selectedLangidProperty;
//    }

    @Override
    public StringConverter<Langid> getStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Langid object) {
                if (object == null) {
                    return null;
                } else {
                    return object.getLangid();  // Langid used as both display and value
                }
            }

            @Override
            public Langid fromString(String string) {
                if (StringUtil.isNotBlank(string)) {
                    return Langid.parse(string).orElse(null);
                } else {
                    return null;
                }
            }
        };
    }

    @Override
    public Collection<Langid> getItems() {
        return Arrays.asList(Langid.values());
    }

    @Override
    public String convertToDisplayText(Langid object) {
        return object.getLangid();  // Langid and display text are the same
    }
}
