package org.jabref.gui.entryeditor;

import javax.swing.undo.UndoManager;

import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.entry.BibEntryTypesManager;

public class DetailOptionalFieldsTab extends OptionalFieldsTabBase {

    public DetailOptionalFieldsTab(UndoManager undoManager,
                                   UndoAction undoAction,
                                   RedoAction redoAction,
                                   GuiPreferences preferences,
                                   BibEntryTypesManager entryTypesManager,
                                   JournalAbbreviationRepository journalAbbreviationRepository,
                                   StateManager stateManager,
                                   PreviewPanel previewPanel) {
        super(
                EntryEditorTabModel.BuiltIn.DETAIL_OPTIONAL_FIELDS.displayName(),
                false,
                undoManager,
                undoAction,
                redoAction,
                preferences,
                entryTypesManager,
                journalAbbreviationRepository,
                stateManager,
                previewPanel
        );
    }
}
