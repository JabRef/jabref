package org.jabref.gui.fieldeditors.optioneditors.mapbased;

import java.util.Map;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

public class TypeEditorViewModel extends StringMapBasedEditorViewModel {

    public TypeEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, UndoManager undoManager) {
        super(field, suggestionProvider, fieldCheckers, undoManager, Map.of(
                "mathesis", Localization.lang("Master's thesis"),
                "phdthesis", Localization.lang("PhD thesis"),
                "candthesis", Localization.lang("Candidate thesis"),
                "bathesis", Localization.lang("Bachelor's thesis"),
                "techreport", Localization.lang("Technical report"),
                "resreport", Localization.lang("Research report"),
                "software", Localization.lang("Software"),
                "datacd", Localization.lang("Data CD"),
                "audiocd", Localization.lang("Audio CD")));
    }
}
