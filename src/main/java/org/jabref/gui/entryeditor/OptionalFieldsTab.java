package org.jabref.gui.entryeditor;

import java.util.Collection;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;

public class OptionalFieldsTab extends FieldsEditorTab {
    public OptionalFieldsTab(BibDatabaseContext databaseContext, SuggestionProviders suggestionProviders, UndoManager undoManager, DialogService dialogService) {
        super(true, databaseContext, suggestionProviders, undoManager, dialogService);

        setText(Localization.lang("Optional fields"));
        setTooltip(new Tooltip(Localization.lang("Show optional fields")));
        setGraphic(IconTheme.JabRefIcons.OPTIONAL.getGraphicNode());
    }

    @Override
    protected Collection<String> determineFieldsToShow(BibEntry entry, EntryType entryType) {
        return entryType.getPrimaryOptionalFields();
    }
}
