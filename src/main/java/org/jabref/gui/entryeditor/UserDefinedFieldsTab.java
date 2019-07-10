package org.jabref.gui.entryeditor;

import java.util.Collection;
import java.util.List;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.icon.IconTheme;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.field.Field;

public class UserDefinedFieldsTab extends FieldsEditorTab {
    private final List<Field> fields;

    public UserDefinedFieldsTab(String name, List<Field> fields, BibDatabaseContext databaseContext, SuggestionProviders suggestionProviders, UndoManager undoManager, DialogService dialogService) {
        super(false, databaseContext, suggestionProviders, undoManager, dialogService);
        this.fields = fields;

        setText(name);
        setGraphic(IconTheme.JabRefIcons.OPTIONAL.getGraphicNode());
    }

    @Override
    protected Collection<Field> determineFieldsToShow(BibEntry entry, EntryType entryType) {
        return fields;
    }
}
