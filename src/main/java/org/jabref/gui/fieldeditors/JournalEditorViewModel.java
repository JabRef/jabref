package org.jabref.gui.fieldeditors;

import java.util.Optional;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.strings.StringUtil;

public class JournalEditorViewModel extends AbstractEditorViewModel {
    private final JournalAbbreviationLoader journalAbbreviationLoader;
    private final JournalAbbreviationPreferences journalAbbreviationPreferences;

    public JournalEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider, JournalAbbreviationLoader journalAbbreviationLoader, JournalAbbreviationPreferences journalAbbreviationPreferences, FieldCheckers fieldCheckers) {
        super(fieldName, suggestionProvider, fieldCheckers);

        this.journalAbbreviationLoader = journalAbbreviationLoader;
        this.journalAbbreviationPreferences = journalAbbreviationPreferences;
    }

    public void toggleAbbreviation() {
        if (StringUtil.isBlank(text.get())) {
            return;
        }

        JournalAbbreviationRepository abbreviationRepository = journalAbbreviationLoader.getRepository(journalAbbreviationPreferences);
        if (abbreviationRepository.isKnownName(text.get())) {
            Optional<String> nextAbbreviation = abbreviationRepository.getNextAbbreviation(text.get());

            if (nextAbbreviation.isPresent()) {
                text.set(nextAbbreviation.get());
                // TODO: Add undo
                //panel.getUndoManager().addEdit(new UndoableFieldChange(entry, editor.getFieldName(), text, nextAbbreviation));
            }
        }
    }
}
