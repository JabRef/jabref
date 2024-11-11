package org.jabref.gui.entryeditor;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Tooltip;

import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.OrFields;

public class RequiredFieldsTab extends FieldsEditorTab {

    public static final String NAME = "Required fields";
    private final BibEntryTypesManager entryTypesManager;

    public RequiredFieldsTab(BibDatabaseContext databaseContext,
                             SuggestionProviders suggestionProviders,
                             UndoManager undoManager,
                             UndoAction undoAction,
                             RedoAction redoAction,
                             GuiPreferences preferences,
                             BibEntryTypesManager entryTypesManager,
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
                previewPanel
        );
        this.entryTypesManager = entryTypesManager;
        setText(Localization.lang("Required fields"));
        setTooltip(new Tooltip(Localization.lang("Show required fields")));
        setGraphic(IconTheme.JabRefIcons.REQUIRED.getGraphicNode());
    }

    @Override
    protected SequencedSet<Field> determineFieldsToShow(BibEntry entry) {
        Optional<BibEntryType> entryType = entryTypesManager.enrich(entry.getType(), databaseContext.getMode());
        SequencedSet<Field> fields = new LinkedHashSet<>();
        if (entryType.isPresent()) {
            for (OrFields orFields : entryType.get().getRequiredFields()) {
                fields.addAll(orFields.getFields());
            }
            // Add the edit field for BibTeX key (AKA citation key)
            fields.add(InternalField.KEY_FIELD);
        } else {
            // Entry type unknown -> treat all fields as required
            fields.addAll(entry.getFields());
        }
        return fields;
    }
}
