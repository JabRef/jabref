package org.jabref.gui.fieldeditors;

import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.undo.UndoableKeyChange;
import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternUtil;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;

public class BibtexKeyEditorViewModel extends AbstractEditorViewModel {
    private BibtexKeyPatternPreferences keyPatternPreferences;
    private BibDatabaseContext bibDatabaseContext;
    private UndoManager undoManager;

    public BibtexKeyEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, BibtexKeyPatternPreferences keyPatternPreferences, BibDatabaseContext bibDatabaseContext, UndoManager undoManager) {
        super(fieldName, suggestionProvider, fieldCheckers);
        this.keyPatternPreferences = keyPatternPreferences;
        this.bibDatabaseContext = bibDatabaseContext;
        this.undoManager = undoManager;
    }

    public void generateKey() {
        Optional<FieldChange> fieldChange = BibtexKeyPatternUtil.makeAndSetLabel(
                bibDatabaseContext.getMetaData().getCiteKeyPattern(keyPatternPreferences.getKeyPattern()),
                bibDatabaseContext.getDatabase(),
                entry,
                keyPatternPreferences);
        fieldChange.ifPresent(change -> undoManager.addEdit(new UndoableKeyChange(change)));
    }
}
