package org.jabref.gui.fieldeditors;

import javafx.scene.control.Button;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.field.Field;
import org.jabref.model.undo.UndoManager;

public class JournalEditorViewModel extends AbstractEditorViewModel {
    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final GuiPreferences preferences;

    public JournalEditorViewModel(
            Field field,
            SuggestionProvider<?> suggestionProvider,
            JournalAbbreviationRepository journalAbbreviationRepository,
            FieldCheckers fieldCheckers,
            TaskExecutor taskExecutor,
            DialogService dialogService,
            GuiPreferences preferences,
            UndoManager undoManager) {
        super(field, suggestionProvider, fieldCheckers, undoManager);
        this.journalAbbreviationRepository = journalAbbreviationRepository;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.preferences = preferences;
    }

    public void toggleAbbreviation() {
        if (StringUtil.isBlank(text.get())) {
            return;
        }

        // Ignore brackets when matching abbreviations.
        final String name = StringUtil.ignoreCurlyBracket(text.get());

        journalAbbreviationRepository.getNextAbbreviation(name).ifPresent(nextAbbreviation -> {
            // Recorded on the undo stack by the binding installed in AbstractEditorViewModel#bindToEntry.
            text.set(nextAbbreviation);
        });
    }

    public void showJournalInfo(Button journalInfoButton) {
        PopOverUtil.showJournalInfo(journalInfoButton, entry, dialogService, taskExecutor, preferences.getImporterPreferences());
    }
}
