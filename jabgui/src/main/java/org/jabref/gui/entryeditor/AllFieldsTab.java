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

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
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
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldTextMapper;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.OrFields;

import com.google.common.eventbus.Subscribe;
import org.jspecify.annotations.Nullable;

/// The single scroll-list tab showing *all* fields of an entry (issue #12711):
/// the citation key, all required fields (even when unset), and every set field.
/// Replaces the classic category tabs (required / optional / other / …) as the default view.
///
/// Fields are grouped Google-Contacts style ({@link FieldListSections}); below the list,
/// unset optional fields can be added via one-click chips ("Show more" reveals the
/// secondary-optional ones) or via a free-form field-name box.
public class AllFieldsTab extends FieldsEditorTab {

    /// Preferred number of visible text rows for multiline editors in the scroll list
    /// (instead of the JavaFX TextArea default of 10).
    private static final int MULTILINE_ROWS = 4;

    /// Pixels of preferred height granted per weight unit for editors with weight > 1
    /// (e.g. the linked-files list), since percent-height rows do not exist in the scroll list.
    private static final double HEIGHT_PER_WEIGHT = 60;

    private final BibEntryTypesManager entryTypesManager;

    /// Fields the user added via chip / free-form box that are still empty: they are not part
    /// of {@link BibEntry#getFields()} yet, but must stay visible while this entry is edited.
    private final Set<Field> userAddedFields = new LinkedHashSet<>();
    private @Nullable BibEntry entryOfUserAddedFields;

    /// Sticky per tab instance: whether the secondary-optional chips are expanded.
    private boolean showSecondaryOptionalChips;

    /// The entry whose event bus this tab is currently subscribed to (for live refresh
    /// when fields are set/unset from outside, e.g. Source tab, fetchers, undo).
    private @Nullable BibEntry subscribedEntry;

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
    /// fields sorted by name, then still-empty user-added fields.
    @Override
    protected SequencedSet<Field> determineFieldsToShow(BibEntry entry) {
        if (entry != entryOfUserAddedFields) {
            userAddedFields.clear();
            entryOfUserAddedFields = entry;
        }

        BibDatabaseMode mode = getDatabaseMode();
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
        fields.addAll(userAddedFields);
        return fields;
    }

    @Override
    protected boolean stretchContentToTabHeight() {
        return false;
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        if (subscribedEntry != entry) {
            if (subscribedEntry != null) {
                subscribedEntry.unregisterListener(this);
            }
            entry.registerListener(this);
            subscribedEntry = entry;
        }
        super.bindToEntry(entry);
    }

    /// Refreshes the list when a field is set or unset from outside this tab
    /// (Source tab, fetchers, undo, …). Rebuilds only when the set of shown fields
    /// actually changes, so typing inside a visible editor never rebuilds or steals focus.
    @Subscribe
    public void listen(FieldChangedEvent event) {
        if (event.getBibEntry() != subscribedEntry) {
            return;
        }
        Platform.runLater(() -> refreshShownFieldsIfNeeded(event));
    }

    private void refreshShownFieldsIfNeeded(FieldChangedEvent event) {
        BibEntry entry = subscribedEntry;
        if ((entry == null) || (getCurrentEntry() != entry)) {
            return;
        }
        // A visible field that was just cleared stays visible while this entry is edited
        // (otherwise deleting the last character would remove the editor mid-edit).
        if (editors.containsKey(event.getField())
                && ((event.getNewValue() == null) || event.getNewValue().isEmpty())) {
            userAddedFields.add(event.getField());
        }
        SequencedSet<Field> target = determineFieldsToShow(entry);
        if (!target.equals(editors.keySet())) {
            rebuildPanel(activeDatabaseContext(), entry);
        }
    }

    /// Single column of label/editor rows with natural heights (the tab scrolls instead of
    /// stretching the editors to the tab height), grouped into sections
    /// (main / identifiers / files & links / comments) with a header before each named section,
    /// followed by the add-field controls.
    @Override
    protected void layoutEditors(BibDatabaseContext bibDatabaseContext, BibEntry entry, boolean compressed, List<Label> labels) {
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
                // FieldNameLabel sets prefHeight to infinity to fill the stretch layout's
                // percent-height rows; in the natural-height list that would blow up every
                // row's preferred height, so reset it to the computed size.
                label.setPrefHeight(Region.USE_COMPUTED_SIZE);
                GridPane.setValignment(label, VPos.TOP);
                gridPane.add(label, 0, row);
                gridPane.add(editors.get(field).getNode(), 1, row);
                row++;
            }
        }

        gridPane.add(createAddFieldArea(bibDatabaseContext, entry), 0, row, 2, 1);

        editors.values().forEach(AllFieldsTab::applyNaturalHeight);
    }

    private static Node createSectionHeader(String text) {
        Label header = new Label(text);
        header.getStyleClass().add("all-fields-section-header");
        VBox box = new VBox(new Separator(), header);
        box.getStyleClass().add("all-fields-section");
        return box;
    }

    // region add-field controls

    private Node createAddFieldArea(BibDatabaseContext bibDatabaseContext, BibEntry entry) {
        BibDatabaseMode mode = getDatabaseMode();
        Optional<BibEntryType> entryType = entryTypesManager.enrich(entry.getType(), mode);

        FlowPane chips = new FlowPane();
        chips.getStyleClass().add("all-fields-add-chips");

        if (entryType.isPresent()) {
            List<Field> shown = List.copyOf(editors.keySet());
            FieldListSections.subtract(entryType.get().getImportantOptionalFields(), shown)
                             .forEach(field -> chips.getChildren().add(createAddChip(bibDatabaseContext, entry, field)));

            SequencedSet<Field> secondary = FieldListSections.subtract(
                    entryType.get().getDetailOptionalNotDeprecatedFields(mode), shown);
            if (!secondary.isEmpty()) {
                if (showSecondaryOptionalChips) {
                    secondary.forEach(field -> chips.getChildren().add(createAddChip(bibDatabaseContext, entry, field)));
                }
                Hyperlink toggle = new Hyperlink(showSecondaryOptionalChips
                                                 ? Localization.lang("Show less")
                                                 : Localization.lang("Show more"));
                toggle.setOnAction(_ -> {
                    showSecondaryOptionalChips = !showSecondaryOptionalChips;
                    rebuildPanel(bibDatabaseContext, entry);
                });
                chips.getChildren().add(toggle);
            }
        }

        ComboBox<String> fieldNameBox = new ComboBox<>();
        fieldNameBox.setEditable(true);
        fieldNameBox.getItems().addAll(FieldFactory.getAllFieldsWithOutInternal().stream()
                                                   .map(Field::getName)
                                                   .sorted()
                                                   .toList());
        fieldNameBox.setPromptText(Localization.lang("Field name"));
        Button addButton = new Button(Localization.lang("Add"));
        Runnable addAction = () -> addFreeFormField(bibDatabaseContext, entry, fieldNameBox.getEditor().getText());
        addButton.setOnAction(_ -> addAction.run());
        fieldNameBox.getEditor().setOnAction(_ -> addAction.run());
        HBox freeFormRow = new HBox(fieldNameBox, addButton);
        freeFormRow.getStyleClass().add("all-fields-add-free-form");
        freeFormRow.setAlignment(Pos.CENTER_LEFT);

        VBox area = new VBox(new Separator(), chips, freeFormRow);
        area.getStyleClass().add("all-fields-add-area");
        return area;
    }

    private Button createAddChip(BibDatabaseContext bibDatabaseContext, BibEntry entry, Field field) {
        Button chip = new Button("+ " + FieldTextMapper.getDisplayName(field));
        chip.getStyleClass().add("all-fields-add-chip");
        chip.setOnAction(_ -> showFieldEditor(bibDatabaseContext, entry, field));
        return chip;
    }

    private void addFreeFormField(BibDatabaseContext bibDatabaseContext, BibEntry entry, @Nullable String fieldName) {
        String trimmed = fieldName == null ? "" : fieldName.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        showFieldEditor(bibDatabaseContext, entry, FieldFactory.parseField(entry.getType(), trimmed));
    }

    /// Makes an editor for `field` visible in the list (adding it as still-empty user-added
    /// field if necessary) and focuses it.
    private void showFieldEditor(BibDatabaseContext bibDatabaseContext, BibEntry entry, Field field) {
        userAddedFields.add(field);
        rebuildPanel(bibDatabaseContext, entry);
        Platform.runLater(() -> requestFocus(field));
    }

    private void rebuildPanel(BibDatabaseContext bibDatabaseContext, BibEntry entry) {
        setupPanel(bibDatabaseContext, entry, false);
    }

    // endregion

    private BibDatabaseMode getDatabaseMode() {
        return stateManager.getActiveDatabase()
                           .map(BibDatabaseContext::getMode)
                           .orElse(BibDatabaseMode.BIBLATEX);
    }

    private BibDatabaseContext activeDatabaseContext() {
        return stateManager.getActiveDatabase().orElse(new BibDatabaseContext());
    }

    private static void applyNaturalHeight(FieldEditorFX editor) {
        normalizeInputHeights(editor.getNode());
        if ((editor.getWeight() > 1) && (editor.getNode() instanceof Region region)) {
            region.setPrefHeight(editor.getWeight() * HEIGHT_PER_WEIGHT);
        }
    }

    /// The classic stretch layout lets text inputs fill their percent-height rows by setting
    /// an infinite pref height ({@link org.jabref.gui.fieldeditors.EditorTextField}); in the
    /// natural-height list that blows up the rows' preferred heights, so reset text fields to
    /// their computed size and cap text areas at a few visible rows.
    private static void normalizeInputHeights(Node node) {
        if (node instanceof TextArea textArea) {
            textArea.setPrefRowCount(MULTILINE_ROWS);
            textArea.setPrefHeight(Region.USE_COMPUTED_SIZE);
        } else if (node instanceof TextField textField) {
            textField.setPrefHeight(Region.USE_COMPUTED_SIZE);
        } else if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(AllFieldsTab::normalizeInputHeights);
        }
    }
}
