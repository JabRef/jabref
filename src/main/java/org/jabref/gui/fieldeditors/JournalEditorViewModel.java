package org.jabref.gui.fieldeditors;

import java.util.Optional;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.journals.AbbreviationRepository;
import org.jabref.model.entry.field.Field;
import org.jabref.model.strings.StringUtil;

public class JournalEditorViewModel extends AbstractEditorViewModel {
    private final AbbreviationRepository journalAbbreviationRepository;

    public JournalEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, AbbreviationRepository journalAbbreviationRepository, FieldCheckers fieldCheckers) {
        super(field, suggestionProvider, fieldCheckers);

        this.journalAbbreviationRepository = journalAbbreviationRepository;
    }

    public void toggleAbbreviation() {
        if (StringUtil.isBlank(text.get())) {
            return;
        }

        // Ignore brackets when matching abbreviations.
        final String name = StringUtil.ignoreCurlyBracket(text.get());

        if (journalAbbreviationRepository.isKnownName(name)) {
            Optional<String> nextAbbreviation = journalAbbreviationRepository.getNextAbbreviation(name);

            if (nextAbbreviation.isPresent()) {
                text.set(nextAbbreviation.get());
                // TODO: Add undo
                // panel.getUndoManager().addEdit(new UndoableFieldChange(entry, editor.getName(), text, nextAbbreviation));
            }
        }
    }
}
