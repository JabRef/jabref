package org.jabref.gui.fieldeditors;

import java.util.List;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class CustomFieldEditorViewModel extends MapBasedEditorViewModel<String> {

    private BiMap<String, String> itemMap;

    public CustomFieldEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider,
                                      FieldCheckers fieldCheckers, UndoManager undoManager, BibDatabaseContext databaseContext) {
        super(field, suggestionProvider, fieldCheckers, undoManager);

        List<String> values = databaseContext.getMetaData().getContentSelectorValuesForField(field);
        itemMap = HashBiMap.create(values.size());
        for (String value : values) {
            itemMap.put(value, value);
        }
    }

    @Override
    protected BiMap<String, String> getItemMap() {
        return itemMap;
    }

    @Override
    public String convertToDisplayText(String object) {
        return object;
    }
}
