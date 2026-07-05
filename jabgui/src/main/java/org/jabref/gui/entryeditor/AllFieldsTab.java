package org.jabref.gui.entryeditor;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
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
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;

import com.google.common.eventbus.Subscribe;
import org.jspecify.annotations.Nullable;

/// The single scroll-list tab ("Main") showing *all* fields of an entry (issue #12711):
/// the citation key, all required fields (even when unset), and every set field.
/// Replaces the classic category tabs (required / optional / other / …) as the default view.
///
/// Below the main fields sits a chip bar for adding unset optional fields ("Show more"
/// reveals the secondary-optional ones). The identifiers, files & links, and comments
/// groups are always-present collapsible sections — collapsed when empty — each with its
/// own add-chips for its unset member fields. A free-form field-name box at the bottom
/// adds arbitrary fields.
public class AllFieldsTab extends FieldsEditorTab {

    /// Preferred number of visible text rows for multiline editors in the scroll list
    /// (instead of the JavaFX TextArea default of 10).
    private static final int MULTILINE_ROWS = 4;

    /// Pixels of preferred height granted per weight unit for editors with weight > 1
    /// (e.g. the linked-files list), since percent-height rows do not exist in the scroll list.
    private static final double HEIGHT_PER_WEIGHT = 60;

    private final BibEntryTypesManager entryTypesManager;
    private final GuiPreferences guiPreferences;

    /// The current user's personal comment field (derived from the default-owner preference).
    private final UserSpecificCommentField userSpecificCommentField;

    /// Fields the user added via chip / free-form box that are still empty: they are not part
    /// of {@link BibEntry#getFields()} yet, but must stay visible while this entry is edited.
    private final Set<Field> userAddedFields = new LinkedHashSet<>();
    private @Nullable BibEntry entryOfUserAddedFields;

    /// Manual expand/collapse decisions per section, kept across rebuilds for the current
    /// entry (cleared on entry switch). Without an override, a section is expanded iff it
    /// contains at least one shown field.
    private final Map<FieldListSections.SectionType, Boolean> sectionExpandOverrides =
            new EnumMap<>(FieldListSections.SectionType.class);

    /// Sticky per tab instance: whether the secondary-optional chips are expanded.
    private boolean showSecondaryOptionalChips;

    /// The entry whose event bus this tab is currently subscribed to (for live refresh
    /// when fields are set/unset from outside, e.g. Source tab, fetchers, undo).
    private @Nullable BibEntry subscribedEntry;

    /// Scroll content: main grid + chip bar + section panes + free-form add row.
    private @Nullable VBox listContainer;

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
        this.guiPreferences = preferences;
        String defaultOwner = preferences.getOwnerPreferences().getDefaultOwner()
                                         .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "-");
        this.userSpecificCommentField = new UserSpecificCommentField(defaultOwner);

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
            sectionExpandOverrides.clear();
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
    protected Node getEditorContent() {
        if (listContainer == null) {
            listContainer = new VBox();
            listContainer.getStyleClass().add("all-fields-container");
        }
        return listContainer;
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

    /// Main fields as a grid with natural row heights, then the optional-field chip bar,
    /// then the always-present collapsible sections (identifiers / files & links / comments,
    /// collapsed when empty) each with its own add-chips, then the free-form add row.
    /// The whole column scrolls instead of stretching to the tab height.
    @Override
    protected void layoutEditors(BibDatabaseContext bibDatabaseContext, BibEntry entry, boolean compressed, List<Label> labels) {
        // labels were created in editors-map iteration order (see FieldsEditorTab#setupPanel)
        Map<Field, Label> labelForField = new LinkedHashMap<>();
        int labelIndex = 0;
        for (Field field : editors.keySet()) {
            labelForField.put(field, labels.get(labelIndex));
            labelIndex++;
        }

        Map<FieldListSections.SectionType, SequencedSet<Field>> buckets =
                new EnumMap<>(FieldListSections.SectionType.class);
        for (FieldListSections.SectionType type : FieldListSections.SectionType.values()) {
            buckets.put(type, new LinkedHashSet<>());
        }
        editors.keySet().forEach(field -> buckets.get(FieldListSections.sectionOf(field)).add(field));

        // Main section rows go into the (already cleared) inherited gridPane
        if (!gridPane.getStyleClass().contains("all-fields-list")) {
            gridPane.getStyleClass().add("all-fields-list");
        }
        addFieldRows(gridPane, buckets.get(FieldListSections.SectionType.MAIN), labelForField);

        VBox container = (VBox) getEditorContent();
        container.getChildren().setAll(gridPane, createMainChipBar(bibDatabaseContext, entry));
        for (FieldListSections.SectionType type : FieldListSections.SectionType.values()) {
            if (type == FieldListSections.SectionType.MAIN) {
                continue;
            }
            container.getChildren().add(
                    createSectionPane(type, buckets.get(type), labelForField, bibDatabaseContext, entry));
        }
        container.getChildren().add(createFreeFormAddRow(bibDatabaseContext, entry));

        editors.values().forEach(AllFieldsTab::applyNaturalHeight);
    }

    /// Label/editor rows with natural heights, label column as narrow as its content.
    private void addFieldRows(GridPane grid, SequencedCollection<Field> fields, Map<Field, Label> labelForField) {
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(Region.USE_PREF_SIZE);
        ColumnConstraints editorColumn = new ColumnConstraints();
        editorColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().setAll(labelColumn, editorColumn);

        int row = 0;
        for (Field field : fields) {
            Label label = labelForField.get(field);
            // FieldNameLabel sets prefHeight to infinity to fill the stretch layout's
            // percent-height rows; in the natural-height list that would blow up every
            // row's preferred height, so reset it to the computed size.
            label.setPrefHeight(Region.USE_COMPUTED_SIZE);
            GridPane.setValignment(label, VPos.TOP);
            grid.add(label, 0, row);
            grid.add(editors.get(field).getNode(), 1, row);
            row++;
        }
    }

    // region sections

    /// An always-present collapsible section: its shown fields as rows plus add-chips for
    /// its unset member fields. Collapsed by default when it contains no field; a manual
    /// expand/collapse survives rebuilds until another entry is opened.
    private TitledPane createSectionPane(FieldListSections.SectionType type,
                                         SequencedSet<Field> shownFields,
                                         Map<Field, Label> labelForField,
                                         BibDatabaseContext bibDatabaseContext,
                                         BibEntry entry) {
        VBox content = new VBox();
        content.getStyleClass().add("all-fields-section-content");

        if (!shownFields.isEmpty()) {
            GridPane sectionGrid = new GridPane();
            sectionGrid.setHgap(10);
            sectionGrid.setVgap(8);
            addFieldRows(sectionGrid, shownFields, labelForField);
            content.getChildren().add(sectionGrid);
        }

        SequencedSet<Field> chipFields = FieldListSections.subtract(sectionMemberFields(type), editors.keySet());
        if (!chipFields.isEmpty()) {
            FlowPane chips = new FlowPane();
            chips.getStyleClass().add("all-fields-add-chips");
            chipFields.forEach(field -> chips.getChildren().add(createAddChip(bibDatabaseContext, entry, field)));
            content.getChildren().add(chips);
        }

        TitledPane pane = new TitledPane(type.header().orElse(""), content);
        pane.getStyleClass().add("all-fields-section-pane");
        pane.setCollapsible(true);
        pane.setAnimated(false);
        pane.setExpanded(sectionExpandOverrides.getOrDefault(type, !shownFields.isEmpty()));
        pane.expandedProperty().addListener((_, _, expanded) -> sectionExpandOverrides.put(type, expanded));
        return pane;
    }

    /// All member fields of a section offered as add-chips; the comments section offers the
    /// general comment plus the current user's personal comment field (if enabled).
    private SequencedSet<Field> sectionMemberFields(FieldListSections.SectionType type) {
        if (type == FieldListSections.SectionType.COMMENTS) {
            SequencedSet<Field> commentFields = new LinkedHashSet<>();
            commentFields.add(StandardField.COMMENT);
            if (guiPreferences.getEntryEditorPreferences().shouldShowUserCommentsFields()) {
                commentFields.add(userSpecificCommentField);
            }
            return commentFields;
        }
        return FieldListSections.fieldsOf(type);
    }

    // endregion

    // region add-field controls

    /// Chips for the entry type's unset optional fields that belong to the main section
    /// (identifier/file/comment fields get their chips inside their own section);
    /// "Show more" reveals the secondary-optional ones.
    private Node createMainChipBar(BibDatabaseContext bibDatabaseContext, BibEntry entry) {
        BibDatabaseMode mode = getDatabaseMode();
        Optional<BibEntryType> entryType = entryTypesManager.enrich(entry.getType(), mode);

        FlowPane chips = new FlowPane();
        chips.getStyleClass().add("all-fields-add-chips");

        if (entryType.isPresent()) {
            List<Field> shown = List.copyOf(editors.keySet());
            FieldListSections.subtract(entryType.get().getImportantOptionalFields(), shown).stream()
                             .filter(field -> FieldListSections.sectionOf(field) == FieldListSections.SectionType.MAIN)
                             .forEach(field -> chips.getChildren().add(createAddChip(bibDatabaseContext, entry, field)));

            List<Field> secondary = FieldListSections.subtract(
                                                             entryType.get().getDetailOptionalNotDeprecatedFields(mode), shown).stream()
                                                     .filter(field -> FieldListSections.sectionOf(field) == FieldListSections.SectionType.MAIN)
                                                     .toList();
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

        return chips;
    }

    private Node createFreeFormAddRow(BibDatabaseContext bibDatabaseContext, BibEntry entry) {
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
        return freeFormRow;
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
