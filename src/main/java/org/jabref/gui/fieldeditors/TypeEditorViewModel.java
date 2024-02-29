package org.jabref.gui.fieldeditors;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class TypeEditorViewModel extends MapBasedEditorViewModel<String> {

    private BiMap<String, String> itemMap = HashBiMap.create(8);

    public TypeEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers) {
        super(field, suggestionProvider, fieldCheckers);

        // Initialize default types
        addCustomType("mathesis", Localization.lang("Master's thesis"));
        addCustomType("phdthesis", Localization.lang("PhD thesis"));
        addCustomType("candthesis", Localization.lang("Candidate thesis"));
        addCustomType("techreport", Localization.lang("Technical report"));
        addCustomType("resreport", Localization.lang("Research report"));
        addCustomType("software", Localization.lang("Software"));
        addCustomType("datacd", Localization.lang("Data CD"));
        addCustomType("audiocd", Localization.lang("Audio CD"));
    }

    @Override
    protected BiMap<String, String> getItemMap() {
        return itemMap;
    }

    @Override
    public String convertToDisplayText(String object) {
        return object;
    }

    // Method to add custom types with key value pair
    public void addCustomType(String key, String displayName) {
        itemMap.put(key, displayName);
    }
}
