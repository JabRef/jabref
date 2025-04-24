package org.jabref.gui.fieldeditors.optioneditors.mapbased;

import java.util.HashMap;
import java.util.Map;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

public class PatentTypeEditorViewModel extends StringMapBasedEditorViewModel {

    public PatentTypeEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, UndoManager undoManager) {
        super(field, suggestionProvider, fieldCheckers, undoManager, getMap());
    }

    private static Map<String, String> getMap() {
        Map<String, String> itemMap = new HashMap<>();
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
        return itemMap;
    }
}
