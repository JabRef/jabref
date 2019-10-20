package org.jabref.gui.entryeditor;

import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.JabRefPreferences;

public class UserDefinedFieldsTab extends FieldsEditorTab {
    private final SortedSet<Field> fields;

    public UserDefinedFieldsTab(String name, Set<Field> fields, BibDatabaseContext databaseContext, SuggestionProviders suggestionProviders, UndoManager undoManager, DialogService dialogService, JabRefPreferences preferences, BibEntryTypesManager entryTypesManager, ExternalFileTypes externalFileTypes, TaskExecutor taskExecutor, JournalAbbreviationLoader journalAbbreviationLoader) {
        super(false, databaseContext, suggestionProviders, undoManager, dialogService, preferences, externalFileTypes, taskExecutor, journalAbbreviationLoader);

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
