package org.jabref.gui.entryeditor;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.stream.Collectors;

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
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;

import com.tobiasdiez.easybind.EasyBind;

public class DeprecatedFieldsTab extends FieldsEditorTab {

    public static final String NAME = "Deprecated fields";
    private final BibEntryTypesManager entryTypesManager;

    public DeprecatedFieldsTab(BibDatabaseContext databaseContext,
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

        setText(Localization.lang("Deprecated fields"));
        EasyBind.subscribe(preferences.getWorkspacePreferences().showAdvancedHintsProperty(), advancedHints -> {
            if (advancedHints) {
                setTooltip(new Tooltip(Localization.lang("Shows fields having a successor in biblatex.\nFor instance, the publication month should be part of the date field.\nUse the Clean up Entries functionality to convert the entry to biblatex.")));
            } else {
                setTooltip(new Tooltip(Localization.lang("Shows fields having a successor in biblatex.")));
            }
        });
        setGraphic(IconTheme.JabRefIcons.OPTIONAL.getGraphicNode());
    }

    @Override
    protected SequencedSet<Field> determineFieldsToShow(BibEntry entry) {
        BibDatabaseMode mode = databaseContext.getMode();
        Optional<BibEntryType> entryType = entryTypesManager.enrich(entry.getType(), mode);
        if (entryType.isPresent()) {
            return entryType.get().getDeprecatedFields(mode).stream().filter(field -> entry.getField(field).isPresent()).collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            // Entry type unknown -> treat all fields as required (thus no optional fields)
            return new LinkedHashSet<>();
        }
    }
}
