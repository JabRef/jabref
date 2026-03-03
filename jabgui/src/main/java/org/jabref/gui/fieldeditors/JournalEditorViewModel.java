package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Button;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.conferences.ConferenceAbbreviationRepository;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class JournalEditorViewModel extends AbstractEditorViewModel {
    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final ConferenceAbbreviationRepository conferenceAbbreviationRepository;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;

    public JournalEditorViewModel(
            Field field,
            SuggestionProvider<?> suggestionProvider,
            JournalAbbreviationRepository journalAbbreviationRepository,
            ConferenceAbbreviationRepository conferenceAbbreviationRepository,
            FieldCheckers fieldCheckers,
            TaskExecutor taskExecutor,
            DialogService dialogService,
            UndoManager undoManager) {
        super(field, suggestionProvider, fieldCheckers, undoManager);
        this.journalAbbreviationRepository = journalAbbreviationRepository;
        this.conferenceAbbreviationRepository = conferenceAbbreviationRepository;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
    }

    public void toggleAbbreviation() {
        if (StringUtil.isBlank(text.get())) {
            return;
        }

        // Ignore brackets when matching abbreviations.
        final String name = StringUtil.ignoreCurlyBracket(text.get());

        if (field == StandardField.BOOKTITLE) {
            conferenceAbbreviationRepository.getNextAbbreviation(name)
                                            .ifPresent(nextAbbreviation -> {
                                                text.set(nextAbbreviation);
                                                // TODO: Add undo
                                                // panel.getUndoManager().addEdit(new UndoableFieldChange(entry, editor.getName(), text, nextAbbreviation));
                                            });
            return;
        }

        journalAbbreviationRepository.getNextAbbreviation(name).ifPresent(nextAbbreviation -> {
            text.set(nextAbbreviation);
            // TODO: Add undo
            // panel.getUndoManager().addEdit(new UndoableFieldChange(entry, editor.getName(), text, nextAbbreviation));
        });
    }

    public void showJournalInfo(Button journalInfoButton) {
        PopOverUtil.showJournalInfo(journalInfoButton, entry, dialogService, taskExecutor);
    }
}
