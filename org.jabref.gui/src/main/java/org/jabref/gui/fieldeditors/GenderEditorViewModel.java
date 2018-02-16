package org.jabref.gui.fieldeditors;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class GenderEditorViewModel extends MapBasedEditorViewModel<String> {

    private BiMap<String, String> itemMap = HashBiMap.create(7);

    public GenderEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers) {
        super(fieldName, suggestionProvider, fieldCheckers);

        itemMap.put("sf", Localization.lang("Female name"));
        itemMap.put("sm", Localization.lang("Male name"));
        itemMap.put("sn", Localization.lang("Neuter name"));
        itemMap.put("pf", Localization.lang("Female names"));
        itemMap.put("pm", Localization.lang("Male names"));
        itemMap.put("pn", Localization.lang("Neuter names"));
        itemMap.put("pp", Localization.lang("Mixed names"));
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
