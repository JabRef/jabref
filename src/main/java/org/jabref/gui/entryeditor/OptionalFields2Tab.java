package org.jabref.gui.entryeditor;

import java.util.Collection;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Tooltip;

import org.jabref.gui.IconTheme;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;

public class OptionalFields2Tab extends FieldsEditorTab {
    public OptionalFields2Tab(BibDatabaseContext databaseContext, SuggestionProviders suggestionProviders, UndoManager undoManager) {
        super(true, databaseContext, suggestionProviders, undoManager);

        setText(Localization.lang("Optional fields 2"));
        setTooltip(new Tooltip(Localization.lang("Show optional fields")));
        setGraphic(IconTheme.JabRefIcon.OPTIONAL.getGraphicNode());
    }

    @Override
    protected Collection<String> determineFieldsToShow(BibEntry entry, EntryType entryType) {
        return entryType.getSecondaryOptionalNotDeprecatedFields();
    }
}
