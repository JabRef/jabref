package org.jabref.gui.fieldeditors;

import java.util.Optional;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.strings.StringUtil;

public class JournalEditorViewModel extends AbstractEditorViewModel {
    private final JournalAbbreviationRepository journalAbbreviationRepository;

    public JournalEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider, JournalAbbreviationRepository journalAbbreviationRepository, FieldCheckers fieldCheckers) {
        super(fieldName, suggestionProvider, fieldCheckers);

        this.journalAbbreviationRepository = journalAbbreviationRepository;
    }

    public void toggleAbbreviation() {
        if (StringUtil.isBlank(text.get())) {
            return;
        }

        if (journalAbbreviationRepository.isKnownName(text.get())) {
            Optional<String> nextAbbreviation = journalAbbreviationRepository.getNextAbbreviation(text.get());

            if (nextAbbreviation.isPresent()) {
                text.set(nextAbbreviation.get());
                // TODO: Add undo
                //panel.getUndoManager().addEdit(new UndoableFieldChange(entry, editor.getFieldName(), text, nextAbbreviation));
            }
        }
    }
}
