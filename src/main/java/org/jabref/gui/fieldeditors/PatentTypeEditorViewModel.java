package org.jabref.gui.fieldeditors;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class PatentTypeEditorViewModel extends MapBasedEditorViewModel<String> {

    private BiMap<String, String> itemMap = HashBiMap.create(12);

    public PatentTypeEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers) {
        super(fieldName, suggestionProvider, fieldCheckers);

        itemMap.put("patent", Localization.lang("Patent"));
        itemMap.put("patentde", Localization.lang("German patent"));
        itemMap.put("patenteu", Localization.lang("European patent"));
        itemMap.put("patentfr", Localization.lang("French patent"));
        itemMap.put("patentuk", Localization.lang("British patent"));
        itemMap.put("patentus", Localization.lang("U.S. patent"));
        itemMap.put("patreq", Localization.lang("Patent request"));
        itemMap.put("patreqde", Localization.lang("German patent request"));
        itemMap.put("patreqeu", Localization.lang("European patent request"));
        itemMap.put("patreqfr", Localization.lang("French patent request"));
        itemMap.put("patrequk", Localization.lang("British patent request"));
        itemMap.put("patrequs", Localization.lang("U.S. patent request"));
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
