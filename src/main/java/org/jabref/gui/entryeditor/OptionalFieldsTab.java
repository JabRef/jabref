package org.jabref.gui.entryeditor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;

public class OptionalFieldsTab extends FieldsEditorTab {
    public OptionalFieldsTab(BibDatabaseContext databaseContext, SuggestionProviders suggestionProviders, UndoManager undoManager, DialogService dialogService) {
        super(true, databaseContext, suggestionProviders, undoManager, dialogService);

        setText(Localization.lang("Optional fields"));
        setTooltip(new Tooltip(Localization.lang("Show optional fields")));
        setGraphic(IconTheme.JabRefIcons.OPTIONAL.getGraphicNode());
    }

    @Override
    protected Collection<Field> determineFieldsToShow(BibEntry entry) {
        Optional<BibEntryType> entryType = BibEntryTypesManager.enrich(entry.getType(), databaseContext.getMode());
        if (entryType.isPresent()) {
            return new HashSet<>(entryType.get().getPrimaryOptionalFields());
        } else {
            // Entry type unknown -> treat all fields as required
            return Collections.emptySet();
        }
    }
}
