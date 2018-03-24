package org.jabref.gui.entryeditor;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;

public class OtherFieldsTab extends FieldsEditorTab {

    private final List<String> customTabFieldNames;

    public OtherFieldsTab(BibDatabaseContext databaseContext, SuggestionProviders suggestionProviders, UndoManager undoManager, List<String> customTabFieldNames, DialogService dialogService) {
        super(false, databaseContext, suggestionProviders, undoManager, dialogService);

        setText(Localization.lang("Other fields"));
        setTooltip(new Tooltip(Localization.lang("Show remaining fields")));
        setGraphic(IconTheme.JabRefIcons.OPTIONAL.getGraphicNode());
        this.customTabFieldNames = customTabFieldNames;
    }

    @Override
    protected Collection<String> determineFieldsToShow(BibEntry entry, EntryType entryType) {
        List<String> allKnownFields = entryType.getAllFields().stream().map(String::toLowerCase)
                .collect(Collectors.toList());
        List<String> otherFields = entry.getFieldNames().stream().map(String::toLowerCase)
                .filter(field -> !allKnownFields.contains(field)).collect(Collectors.toList());

        otherFields.removeAll(entryType.getDeprecatedFields());
        otherFields.remove(BibEntry.KEY_FIELD);
        otherFields.removeAll(customTabFieldNames);
        return otherFields;
    }
}
