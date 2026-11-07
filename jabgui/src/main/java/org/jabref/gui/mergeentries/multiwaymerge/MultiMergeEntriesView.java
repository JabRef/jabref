package org.jabref.gui.mergeentries.multiwaymerge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.jabref.gui.mergeentries.DiffMode;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldTextMapper;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
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
    private final HashMap<Field, MultiMergeFieldRow> fieldRows = new HashMap<>();

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
                MultiMergeFieldRow fieldRow = new MultiMergeFieldRow(
                        newField,
                        nextRowIndex,
                        viewModel,
                        headerToggleGroup,
                        fieldHeader,
                        fieldEditor,
                        optionsGrid,
                        preferences);
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
        header.getStyleClass().addAll("merge-header-cell", "toggle-button");
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

        column.isLoadingProperty().addListener((_, _, newValue) -> {
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
            MultiMergeFieldRow row = fieldRows.get(field);
            MultiMergeCell cell = createCell(value, field, columnIndex, row);
            optionsGrid.add(cell, columnIndex, row.getRowIndex());
        }

        addEmptyCellsForMissingFields(entry, columnIndex);
    }

    private MultiMergeCell createCell(String content, Field field, int columnIndex, MultiMergeFieldRow row) {
        Region headerRegion = (Region) supplierHeader.getChildren().get(columnIndex);
        Region editorRegion = (Region) fieldEditor.getChildren().get(row.getRowIndex());
        return new MultiMergeCell(
                content,
                field,
                columnIndex,
                row.getRowIndex(),
                row,
                headerRegion,
                editorRegion,
                row.getFieldBinding(),
                diffMode.valueProperty(),
                preferences,
                this::addSource);
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
            MultiMergeFieldRow fieldRow = fieldRows.get(field);
            if ((fieldRow == null) || (columnIndex >= supplierHeader.getChildren().size())) {
                return;
            }

            boolean cellAlreadyExists = optionsGrid.getChildrenUnmodifiable().stream()
                                                   .filter(HBox.class::isInstance)
                                                   .anyMatch(node -> Objects.equals(GridPane.getColumnIndex(node), columnIndex)
                                                           && Objects.equals(GridPane.getRowIndex(node), fieldRow.getRowIndex()));
            if (!cellAlreadyExists) {
                optionsGrid.add(createCell("", field, columnIndex, fieldRow), columnIndex, fieldRow.getRowIndex());
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
}
