package org.jabref.gui.entryeditor;

import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.Set;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class UserDefinedFieldsTab extends FieldsEditorTab {
    private final LinkedHashSet<Field> fields;

    public UserDefinedFieldsTab(String name,
                                Set<Field> fields,
                                BibDatabaseContext databaseContext,
                                SuggestionProviders suggestionProviders,
                                UndoManager undoManager,
                                UndoAction undoAction,
                                RedoAction redoAction,
                                GuiPreferences preferences,
                                JournalAbbreviationRepository journalAbbreviationRepository,
                                PreviewPanel previewPanel) {
        super(
                false,
                databaseContext,
                suggestionProviders,
                undoManager,
                undoAction,
                redoAction,
                preferences,
                journalAbbreviationRepository,
                previewPanel);

        this.fields = new LinkedHashSet<>(fields);

        setText(name);
        setGraphic(IconTheme.JabRefIcons.OPTIONAL.getGraphicNode());
    }

    @Override
    protected SequencedSet<Field> determineFieldsToShow(BibEntry entry) {
        return fields;
    }
}
