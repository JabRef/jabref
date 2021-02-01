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

        itemMap.put("mathesis", Localization.lang("Master's thesis"));
        itemMap.put("phdthesis", Localization.lang("PhD thesis"));
        itemMap.put("candthesis", Localization.lang("Candidate thesis"));
        itemMap.put("techreport", Localization.lang("Technical report"));
        itemMap.put("resreport", Localization.lang("Research report"));
        itemMap.put("software", Localization.lang("Software"));
        itemMap.put("datacd", Localization.lang("Data CD"));
        itemMap.put("audiocd", Localization.lang("Audio CD"));
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
