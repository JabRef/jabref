package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.bibtexkeypattern.GenerateBibtexKeySingleAction;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import de.saxsys.mvvmfx.utils.commands.Command;

public class BibtexKeyEditorViewModel extends AbstractEditorViewModel {
    private final PreferencesService preferencesService;
    private final BibDatabaseContext databaseContext;
    private final UndoManager undoManager;
    private final DialogService dialogService;

    public BibtexKeyEditorViewModel(Field field, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, PreferencesService preferencesService, BibDatabaseContext databaseContext, UndoManager undoManager, DialogService dialogService) {
        super(field, suggestionProvider, fieldCheckers);
        this.preferencesService = preferencesService;
        this.databaseContext = databaseContext;
        this.undoManager = undoManager;
        this.dialogService = dialogService;
    }

    public Command getGenerateCiteKeyCommand() {
        return new GenerateBibtexKeySingleAction(entry, databaseContext, dialogService, preferencesService, undoManager);
    }
}
