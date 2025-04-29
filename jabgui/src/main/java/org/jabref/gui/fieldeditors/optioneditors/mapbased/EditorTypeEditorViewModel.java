package org.jabref.gui.fieldeditors.optioneditors.mapbased;

import java.util.Map;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

public class EditorTypeEditorViewModel extends StringMapBasedEditorViewModel {

    public EditorTypeEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, UndoManager undoManager) {
        super(field, suggestionProvider, fieldCheckers, undoManager, Map.of(
                "editor", Localization.lang("Editor"),
                "compiler", Localization.lang("Compiler"),
                "founder", Localization.lang("Founder"),
                "continuator", Localization.lang("Continuator"),
                "redactor", Localization.lang("Redactor"),
                "reviser", Localization.lang("Reviser"),
                "collaborator", Localization.lang("Collaborator")));
    }
}
