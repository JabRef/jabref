package org.jabref.gui.fieldeditors;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class PaginationEditorViewModel extends MapBasedEditorViewModel<String> {

    private BiMap<String, String> itemMap = HashBiMap.create(7);

    public PaginationEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers) {
        super(field, suggestionProvider, fieldCheckers);

        itemMap.put("page", Localization.lang("Page"));
        itemMap.put("column", Localization.lang("Column"));
        itemMap.put("line", Localization.lang("Line"));
        itemMap.put("verse", Localization.lang("Verse"));
        itemMap.put("section", Localization.lang("Section"));
        itemMap.put("paragraph", Localization.lang("Paragraph"));
        itemMap.put("none", Localization.lang("None"));
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
