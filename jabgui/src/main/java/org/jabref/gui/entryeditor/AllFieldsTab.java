package org.jabref.gui.entryeditor;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Tooltip;

import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.OrFields;

/// The single scroll-list tab showing *all* fields of an entry (issue #12711):
/// the citation key, all required fields (even when unset), and every set field.
/// Replaces the classic category tabs (required / optional / other / …) as the default view.
public class AllFieldsTab extends FieldsEditorTab {

    private final BibEntryTypesManager entryTypesManager;

    public AllFieldsTab(UndoManager undoManager,
                        UndoAction undoAction,
                        RedoAction redoAction,
                        GuiPreferences preferences,
                        BibEntryTypesManager entryTypesManager,
                        JournalAbbreviationRepository journalAbbreviationRepository,
                        StateManager stateManager,
                        PreviewPanel previewPanel) {
        super(
                false,
                undoManager,
                undoAction,
                redoAction,
                preferences,
                journalAbbreviationRepository,
                stateManager,
                previewPanel
        );

        this.entryTypesManager = entryTypesManager;
        setText(EntryEditorTabModel.BuiltIn.ALL_FIELDS.displayName());
        setTooltip(new Tooltip(Localization.lang("Show all fields")));
        setGraphic(IconTheme.JabRefIcons.REQUIRED.getGraphicNode());
    }

    /// Order: citation key, required fields (entry-type order), set optional fields
    /// (important first, then detail; each in entry-type order), then all remaining set
    /// fields sorted by name.
    @Override
    protected SequencedSet<Field> determineFieldsToShow(BibEntry entry) {
        BibDatabaseMode mode = stateManager.getActiveDatabase().map(BibDatabaseContext::getMode)
                                           .orElse(BibDatabaseMode.BIBLATEX);
        Optional<BibEntryType> entryType = entryTypesManager.enrich(entry.getType(), mode);

        Set<Field> setFields = entry.getFields();
        SequencedSet<Field> fields = new LinkedHashSet<>();
        fields.add(InternalField.KEY_FIELD);
        if (entryType.isPresent()) {
            for (OrFields orFields : entryType.get().getRequiredFields()) {
                fields.addAll(orFields.getFields());
            }
            entryType.get().getImportantOptionalFields().stream()
                     .filter(setFields::contains)
                     .forEach(fields::add);
            entryType.get().getDetailOptionalNotDeprecatedFields(mode).stream()
                     .filter(setFields::contains)
                     .forEach(fields::add);
        }
        setFields.stream()
                 .sorted(Comparator.comparing(Field::getName))
                 .forEach(fields::add);
        return fields;
    }
}
