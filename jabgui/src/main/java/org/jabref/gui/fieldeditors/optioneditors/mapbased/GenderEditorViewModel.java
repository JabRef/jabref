package org.jabref.gui.fieldeditors.optioneditors.mapbased;

import java.util.Map;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

public class GenderEditorViewModel extends StringMapBasedEditorViewModel {

    public GenderEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, UndoManager undoManager) {
        super(field, suggestionProvider, fieldCheckers, undoManager, Map.of(
                "sf", Localization.lang("Female name"),
                "sm", Localization.lang("Male name"),
                "sn", Localization.lang("Neuter name"),
                "pf", Localization.lang("Female names"),
                "pm", Localization.lang("Male names"),
                "pn", Localization.lang("Neuter names"),
                "pp", Localization.lang("Mixed names")));
    }
}
