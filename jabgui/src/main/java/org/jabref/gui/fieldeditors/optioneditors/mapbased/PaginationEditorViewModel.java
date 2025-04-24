package org.jabref.gui.fieldeditors.optioneditors.mapbased;

import java.util.Map;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

public class PaginationEditorViewModel extends StringMapBasedEditorViewModel {

    public PaginationEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, UndoManager undoManager) {
        super(field, suggestionProvider, fieldCheckers, undoManager, Map.of(
                "page", Localization.lang("Page"),
                "column", Localization.lang("Column"),
                "line", Localization.lang("Line"),
                "verse", Localization.lang("Verse"),
                "section", Localization.lang("Section"),
                "paragraph", Localization.lang("Paragraph"),
                "none", Localization.lang("None")));
    }
}
