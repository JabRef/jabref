package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;

public class OtherFieldsTab extends FieldsEditorTab {

    public static final String NAME = "Other fields";
    private final List<Field> customTabsFieldNames;
    private final BibEntryTypesManager entryTypesManager;

    public OtherFieldsTab(UndoManager undoManager,
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
        this.customTabsFieldNames = new ArrayList<>();
        preferences.getEntryEditorPreferences().getEntryEditorTabs().values().forEach(customTabsFieldNames::addAll);

        setText(Localization.lang("Other fields"));
        setTooltip(new Tooltip(Localization.lang("Show remaining fields")));
        setGraphic(IconTheme.JabRefIcons.OPTIONAL.getGraphicNode());
    }

    @Override
    protected SequencedSet<Field> determineFieldsToShow(BibEntry entry) {
        BibDatabaseMode mode = stateManager.getActiveDatabase().map(BibDatabaseContext::getMode)
                                           .orElse(BibDatabaseMode.BIBLATEX);
        Optional<BibEntryType> entryType = entryTypesManager.enrich(entry.getType(), mode);
        if (entryType.isPresent()) {
            // Get all required and optional fields configured for the entry
            Set<Field> allKnownFields = entryType.get().getAllFields();
            // Remove all fields being required or optional
            SequencedSet<Field> otherFields = entry.getFields().stream()
                                                   .filter(field -> !allKnownFields.contains(field))
                                                   .collect(Collectors.toCollection(LinkedHashSet::new));
            // The key field is in the required tab, but has a special treatment
            otherFields.remove(InternalField.KEY_FIELD);
            // Remove all fields contained in JabRef's tab "Deprecated"
            otherFields.removeAll(entryType.get().getDeprecatedFields(mode));
            // Remove all fields contained in the custom tabs
            customTabsFieldNames.forEach(otherFields::remove);
            // Remove all user-comment fields (tab org.jabref.gui.entryeditor.CommentsTab)
            otherFields.removeIf(field -> field.equals(StandardField.COMMENT));
            otherFields.removeIf(field -> field instanceof UserSpecificCommentField);
            return otherFields;
        } else {
            // Entry type unknown -> treat all fields as required (thus no other fields)
            return new LinkedHashSet<>();
        }
    }
}
