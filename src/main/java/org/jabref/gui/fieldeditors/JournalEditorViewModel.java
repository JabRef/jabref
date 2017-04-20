package org.jabref.gui.fieldeditors;

import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.strings.StringUtil;

public class JournalEditorViewModel extends AbstractViewModel {
    private final JournalAbbreviationLoader journalAbbreviationLoader;
    private final JournalAbbreviationPreferences journalAbbreviationPreferences;
    private StringProperty text = new SimpleStringProperty();

    public JournalEditorViewModel(JournalAbbreviationLoader journalAbbreviationLoader, JournalAbbreviationPreferences journalAbbreviationPreferences) {
        this.journalAbbreviationLoader = journalAbbreviationLoader;
        this.journalAbbreviationPreferences = journalAbbreviationPreferences;
    }

    public StringProperty textProperty() {
        return text;
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
