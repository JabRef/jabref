package org.jabref.gui.entryeditor;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

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

public class DeprecatedFieldsTab extends FieldsEditorTab {
    public DeprecatedFieldsTab(BibDatabaseContext databaseContext, SuggestionProviders suggestionProviders, UndoManager undoManager, DialogService dialogService) {
        super(false, databaseContext, suggestionProviders, undoManager, dialogService);

        setText(Localization.lang("Deprecated fields"));
        setTooltip(new Tooltip(Localization.lang("Show deprecated BibTeX fields")));
        setGraphic(IconTheme.JabRefIcons.OPTIONAL.getGraphicNode());
    }

    @Override
    protected Collection<Field> determineFieldsToShow(BibEntry entry) {
        Optional<BibEntryType> entryType = BibEntryTypesManager.enrich(entry.getType(), databaseContext.getMode());
        if (entryType.isPresent()) {
            return entryType.get().getDeprecatedFields()
                            .stream()
                            .filter(entry::hasField)
                            .collect(Collectors.toList());
        } else {
            // Entry type unknown -> treat all fields as required
            return Collections.emptySet();
        }
    }
}
