package org.jabref.gui.entryeditor;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;

import javax.swing.undo.UndoManager;

import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.jabref.gui.StateManager;
import org.jabref.gui.fieldeditors.FieldEditorFX;
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

    /// Preferred number of visible text rows for multiline editors in the scroll list
    /// (instead of the JavaFX TextArea default of 10).
    private static final int MULTILINE_ROWS = 4;

    /// Pixels of preferred height granted per weight unit for editors with weight > 1
    /// (e.g. the linked-files list), since percent-height rows do not exist in the scroll list.
    private static final double HEIGHT_PER_WEIGHT = 60;

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

    @Override
    protected boolean stretchContentToTabHeight() {
        return false;
    }

    /// Single column of label/editor rows with natural heights (the tab scrolls instead of
    /// stretching the editors to the tab height), grouped into sections
    /// (main / identifiers / files & links / comments) with a header before each named section.
    @Override
    protected void layoutEditors(List<Label> labels, boolean compressed) {
        if (!gridPane.getStyleClass().contains("all-fields-list")) {
            gridPane.getStyleClass().add("all-fields-list");
        }

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(Region.USE_PREF_SIZE);
        ColumnConstraints editorColumn = new ColumnConstraints();
        editorColumn.setHgrow(Priority.ALWAYS);
        gridPane.getColumnConstraints().addAll(labelColumn, editorColumn);

        // labels were created in editors-map iteration order (see FieldsEditorTab#setupPanel)
        Map<Field, Label> labelForField = new LinkedHashMap<>();
        int labelIndex = 0;
        for (Field field : editors.keySet()) {
            labelForField.put(field, labels.get(labelIndex));
            labelIndex++;
        }

        int row = 0;
        // editors is a LinkedHashMap; copy keeps the display order as a SequencedCollection
        for (FieldListSections.Section section : FieldListSections.partition(List.copyOf(editors.keySet()))) {
            Optional<String> header = section.type().header();
            if (header.isPresent()) {
                gridPane.add(createSectionHeader(header.get()), 0, row, 2, 1);
                row++;
            }
            for (Field field : section.fields()) {
                Label label = labelForField.get(field);
                GridPane.setValignment(label, VPos.TOP);
                gridPane.add(label, 0, row);
                gridPane.add(editors.get(field).getNode(), 1, row);
                row++;
            }
        }

        editors.values().forEach(AllFieldsTab::applyNaturalHeight);
    }

    private static Node createSectionHeader(String text) {
        Label header = new Label(text);
        header.getStyleClass().add("all-fields-section-header");
        VBox box = new VBox(new Separator(), header);
        box.getStyleClass().add("all-fields-section");
        return box;
    }

    private static void applyNaturalHeight(FieldEditorFX editor) {
        limitTextAreaRows(editor.getNode());
        if ((editor.getWeight() > 1) && (editor.getNode() instanceof Region region)) {
            region.setPrefHeight(editor.getWeight() * HEIGHT_PER_WEIGHT);
        }
    }

    private static void limitTextAreaRows(Node node) {
        if (node instanceof TextArea textArea) {
            textArea.setPrefRowCount(MULTILINE_ROWS);
        } else if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(AllFieldsTab::limitTextAreaRows);
        }
    }
}
