package org.jabref.gui.entryeditor;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.JabRefPreferences;

public class UserDefinedFieldsTab extends FieldsEditorTab {
    private final LinkedHashSet<Field> fields;

    public UserDefinedFieldsTab(String name, Set<Field> fields, BibDatabaseContext databaseContext, SuggestionProviders suggestionProviders, UndoManager undoManager, DialogService dialogService, JabRefPreferences preferences, BibEntryTypesManager entryTypesManager, ExternalFileTypes externalFileTypes, TaskExecutor taskExecutor, JournalAbbreviationRepository journalAbbreviationRepository) {
        super(false, databaseContext, suggestionProviders, undoManager, dialogService, preferences, externalFileTypes, taskExecutor, journalAbbreviationRepository);

        this.fields = new LinkedHashSet<>(fields);

        setText(name);
        setGraphic(IconTheme.JabRefIcons.OPTIONAL.getGraphicNode());
    }

    @Override
    protected Set<Field> determineFieldsToShow(BibEntry entry) {
        return fields;
    }
}
