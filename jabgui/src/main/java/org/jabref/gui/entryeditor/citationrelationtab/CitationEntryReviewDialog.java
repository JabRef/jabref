package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.entryeditor.FieldListSections;
import org.jabref.gui.fieldeditors.EditorTextArea;
import org.jabref.gui.fieldeditors.FieldEditorFX;
import org.jabref.gui.fieldeditors.FieldEditors;
import org.jabref.gui.fieldeditors.FieldNameLabel;
import org.jabref.gui.fieldeditors.LinkedFilesEditor;
import org.jabref.gui.fieldeditors.TagsEditor;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.undo.JabRefGuiUndoManager;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.FieldsUtil;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.model.entry.types.EntryType;

import com.airhacks.afterburner.injection.Injector;
import com.google.common.eventbus.Subscribe;
import org.jspecify.annotations.NullMarked;

/// GUI to shows one entry with its type and fields editable, so that the
/// metadata can be corrected before the entry becomes part of the library.
@NullMarked
public class CitationEntryReviewDialog extends BaseDialog<CitationEntryReviewDialog.Decision> {

    /// The user's choice for the entry currently under review.
    public enum Decision {
        /// Go back to the previous entry, leaving the choice for this one as it was.
        PREVIOUS,
        /// Leave this entry out and continue with the next one. On the last entry, the chosen entries are added.
        SKIP,
        /// Add this entry with its edits and continue with the next one. On the last entry, the chosen entries are added.
        NEXT,
        /// End the review without adding any entry.
        CANCEL
    }

    private static final double LABEL_COLUMN_MIN_WIDTH = 110;
    private static final double EDITOR_COLUMN_MIN_WIDTH = 360;
    private static final int COLLAPSED_MULTILINE_ROWS = 5;
    private static final double HEIGHT_PER_WEIGHT = 60;
    private static final double FIELDS_DIVIDER_POSITION = 0.55;
    private static final double DIALOG_PREF_WIDTH = 1100;
    private static final double DIALOG_PREF_HEIGHT = 650;
    private static final double GRID_HGAP = 10;
    private static final double GRID_VGAP = 6;
    private static final double FIELD_AREA_SPACING = 8;

    /// List of fields that will be chips
    private static final List<Field> COMMON_CHIP_FIELDS = List.of(StandardField.ABSTRACT);

    /// Characters not allowed in the name of the user specific comment field.
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]");

    /// Multiline fields the user has focused, and thereby expanded. The rows are rebuilt after a chip
    /// click or a type change, so the expansion must outlive the editors.
    private final Set<Field> expandedFields = new HashSet<>();

    /// Last laid-out width of each field's growing text area
    private final Map<Field, Double> textAreaWidths = new HashMap<>();

    private final BibEntry entry;
    private final BibDatabaseContext databaseContext;
    private final BibEntryTypesManager entryTypesManager;
    private final GuiPreferences preferences;
    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final SuggestionProviders suggestionProviders;
    private final UndoAction undoAction;
    private final RedoAction redoAction;

    private final ComboBox<EntryType> entryTypeCombo = new ComboBox<>();

    /// The entry is not part of the library yet, so its edits are kept out of the library's undo history.
    private final JabRefGuiUndoManager reviewUndoManager = new JabRefGuiUndoManager();

    /// Editor of every shown field, in display order
    private final Map<Field, FieldEditorFX> editors = new LinkedHashMap<>();

    private final GridPane fieldGrid = new GridPane();
    private final VBox fieldArea = new VBox(FIELD_AREA_SPACING);

    /// Fields without a value that are shown anyway: added with a chip, or cleared by the user
    private final Set<Field> fieldsShownWithoutValue = new LinkedHashSet<>();
    private boolean showSecondaryOptionalChips;

    /// The current user's personal comment field, offered as a chip of the comments section.
    private final UserSpecificCommentField userSpecificCommentField;

    /// Expand state of each section, so that the rebuild after a chip click does not collapse a section
    private final Map<FieldListSections.SectionType, Boolean> sectionExpandOverrides =
            new EnumMap<>(FieldListSections.SectionType.class);

    /// Function to build the review dialog for one fetched entry
    ///
    /// @param entry             copy of the fetched entry, edited in place
    /// @param position          number of this entry within the review
    /// @param total             number of entries to review
    /// @param databaseContext   library the entry is reviewed for
    /// @param entryTypesManager source of the selectable entry types
    /// @param preferences       preferences the field editors are built from
    /// @param stateManager      source of the library's autocompletion suggestions
    /// @param dialogService     service the preview panel and the undo actions report through
    /// @param taskExecutor      executor the preview panel renders on
    public CitationEntryReviewDialog(BibEntry entry,
                                     int position,
                                     int total,
                                     BibDatabaseContext databaseContext,
                                     BibEntryTypesManager entryTypesManager,
                                     GuiPreferences preferences,
                                     StateManager stateManager,
                                     DialogService dialogService,
                                     TaskExecutor taskExecutor) {
        this.entry = entry;
        this.databaseContext = databaseContext;
        this.entryTypesManager = entryTypesManager;
        this.preferences = preferences;
        this.journalAbbreviationRepository = Injector.instantiateModelOrService(JournalAbbreviationRepository.class);
        this.suggestionProviders = stateManager.activeTabProperty().get()
                                               .map(LibraryTab::getSuggestionProviders)
                                               .orElse(new SuggestionProviders());
        this.undoAction = createUndoAction(dialogService, stateManager);
        this.redoAction = createRedoAction(dialogService, stateManager);
        String defaultOwner = NON_ALPHANUMERIC.matcher(
                preferences.getOwnerPreferences().getDefaultOwner().toLowerCase(Locale.ROOT)).replaceAll("-");
        this.userSpecificCommentField = new UserSpecificCommentField(defaultOwner);

        setTitle(Localization.lang("Review entry %0 of %1", position, total));

        ButtonType previousButtonType = new ButtonType(Localization.lang("Previous entry"), ButtonBar.ButtonData.BACK_PREVIOUS);
        ButtonType skipButtonType = new ButtonType(Localization.lang("Skip entry"), ButtonBar.ButtonData.OTHER);
        // On the last entry there is no next one; moving on imports the chosen entries
        ButtonType nextButtonType = position == total
                                    ? new ButtonType(Localization.lang("Import entries"), ButtonBar.ButtonData.OK_DONE)
                                    : new ButtonType(Localization.lang("Next entry"), ButtonBar.ButtonData.NEXT_FORWARD);
        ButtonType cancelButtonType = new ButtonType(Localization.lang("End review without imports"), ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().setAll(previousButtonType, skipButtonType, nextButtonType, cancelButtonType);
        getDialogPane().lookupButton(previousButtonType).setDisable(position == 1);

        setUpEntryTypeCombo();
        getDialogPane().setContent(createContent(stateManager, dialogService, taskExecutor));
        getDialogPane().setPrefSize(DIALOG_PREF_WIDTH, DIALOG_PREF_HEIGHT);

        // Registered after the first build, so that neither reacts while the dialog is still being set up
        entryTypeCombo.valueProperty().addListener((_, _, selectedType) ->
                Optional.ofNullable(selectedType).ifPresent(this::applyEntryType));
        entry.registerListener(this);
        setOnHidden(_ -> entry.unregisterListener(this));

        setResultConverter(buttonType -> {
            // Kept whatever the decision is, so that the type is still chosen when the user comes back to this entry
            entry.setType(entryTypeCombo.getValue());
            if (previousButtonType.equals(buttonType)) {
                return Decision.PREVIOUS;
            }
            if (skipButtonType.equals(buttonType)) {
                return Decision.SKIP;
            }
            if (nextButtonType.equals(buttonType)) {
                return Decision.NEXT;
            }
            return Decision.CANCEL;
        });
    }

    /// Function to fill the entry type combo box with the types of the library's mode
    private void setUpEntryTypeCombo() {
        entryTypeCombo.getItems().setAll(entryTypesManager.getAllTypes(databaseContext.getMode()).stream()
                                                          .map(BibEntryType::getType)
                                                          .sorted(Comparator.comparing(EntryType::getDisplayName))
                                                          .toList());
        new ViewModelListCellFactory<EntryType>()
                .withText(EntryType::getDisplayName)
                .install(entryTypeCombo);
        entryTypeCombo.setValue(entry.getType());
        entryTypeCombo.setMaxWidth(Double.MAX_VALUE);
    }

    /// Function to build the dialog body: the entry summary above the field rows and the preview
    ///
    /// @param stateManager  source of the library state the preview panel reads
    /// @param dialogService service the preview panel reports through
    /// @param taskExecutor  executor the preview panel renders on
    private VBox createContent(StateManager stateManager, DialogService dialogService, TaskExecutor taskExecutor) {
        Label summary = new Label(entry.getAuthorTitleYear());
        summary.setWrapText(true);

        fieldArea.getStyleClass().addAll("all-fields-container", "padding-12");
        setupFieldArea();

        ScrollPane scrollPane = new ScrollPane(fieldArea);
        scrollPane.setFitToWidth(true);

        // The preview follows the edits, as next to the entry editor's fields
        PreviewPanel previewPanel = new PreviewPanel(dialogService, preferences.getKeyBindingRepository(), preferences, taskExecutor, stateManager);
        previewPanel.setDatabase(databaseContext);
        previewPanel.setEntry(entry);

        SplitPane fieldsAndPreview = new SplitPane(scrollPane, previewPanel);
        fieldsAndPreview.setDividerPositions(FIELDS_DIVIDER_POSITION);
        VBox.setVgrow(fieldsAndPreview, Priority.ALWAYS);

        VBox content = new VBox(10, summary, fieldsAndPreview);
        content.setPadding(new Insets(10));
        // The styling of the field rows, the sections and the chips is scoped to the entry editor, and
        // this dialog shows exactly that content
        content.getStyleClass().add("entry-editor");
        return content;
    }

    /// The label and the editor of one field, kept together while the rows are distributed over the
    /// sections they belong to.
    ///
    /// @param field  field of the row
    /// @param label  label shown in front of the editor
    /// @param editor editor of the field
    private record FieldRow(Field field, Label label, FieldEditorFX editor) {
    }

    /// Function to build the rows of the fields to show, replacing the rows of the previous run
    private void setupFieldArea() {
        editors.clear();
        fieldGrid.getChildren().clear();
        fieldGrid.getColumnConstraints().clear();

        Map<FieldListSections.SectionType, List<FieldRow>> rowsPerSection =
                new EnumMap<>(FieldListSections.SectionType.class);
        for (Field field : determineFieldsToShow()) {
            rowsPerSection.computeIfAbsent(FieldListSections.sectionOf(field), _ -> new ArrayList<>())
                          .add(createFieldRow(field));
        }

        layoutMainFields(rowsPerSection.getOrDefault(FieldListSections.SectionType.MAIN, List.of()));
        fieldArea.getChildren().setAll(fieldGrid, createMainChipBar());
        for (FieldListSections.SectionType type : FieldListSections.SectionType.values()) {
            if (type != FieldListSections.SectionType.MAIN) {
                fieldArea.getChildren().add(
                        createSectionPane(type, rowsPerSection.getOrDefault(type, List.of())));
            }
        }
        editors.forEach(this::applyNaturalHeight);
    }

    /// Function to determine the fields to show, and in which order, as the entry editor's "Main" tab does
    private SequencedSet<Field> determineFieldsToShow() {
        Set<Field> setFields = entry.getFields();
        SequencedSet<Field> fields = new LinkedHashSet<>();
        entryTypesManager.enrich(entry.getType(), databaseContext.getMode()).ifPresent(entryType -> {
            entryType.getRequiredFields().forEach(orFields -> fields.addAll(orFields.getFields()));
            entryType.getImportantOptionalFields().stream()
                     .filter(setFields::contains)
                     .forEach(fields::add);
            entryType.getDetailOptionalNotDeprecatedFields(databaseContext.getMode()).stream()
                     .filter(setFields::contains)
                     .forEach(fields::add);
        });
        setFields.stream()
                 .sorted(Comparator.comparing(Field::getName))
                 .forEach(fields::add);
        fields.addAll(fieldsShownWithoutValue);
        fields.removeIf(field -> field instanceof InternalField);
        return fields;
    }

    /// Function to create the label and the editor of one field, and to remember the editor
    ///
    /// @param field field the label and the editor belong to
    private FieldRow createFieldRow(Field field) {
        FieldEditorFX fieldEditor = FieldEditors.getForField(
                field,
                journalAbbreviationRepository,
                preferences,
                databaseContext,
                entry.getType(),
                suggestionProviders,
                reviewUndoManager,
                undoAction,
                redoAction);
        fieldEditor.bindToEntry(entry);
        editors.put(field, fieldEditor);
        return new FieldRow(field, new FieldNameLabel(field), fieldEditor);
    }

    /// Function to arrange the entry type row and the rows of the main group in the kept grid
    ///
    /// @param rows rows of the main group, in display order
    private void layoutMainFields(List<FieldRow> rows) {
        configureFieldGrid(fieldGrid);
        fieldGrid.add(new Label(Localization.lang("Entry type")), 0, 0);
        fieldGrid.add(entryTypeCombo, 1, 0);
        addFieldRows(fieldGrid, rows, 1);
    }

    /// Function to create the collapsible pane of one field group
    ///
    /// Holds the rows of the group and, below them, the chips for its fields without a row.
    /// Collapsed by default while the group has no row.
    ///
    /// @param type section the fields belong to
    /// @param rows rows of the section, in display order
    private TitledPane createSectionPane(FieldListSections.SectionType type, List<FieldRow> rows) {
        VBox sectionContent = new VBox(FIELD_AREA_SPACING);
        if (!rows.isEmpty()) {
            GridPane sectionGrid = new GridPane();
            configureFieldGrid(sectionGrid);
            addFieldRows(sectionGrid, rows, 0);
            sectionContent.getChildren().add(sectionGrid);
        }
        List<Field> chipFields = fieldsWithoutRow(sectionMemberFields(type)).toList();
        if (!chipFields.isEmpty()) {
            sectionContent.getChildren().add(createChips(chipFields));
        }

        TitledPane sectionPane = new TitledPane(type.header().orElseThrow(), sectionContent);
        sectionPane.getStyleClass().add("all-fields-section-pane");
        sectionPane.setAnimated(false);
        sectionPane.setExpanded(sectionExpandOverrides.getOrDefault(type, !rows.isEmpty()));
        sectionPane.expandedProperty().addListener((_, _, expanded) -> sectionExpandOverrides.put(type, expanded));
        return sectionPane;
    }

    /// Function to collect the fields a section offers chips for
    ///
    /// @param type section to offer chips for
    private SequencedSet<Field> sectionMemberFields(FieldListSections.SectionType type) {
        if (type == FieldListSections.SectionType.COMMENTS) {
            SequencedSet<Field> commentFields = new LinkedHashSet<>();
            commentFields.add(StandardField.COMMENT);
            if (preferences.getEntryEditorPreferences().shouldShowUserCommentsFields()) {
                commentFields.add(userSpecificCommentField);
            }
            return commentFields;
        }
        return FieldListSections.fieldsOf(type);
    }

    /// Function to give a grid the label column and the editor column
    ///
    /// @param grid grid the columns are set on
    private static void configureFieldGrid(GridPane grid) {
        grid.setHgap(GRID_HGAP);
        grid.setVgap(GRID_VGAP);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(LABEL_COLUMN_MIN_WIDTH);
        labelColumn.setHalignment(HPos.RIGHT);
        ColumnConstraints editorColumn = new ColumnConstraints();
        editorColumn.setMinWidth(EDITOR_COLUMN_MIN_WIDTH);
        editorColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().setAll(labelColumn, editorColumn);
    }

    /// Function to add one row per field to a grid
    ///
    /// @param grid     grid the rows are added to
    /// @param rows     rows to add, in display order
    /// @param firstRow index of the first free row of the grid
    private static void addFieldRows(GridPane grid, List<FieldRow> rows, int firstRow) {
        int row = firstRow;
        for (FieldRow fieldRow : rows) {
            // FieldNameLabel asks for unlimited height, which would make every row sized to its content unlimited too
            fieldRow.label().setPrefHeight(Region.USE_COMPUTED_SIZE);
            GridPane.setValignment(fieldRow.label(), VPos.TOP);
            grid.add(fieldRow.label(), 0, row);
            grid.add(fieldRow.editor().getNode(), 1, row);
            row++;
        }
    }

    /// Function to let an editor take the height of its content, as the entry editor's "Main" tab does
    ///
    /// @param field  field the editor belongs to
    /// @param editor editor to size
    private void applyNaturalHeight(Field field, FieldEditorFX editor) {
        normalizeInputHeights(field, editor.getNode());
        if (editor instanceof LinkedFilesEditor || editor instanceof TagsEditor) {
            // These size themselves to their content: file rows or wrapped chips. A fixed height would override that.
            return;
        }
        if ((editor.getWeight() > 1) && (editor.getNode() instanceof Region region)) {
            region.setPrefHeight(editor.getWeight() * HEIGHT_PER_WEIGHT);
        }
    }

    /// Function to reset the unlimited height the text inputs ask for, and to let text areas grow with their text
    ///
    /// @param field field the input belongs to
    /// @param node  editor node, searched for text inputs
    private void normalizeInputHeights(Field field, Node node) {
        switch (node) {
            case EditorTextArea textArea -> {
                textArea.setPrefHeight(Region.USE_COMPUTED_SIZE);
                // The width the field had before the rebuild lets the new area wrap correctly in its first frame
                textArea.setGrowWithContent(textAreaWidths.getOrDefault(field, -1.0), COLLAPSED_MULTILINE_ROWS);
                if (expandedFields.contains(field)) {
                    textArea.expand();
                }
                textArea.focusedProperty().addListener((_, _, focused) -> {
                    if (focused) {
                        expandedFields.add(field);
                    }
                });
                textArea.widthProperty().addListener((_, _, width) -> {
                    if (width.doubleValue() > 0) {
                        textAreaWidths.put(field, width.doubleValue());
                    }
                });
            }
            case TextField textField ->
                    textField.setPrefHeight(Region.USE_COMPUTED_SIZE);
            case Parent parent ->
                    parent.getChildrenUnmodifiable().forEach(child -> normalizeInputHeights(field, child));
            case null,
                 default -> {
            }
        }
    }

    /// Function to apply the entry type the user chose
    ///
    /// Which fields are required follows from the entry type, so the rows are built again.
    ///
    /// @param selectedType entry type chosen in the combo box
    private void applyEntryType(EntryType selectedType) {
        entry.setType(selectedType);
        setupFieldArea();
    }

    /// Function to create the buttons which add a field of the main group (chips)
    private Node createMainChipBar() {
        FlowPane chips = new FlowPane();
        chips.getStyleClass().add("gap-4");

        entryTypesManager.enrich(entry.getType(), databaseContext.getMode()).ifPresent(entryType -> {
            mainGroupChipFields(entryType.getImportantOptionalFields())
                    .forEach(field -> chips.getChildren().add(createAddChip(field)));
            fieldsWithoutRow(COMMON_CHIP_FIELDS)
                    .forEach(field -> chips.getChildren().add(createAddChip(field)));

            List<Field> secondaryFields = mainGroupChipFields(
                    entryType.getDetailOptionalNotDeprecatedFields(databaseContext.getMode())).toList();
            if (secondaryFields.isEmpty()) {
                return;
            }
            if (showSecondaryOptionalChips) {
                secondaryFields.forEach(field -> chips.getChildren().add(createAddChip(field)));
            }
            Hyperlink toggle = new Hyperlink(showSecondaryOptionalChips
                                             ? Localization.lang("Show less")
                                             : Localization.lang("Show more"));
            toggle.setOnAction(_ -> {
                showSecondaryOptionalChips = !showSecondaryOptionalChips;
                setupFieldArea();
            });
            chips.getChildren().add(toggle);
        });

        return chips;
    }

    /// Function to lay the chips of one group out side by side
    ///
    /// @param chipFields fields to offer a chip for, in display order
    private FlowPane createChips(List<Field> chipFields) {
        FlowPane chips = new FlowPane();
        chips.getStyleClass().add("gap-4");
        chipFields.forEach(field -> chips.getChildren().add(createAddChip(field)));
        return chips;
    }

    /// Function to pick the chip candidates that belong to the main group
    ///
    /// @param candidates fields a chip could be offered for
    private Stream<Field> mainGroupChipFields(Collection<Field> candidates) {
        return fieldsWithoutRow(candidates)
                .filter(field -> FieldListSections.sectionOf(field) == FieldListSections.SectionType.MAIN);
    }

    /// Function to pick the candidates that have no row at the moment
    ///
    /// @param candidates fields a chip could be offered for
    private Stream<Field> fieldsWithoutRow(Collection<Field> candidates) {
        return candidates.stream().filter(field -> !editors.containsKey(field));
    }

    /// Function to create the button which adds one field
    ///
    /// @param field field the button adds
    private Button createAddChip(Field field) {
        Button chip = new Button(Localization.lang("+ %0", FieldsUtil.getDisplayName(field)));
        chip.getStyleClass().addAll("all-fields-add-chip", "padding-4-12");
        chip.setOnAction(_ -> showFieldEditor(field));
        return chip;
    }

    /// Function to show the editor of a field the entry has no value for, and to focus it
    /// Used when a chip got clicked
    ///
    /// @param field field to show a row for
    private void showFieldEditor(Field field) {
        fieldsShownWithoutValue.add(field);
        setupFieldArea();
        // The outer runLater lets one pulse pass so the editors rebuilt become focusable
        // before the inner runLater requests focus on them.
        Platform.runLater(() -> Platform.runLater(() -> focusEditor(field)));
    }

    /// Function to move the keyboard focus into the editor of one field
    ///
    /// @param field field whose editor is focused
    private void focusEditor(Field field) {
        Optional.ofNullable(editors.get(field)).ifPresent(FieldEditorFX::focus);
    }

    /// Function to follow a change of the fields of the entry under review
    ///
    /// Looking up the bibliographic data of a DOI fills in fields that have no row yet, so the rows are
    /// built again. Only a changed set of fields causes that, so typing in an editor neither rebuilds the
    /// rows nor takes the focus away.
    ///
    /// @param event field change reported by the entry
    @Subscribe
    public void listen(FieldChangedEvent event) {
        if (event.getBibEntry() != entry) {
            return;
        }
        // A shown field whose value the user cleared keeps its row: otherwise deleting the last character
        // would take the editor away from under the cursor.
        if (editors.containsKey(event.getField()) && StringUtil.isBlank(event.getNewValue())) {
            fieldsShownWithoutValue.add(event.getField());
        }
        // The change arrives while the editor is still handling it, so the rebuild waits for the next pulse
        Platform.runLater(() -> {
            if (!determineFieldsToShow().equals(editors.keySet())) {
                setupFieldArea();
            }
        });
    }

    /// The editors trigger undo on the keyboard shortcut. The regular action would undo the last change of the library,
    /// so this one undoes the last change made in this dialog instead.
    private UndoAction createUndoAction(DialogService dialogService, StateManager stateManager) {
        return new UndoAction(dialogService, stateManager) {
            @Override
            public void execute() {
                reviewUndoManager.undo();
            }
        };
    }

    /// Counterpart of [#createUndoAction(DialogService, StateManager)] for redo.
    private RedoAction createRedoAction(DialogService dialogService, StateManager stateManager) {
        return new RedoAction(dialogService, stateManager) {
            @Override
            public void execute() {
                reviewUndoManager.redo();
            }
        };
    }
}
