package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.citationkeypattern.GenerateCitationKeySingleAction;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;

import de.saxsys.mvvmfx.utils.commands.Command;

public class CitationKeyEditorViewModel extends AbstractEditorViewModel {
    private final CliPreferences preferences;
    private final BibDatabaseContext databaseContext;
    private final UndoManager undoManager;
    private final DialogService dialogService;

    public CitationKeyEditorViewModel(Field field,
                                      SuggestionProvider<?> suggestionProvider,
                                      FieldCheckers fieldCheckers,
                                      CliPreferences preferences,
                                      BibDatabaseContext databaseContext,
                                      UndoManager undoManager,
                                      DialogService dialogService) {
        super(field, suggestionProvider, fieldCheckers, undoManager);
        this.preferences = preferences;
        this.databaseContext = databaseContext;
        this.undoManager = undoManager;
        this.dialogService = dialogService;
    }

    public Command getGenerateCiteKeyCommand() {
        return new GenerateCitationKeySingleAction(entry, databaseContext, dialogService, preferences, undoManager);
    }
}
