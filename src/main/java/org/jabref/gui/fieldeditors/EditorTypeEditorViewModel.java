package org.jabref.gui.fieldeditors;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class EditorTypeEditorViewModel extends MapBasedEditorViewModel<String> {

    private BiMap<String, String> itemMap = HashBiMap.create(7);

    public EditorTypeEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers) {
        super(fieldName, suggestionProvider, fieldCheckers);

        itemMap.put("editor", Localization.lang("Editor"));
        itemMap.put("compiler", Localization.lang("Compiler"));
        itemMap.put("founder", Localization.lang("Founder"));
        itemMap.put("continuator", Localization.lang("Continuator"));
        itemMap.put("redactor", Localization.lang("Redactor"));
        itemMap.put("reviser", Localization.lang("Reviser"));
        itemMap.put("collaborator", Localization.lang("Collaborator"));
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
