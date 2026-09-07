package org.jabref.gui.mergeentries.multiwaymerge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.mergeentries.DiffMode;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldTextMapper;
import org.jabref.model.entry.field.StandardField;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.EasyObservableValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiMergeEntriesView extends BaseDialog<BibEntry> {
    public static final int ACTIVE_COLUMNS_MINIMUM = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiMergeEntriesView.class);

    // LEFT
    @FXML private ScrollPane leftScrollPane;
    @FXML private VBox fieldHeader;

    // CENTER
    @FXML private ScrollPane topScrollPane;
    @FXML private HBox supplierHeader;
    @FXML private ScrollPane centerScrollPane;
    @FXML private GridPane optionsGrid;

    // RIGHT
    @FXML private ScrollPane rightScrollPane;
    @FXML private VBox fieldEditor;

    @FXML private Label failedSuppliers;
    @FXML private ComboBox<DiffMode> diffMode;

    private final ToggleGroup headerToggleGroup = new ToggleGroup();
    private final HashMap<Field, FieldRow> fieldRows = new HashMap<>();

    private final MultiMergeEntriesViewModel viewModel;
    private final TaskExecutor taskExecutor;

    private final GuiPreferences preferences;

    public MultiMergeEntriesView(GuiPreferences preferences,
                                 TaskExecutor taskExecutor) {
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;

        viewModel = new MultiMergeEntriesViewModel();

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ButtonType mergeEntries = new ButtonType(Localization.lang("Merge entries"), ButtonBar.ButtonData.OK_DONE);
        this.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, mergeEntries);
        this.setResultConverter(viewModel::resultConverter);

        viewModel.entriesProperty().addListener((ListChangeListener<MultiMergeEntriesViewModel.EntrySource>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (MultiMergeEntriesViewModel.EntrySource entrySourceColumn : c.getAddedSubList()) {
                        addColumn(entrySourceColumn);
                    }
                }
            }
        });

        viewModel.mergedEntryProperty().get().getFieldsObservable().addListener((MapChangeListener<Field, String>) change -> {
            if (change.wasAdded() && !fieldRows.containsKey(change.getKey())) {
                Field newField = change.getKey();
                // UI rows are never removed even if a merged field gets cleared again. Therefore the next row index
                // must be based on the current UI row count instead of the current merged-entry field count.
                int nextRowIndex = fieldEditor.getChildren().size();
                FieldRow fieldRow = new FieldRow(newField, nextRowIndex);
                fieldRows.put(change.getKey(), fieldRow);
                addEmptyCellsForMissingSources(newField);
            }
        });
    }

    @FXML
    public void initialize() {
        topScrollPane.hvalueProperty().bindBidirectional(centerScrollPane.hvalueProperty());
        leftScrollPane.vvalueProperty().bindBidirectional(centerScrollPane.vvalueProperty());
        rightScrollPane.vvalueProperty().bindBidirectional(centerScrollPane.vvalueProperty());

        viewModel.failedSuppliersProperty().addListener((_, _, _) -> {
            failedSuppliers.setText(viewModel.failedSuppliersProperty().get().isEmpty() ? "" : Localization.lang(
                    "Could not extract Metadata from: %0",
                    String.join(", ", viewModel.failedSuppliersProperty())));
            int activeColumns = viewModel.entriesProperty().get().size() - viewModel.failedSuppliersProperty().get().size();
            if (activeColumns < ACTIVE_COLUMNS_MINIMUM) {
                // [impl->req~ux.auto-close.merge-entries~1]
                close();
            }
        });
        fillDiffModes();
    }

    private void fillDiffModes() {
        diffMode.setItems(FXCollections.observableList(List.of(
                DiffMode.PLAIN,
                DiffMode.WORD,
                DiffMode.CHARACTER)));
        new ViewModelListCellFactory<DiffMode>()
                .withText(DiffMode::getDisplayText)
                .install(diffMode);
        diffMode.setValue(preferences.getMergeDialogPreferences().getMergeDiffMode());
        EasyBind.subscribe(this.diffMode.valueProperty(), mode -> preferences.getMergeDialogPreferences().setMergeDiffMode(mode));
    }

    private void addColumn(MultiMergeEntriesViewModel.EntrySource entrySourceColumn) {
        // add header
        int columnIndex = supplierHeader.getChildren().size();
        ToggleButton header = generateEntryHeader(entrySourceColumn, columnIndex);
        header.getStyleClass().addAll("toggle-button", "padding-6");
        HBox.setHgrow(header, Priority.ALWAYS);
        supplierHeader.getChildren().add(header);
        header.setMinWidth(250);

        // setup column constraints
        ColumnConstraints constraint = new ColumnConstraints();
        constraint.setMinWidth(Control.USE_PREF_SIZE);
        constraint.setMaxWidth(Control.USE_PREF_SIZE);
        constraint.prefWidthProperty().bind(header.widthProperty());
        optionsGrid.getColumnConstraints().add(constraint);

        // Copy BibEntry to column - either immediately (if available) or after loading
        if (!entrySourceColumn.isLoadingProperty().getValue()) {
            writeBibEntryToColumn(entrySourceColumn, columnIndex);
        } else {
            header.setDisable(true);
            entrySourceColumn.isLoadingProperty().addListener((_, _, newValue) -> {
                if (!newValue && entrySourceColumn.entryProperty().get() != null) {
                    writeBibEntryToColumn(entrySourceColumn, columnIndex);
                    header.setDisable(false);
                }
            });
        }
    }

    private ToggleButton generateEntryHeader(MultiMergeEntriesViewModel.EntrySource column, int columnIndex) {
        ToggleButton header = new ToggleButton();
        header.setToggleGroup(headerToggleGroup);
        header.textProperty().bind(column.titleProperty());
        setupSourceButtonAction(header, columnIndex);

        if (column.isLoadingProperty().getValue()) {
            ProgressIndicator progressIndicator = new ProgressIndicator(-1);
            progressIndicator.setPrefHeight(20);
            progressIndicator.setMinHeight(Control.USE_PREF_SIZE);
            progressIndicator.setMaxHeight(Control.USE_PREF_SIZE);
            header.setGraphic(progressIndicator);
            progressIndicator.visibleProperty().bind(column.isLoadingProperty());
        }

        column.isLoadingProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue) {
                header.setGraphic(null);
                if (column.entryProperty().get() == null) {
                    header.setMinWidth(0);
                    header.setMaxWidth(0);
                    header.setVisible(false);
                }
            }
        });

        return header;
    }

    /// Adds ToggleButtons for all fields that are set for this BibEntry
    ///
    /// @param entrySourceColumn the entry to write
    /// @param columnIndex       the index of the column to write this entry to
    private void writeBibEntryToColumn(MultiMergeEntriesViewModel.EntrySource entrySourceColumn, int columnIndex) {
        BibEntry entry = entrySourceColumn.entryProperty().get();
        for (Map.Entry<Field, String> fieldEntry : entry.getFieldsObservable().entrySet()) {
            Field field = fieldEntry.getKey();
            String value = fieldEntry.getValue();
            Cell cell = new Cell(value, field, columnIndex);
            optionsGrid.add(cell, columnIndex, fieldRows.get(field).rowIndex);
        }

        addEmptyCellsForMissingFields(entry, columnIndex);
    }

    private void addEmptyCellsForMissingSources(Field field) {
        List<MultiMergeEntriesViewModel.EntrySource> sources = viewModel.entriesProperty().get();
        for (int columnIndex = 0; columnIndex < sources.size(); columnIndex++) {
            BibEntry sourceEntry = sources.get(columnIndex).entryProperty().get();
            if ((sourceEntry != null) && sourceEntry.getField(field).isEmpty()) {
                addEmptyCellIfMissing(field, columnIndex);
            }
        }
    }

    private void addEmptyCellsForMissingFields(BibEntry entry, int columnIndex) {
        for (Field field : fieldRows.keySet()) {
            if (entry.getField(field).isEmpty()) {
                addEmptyCellIfMissing(field, columnIndex);
            }
        }
    }

    // [impl->req~ux.merge-entries.select-empty-field~1]
    private void addEmptyCellIfMissing(Field field, int columnIndex) {
        UiTaskExecutor.runAndWaitInJavaFXThread(() -> {
            FieldRow fieldRow = fieldRows.get(field);
            if ((fieldRow == null) || (columnIndex >= supplierHeader.getChildren().size())) {
                return;
            }

            boolean cellAlreadyExists = optionsGrid.getChildrenUnmodifiable().stream()
                                                   .filter(HBox.class::isInstance)
                                                   .anyMatch(node -> Objects.equals(GridPane.getColumnIndex(node), columnIndex)
                                                           && Objects.equals(GridPane.getRowIndex(node), fieldRow.rowIndex));
            if (!cellAlreadyExists) {
                optionsGrid.add(new Cell("", field, columnIndex), columnIndex, fieldRow.rowIndex);
            }
        });
    }

    /// Set up the button that displays the name of the source so that if it is clicked, all toggles in that column are
    /// selected.
    ///
    /// @param sourceButton the header button to setup
    /// @param column       the column this button is heading
    private void setupSourceButtonAction(ToggleButton sourceButton, int column) {
        sourceButton.selectedProperty().addListener((_, _, newValue) -> {
            if (newValue) {
                optionsGrid.getChildrenUnmodifiable().stream()
                           .filter(node -> GridPane.getColumnIndex(node) == column)
                           .filter(HBox.class::isInstance)
                           .forEach(hbox -> ((HBox) hbox).getChildrenUnmodifiable().stream()
                                                         .filter(ToggleButton.class::isInstance)
                                                         .forEach(toggleButton -> ((ToggleButton) toggleButton).setSelected(true)));
                sourceButton.setSelected(true);
            }
        });
    }

    /// Checks if the Field can be multiline
    ///
    /// @param field the field to be checked
    /// @return true if the field may be multiline, false otherwise
    private boolean isMultilineField(Field field) {
        if (field.equals(StandardField.DOI)) {
            return false;
        }
        return FieldFactory.isMultiLineField(field, preferences.getFieldPreferences().getNonWrappableFields());
    }

    private class Cell extends HBox {

        private final String content;

        public Cell(String content, Field field, int columnIndex) {
            this.content = content;

            /*
            If this is not explicitly done on the JavaFX thread, the bindings to the text fields don't work properly.
            The text only shows up after one text in that same row is selected by the user.
             */
            UiTaskExecutor.runInJavaFXThread(() -> {

                FieldRow row = fieldRows.get(field);

                prefWidthProperty().bind(((Region) supplierHeader.getChildren().get(columnIndex)).widthProperty());
                setMinWidth(Control.USE_PREF_SIZE);
                setMaxWidth(Control.USE_PREF_SIZE);
                prefHeightProperty().bind(((Region) fieldEditor.getChildren().get(row.rowIndex)).heightProperty());
                setMinHeight(Control.USE_PREF_SIZE);
                setMaxHeight(Control.USE_PREF_SIZE);

                // Button
                ToggleButton cellButton = new ToggleButton();
                cellButton.prefHeightProperty().bind(heightProperty());
                cellButton.setMinHeight(Control.USE_PREF_SIZE);
                cellButton.setMaxHeight(Control.USE_PREF_SIZE);
                cellButton.setGraphicTextGap(0);
                getChildren().add(cellButton);
                cellButton.maxWidthProperty().bind(widthProperty());
                if (content.isEmpty()) {
                    cellButton.prefWidthProperty().bind(widthProperty());
                }
                HBox.setHgrow(cellButton, Priority.ALWAYS);

                // Text
                DiffHighlightingEllipsingTextFlow buttonText = new DiffHighlightingEllipsingTextFlow(content, viewModel.mergedEntryProperty().get().getFieldBinding(field).asOrdinary(), diffMode.valueProperty());

                buttonText.maxWidthProperty().bind(widthProperty().add(-10));
                buttonText.maxHeightProperty().bind(heightProperty());
                cellButton.setGraphic(buttonText);
                cellButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                cellButton.setContentDisplay(ContentDisplay.CENTER);

                // Tooltip
                Tooltip buttonTooltip = new Tooltip(content);
                buttonTooltip.setWrapText(true);
                buttonTooltip.prefWidthProperty().bind(widthProperty());
                buttonTooltip.setTextAlignment(TextAlignment.LEFT);
                cellButton.setTooltip(buttonTooltip);

                cellButton.setToggleGroup(row.toggleGroup);
                // An empty cell is only added so the user can explicitly clear the merged field. Auto-selecting it
                // would clear the value that was just merged (rows are created as a reaction to that value being set).
                if (!content.isEmpty() && (row.toggleGroup.getSelectedToggle() == null)) {
                    cellButton.setSelected(true);
                }

                if (field.equals(StandardField.DOI) && !content.isEmpty()) {
                    Button doiButton = ControlHelper.iconButton(IconTheme.JabRefIcons.LOOKUP_IDENTIFIER);
                    HBox.setHgrow(doiButton, Priority.NEVER);
                    doiButton.prefHeightProperty().bind(cellButton.heightProperty());
                    doiButton.setMinHeight(Control.USE_PREF_SIZE);
                    doiButton.setMaxHeight(Control.USE_PREF_SIZE);

                    getChildren().add(doiButton);

                    doiButton.setOnAction(_ -> {
                        DoiFetcher doiFetcher = new DoiFetcher(preferences.getImportFormatPreferences());
                        doiButton.setDisable(true);
                        addSource(Localization.lang("From DOI"), () -> {
                            try {
                                return doiFetcher.performSearchById(content).get();
                            } catch (FetcherException | NoSuchElementException e) {
                                LOGGER.warn("Failed to fetch BibEntry for DOI {}", content, e);
                                return null;
                            }
                        });
                    });
                }
            });
        }

        public String getContent() {
            return content;
        }
    }

    public void addSource(String title, BibEntry entry) {
        viewModel.addSource(new MultiMergeEntriesViewModel.EntrySource(title, entry));
    }

    public void addSource(String title, Supplier<BibEntry> supplier) {
        viewModel.addSource(new MultiMergeEntriesViewModel.EntrySource(title, supplier, taskExecutor));
    }

    public void addFetchedColumn(String title, IdBasedFetcher fetcher, String id) {
        addSource(title, () -> {
            try {
                return fetcher.performSearchById(id).orElse(null);
            } catch (FetcherException e) {
                LOGGER.warn("Failed to fetch BibEntry for {} using {}", id, title, e);
                return null;
            }
        });
    }

    /// Starts automatic identifier fetching for the columns already added via [#addSource]. Must be called after
    /// all initial sources have been added, since it only considers the columns present at call time.
    public void enableAutomaticIdentifierFetching() {
        for (MultiMergeEntriesViewModel.EntrySource entrySourceColumn : List.copyOf(viewModel.entriesProperty())) {
            if (!entrySourceColumn.isLoadingProperty().getValue()) {
                triggerAutomaticIdentifierFetch(entrySourceColumn);
            } else {
                entrySourceColumn.isLoadingProperty().addListener(new ChangeListener<>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean isLoading) {
                        if (!isLoading) {
                            entrySourceColumn.isLoadingProperty().removeListener(this);
                            triggerAutomaticIdentifierFetch(entrySourceColumn);
                        }
                    }
                });
            }
        }
    }

    private void triggerAutomaticIdentifierFetch(MultiMergeEntriesViewModel.EntrySource entrySourceColumn) {
        BibEntry entry = entrySourceColumn.entryProperty().get();
        if (entry == null) {
            return;
        }
        viewModel.findNewFetchableIdentifiers(entry).forEach((field, value) ->
                WebFetchers.getIdBasedFetcherForField(field, preferences.getImportFormatPreferences())
                           .ifPresent(fetcher -> addFetchedColumn(
                                   Localization.lang("From %0", FieldTextMapper.getDisplayName(field)), fetcher, value)));
    }

    private class FieldRow {

        public final ToggleGroup toggleGroup = new ToggleGroup();
        private final TextInputControl fieldEditorCell;

        private final int rowIndex;

        // Reference needs to be kept, since java garbage collection would otherwise destroy the subscription
        @SuppressWarnings("FieldCanBeLocal") private EasyObservableValue<String> fieldBinding;

        public FieldRow(Field field, int rowIndex) {
            this.rowIndex = rowIndex;

            // setup field editor column entry
            boolean isMultiLine = isMultilineField(field);
            if (isMultiLine) {
                fieldEditorCell = new TextArea();
                ((TextArea) fieldEditorCell).setWrapText(true);
            } else {
                fieldEditorCell = new TextField();
            }

            addRow(field);

            fieldEditorCell.addEventFilter(KeyEvent.KEY_PRESSED, event -> toggleGroup.selectToggle(null));

            toggleGroup.selectedToggleProperty().addListener((_, _, newValue) -> {
                if (newValue == null) {
                    viewModel.setMergedFieldValue(field, null);
                } else {
                    viewModel.setMergedFieldValue(field, ((DiffHighlightingEllipsingTextFlow) ((ToggleButton) newValue).getGraphic()).getFullText());
                    headerToggleGroup.selectToggle(null);
                }
            });
        }

        /// Adds a row that represents this field
        ///
        /// @param field the field to add to the view as a new row in the table
        private void addRow(Field field) {
            VBox.setVgrow(fieldEditorCell, Priority.ALWAYS);

            fieldBinding = viewModel.mergedEntryProperty().get().getFieldBinding(field).asOrdinary();
            BindingsHelper.bindBidirectional(
                    fieldEditorCell.textProperty(),
                    fieldBinding,
                    text -> fieldEditorCell.setText(text == null ? "" : text),
                    binding -> viewModel.setMergedFieldValue(field, binding));

            fieldEditorCell.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(fieldEditorCell, Priority.ALWAYS);
            fieldEditor.getChildren().add(fieldEditorCell);

            // setup header label
            Label fieldHeaderLabel = new Label(FieldTextMapper.getDisplayName(field));
            fieldHeaderLabel.prefHeightProperty().bind(fieldEditorCell.heightProperty());
            fieldHeaderLabel.setMaxWidth(Control.USE_PREF_SIZE);
            fieldHeaderLabel.setMinWidth(Control.USE_PREF_SIZE);
            fieldHeader.getChildren().add(fieldHeaderLabel);

            // setup RowConstraints
            RowConstraints constraint = new RowConstraints();
            constraint.setMinHeight(Control.USE_PREF_SIZE);
            constraint.setMaxHeight(Control.USE_PREF_SIZE);
            constraint.prefHeightProperty().bind(fieldEditorCell.heightProperty());
            optionsGrid.getRowConstraints().add(constraint);
        }
    }
}
