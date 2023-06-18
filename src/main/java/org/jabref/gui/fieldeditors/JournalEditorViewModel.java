package org.jabref.gui.fieldeditors;

import javafx.scene.Node;
import javafx.scene.control.Button;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.journalinfo.JournalInfoView;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.strings.StringUtil;

import org.controlsfx.control.PopOver;

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

    public void showJournalInfo(Button journalInfoButton) {
        PopOver popOver = new PopOver();
        Node node = new JournalInfoView().getNode();
        popOver.setContentNode(node);
        popOver.setDetachable(false);
        popOver.setTitle(Localization.lang("Journal Information"));
        popOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);
        popOver.setArrowSize(0);
        popOver.show(journalInfoButton, 0);
    }
}
