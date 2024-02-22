package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import javafx.scene.control.ComboBox;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.PreferencesService;

public class KeywordsEditor extends SimpleEditor implements FieldEditorFX {

    private final BibDatabaseContext databaseContext;
    private ComboBox<String> keywordsComboBox;

    public KeywordsEditor(Field field,
                          SuggestionProvider<?> suggestionProvider,
                          FieldCheckers fieldCheckers,
                          PreferencesService preferences,
                          UndoManager undoManager,
                          BibDatabaseContext databaseContext) {
        super(field, suggestionProvider, fieldCheckers, preferences, undoManager);
        this.databaseContext = databaseContext;
        this.keywordsComboBox = createKeywordsComboBox();
        this.getChildren().add(keywordsComboBox);
    }

    @Override
    public double getWeight() {
        return 2;
    }

    private ComboBox<String> createKeywordsComboBox() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(databaseContext.getMetaData().getContentSelectorValuesForField(StandardField.KEYWORDS));
        return comboBox;
    }
}
