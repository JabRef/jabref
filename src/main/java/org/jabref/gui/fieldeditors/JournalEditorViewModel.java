package org.jabref.gui.fieldeditors;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.entry.field.Field;
import org.jabref.model.strings.StringUtil;

public class JournalEditorViewModel extends AbstractEditorViewModel {
    private final JournalAbbreviationRepository journalAbbreviationRepository;

    public JournalEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, JournalAbbreviationRepository journalAbbreviationRepository, FieldCheckers fieldCheckers) {
        super(field, suggestionProvider, fieldCheckers);
        this.journalAbbreviationRepository = journalAbbreviationRepository;
    }

    public void toggleAbbreviation() {
        if (StringUtil.isBlank(text.get())) {
            return;
        }

        // Ignore brackets when matching abbreviations.
        final String name = StringUtil.ignoreCurlyBracket(text.get());

        journalAbbreviationRepository.getNextAbbreviation(name).ifPresent(nextAbbreviation -> {
            text.set(nextAbbreviation);
            // TODO: Add undo
            // panel.getUndoManager().addEdit(new UndoableFieldChange(entry, editor.getName(), text, nextAbbreviation));
        });
    }
}
