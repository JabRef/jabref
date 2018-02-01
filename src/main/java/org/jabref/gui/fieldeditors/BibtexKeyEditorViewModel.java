package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.undo.UndoableKeyChange;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.logic.integrity.FieldCheckers;
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
        new BibtexKeyGenerator(bibDatabaseContext, keyPatternPreferences)
                .generateAndSetKey(entry)
                .ifPresent(change -> undoManager.addEdit(new UndoableKeyChange(change)));
    }
}
