package org.jabref.gui.entryeditor;

import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.icon.IconTheme;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class UserDefinedFieldsTab extends FieldsEditorTab {
    private final SortedSet<Field> fields;

    public UserDefinedFieldsTab(String name, Set<Field> fields, BibDatabaseContext databaseContext, SuggestionProviders suggestionProviders, UndoManager undoManager, DialogService dialogService) {
        super(false, databaseContext, suggestionProviders, undoManager, dialogService);
        this.fields = new TreeSet<>(Comparator.comparing(Field::getName));
        this.fields.addAll(fields);

        setText(name);
        setGraphic(IconTheme.JabRefIcons.OPTIONAL.getGraphicNode());
    }

    @Override
    protected SortedSet<Field> determineFieldsToShow(BibEntry entry) {
        return fields;
    }
}
