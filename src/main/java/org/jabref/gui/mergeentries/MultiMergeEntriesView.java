package org.jabref.gui.mergeentries;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;

import com.airhacks.afterburner.views.ViewLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiMergeEntriesView extends BaseDialog<BibEntry> {

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

    private final MultiMergeEntriesViewModel viewModel;
    private final FieldContentFormatterPreferences fieldContentFormatterPreferences;
    private final ImportFormatPreferences importFormatPreferences;
    private final TaskExecutor taskExecutor;

    public MultiMergeEntriesView(FieldContentFormatterPreferences fieldContentFormatterPreferences, ImportFormatPreferences importFormatPreferences, TaskExecutor taskExecutor) {
        viewModel = new MultiMergeEntriesViewModel();

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        this.fieldContentFormatterPreferences = fieldContentFormatterPreferences;
        this.importFormatPreferences = importFormatPreferences;
        this.taskExecutor = taskExecutor;

        ButtonType mergeEntries = new ButtonType(Localization.lang("Merge entries"), ButtonBar.ButtonData.OK_DONE);
        this.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, mergeEntries);
        this.setResultConverter(buttonType -> {
            if (buttonType.equals(mergeEntries)) {
                return viewModel.getMergedEntry();
            } else {
                return null;
            }
        });
    }

    @FXML
    public void initialize() {
        topScrollPane.hvalueProperty().bindBidirectional(centerScrollPane.hvalueProperty());
        leftScrollPane.vvalueProperty().bindBidirectional(centerScrollPane.vvalueProperty());
        rightScrollPane.vvalueProperty().bindBidirectional(centerScrollPane.vvalueProperty());
    }

    /**
     * Add a new source of bib-data to the view
     * @param title the column header for this source of data
     * @param entry the new data
     */
    public void addSource(String title, BibEntry entry) {
        Button sourceButton = new Button(title);
        int column = addColumn(sourceButton);
        setupSourceButtonAction(sourceButton, column);
        writeBibEntryToColumn(entry, column);
    }

    /**
     * Add a new supplier of bib-data to the view
     * @param title the column header for this source of data
     * @param entrySupplier the supplier that will gather data
     */
    public void addSource(String title, Supplier<Optional<BibEntry>> entrySupplier) {
        LoadingSupplierHeader sourceButton = new LoadingSupplierHeader(title);
        int column = addColumn(sourceButton);
        setupSourceButtonAction(sourceButton, column);

        BackgroundTask.wrap(() -> {
            Optional<BibEntry> entry = entrySupplier.get();
            DefaultTaskExecutor.runInJavaFXThread(() -> {
                if (entry.isPresent()) {
                    sourceButton.setDisable(false);
                    writeBibEntryToColumn(entry.get(), column);
                }
                sourceButton.done();
            });
        }).executeWith(taskExecutor);
    }

    /**
     * Adds a column to the view. The column should represent a new source of data for the BibEntry.
     * The width of this column will be bound to the width of its header.
     * @param header the header of the column
     * @return the index of the new column
     */
    private int addColumn(Region header) {
        int columnIndex = supplierHeader.getChildren().size();

        // add header
        HBox.setHgrow(header, Priority.ALWAYS);
        supplierHeader.getChildren().add(header);
        header.setMinWidth(250);

        // setup column constraints
        ColumnConstraints constraint = new ColumnConstraints();
        constraint.setMinWidth(Control.USE_PREF_SIZE);
        constraint.setMaxWidth(Control.USE_PREF_SIZE);
        constraint.prefWidthProperty().bind(header.widthProperty());
        optionsGrid.getColumnConstraints().add(constraint);
        return columnIndex;
    }

    /**
     * Adds ToggleButtons for all fields that are set for this BibEntry
     * @param bibEntry the entry to write
     * @param column the index of the column to write this entry to
     */
    private void writeBibEntryToColumn(BibEntry bibEntry, int column) {
        for (Map.Entry<Field, String> fieldEntry : bibEntry.getFieldMap().entrySet()) {
            // make sure there is a row for the field
            if (!viewModel.getFieldRows().containsKey(fieldEntry.getKey())) {
                addField(fieldEntry.getKey());
            }

            // write the field
            Cell cell = new Cell(fieldEntry.getValue(), fieldEntry.getKey(), column);
            optionsGrid.add(cell, column, viewModel.getFieldRows().get(fieldEntry.getKey()).rowIndex);
        }
    }

    /**
     * Set up the button that displays the name of the source so that if it is clicked, all toggles in that column are
     * selected.
     * @param sourceButton the header button to setup
     * @param column the column this button is heading
     */
    private void setupSourceButtonAction(Button sourceButton, int column) {
        sourceButton.setOnAction(event -> optionsGrid.getChildrenUnmodifiable().stream()
                                                     .filter(node -> GridPane.getColumnIndex(node) == column)
                                                     .filter(node -> node instanceof HBox)
                                                     .forEach(hbox -> ((HBox) hbox).getChildrenUnmodifiable().stream()
                                                                                   .filter(node -> node instanceof ToggleButton)
                                                                                   .forEach(toggleButton -> ((ToggleButton) toggleButton).setSelected(true))));
    }

    /**
     * Adds a row that represents this field
     * @param field the field to add to the view as a new row in the table
     */
    private void addField(Field field) {

        // setup field editor column entry
        boolean isMultiLine = isMultilineField(field);
        TextInputControl fieldEditorCell = null;
        if (isMultiLine) {
            fieldEditorCell = new TextArea();
            ((TextArea) fieldEditorCell).setWrapText(true);
        } else {
            fieldEditorCell = new TextField();
        }
        fieldEditorCell.setPadding(new Insets(15, 0, 15, 0));
        VBox.setVgrow(fieldEditorCell, Priority.ALWAYS);
        MultiMergeEntriesViewModel.FieldRow newRow = new MultiMergeEntriesViewModel.FieldRow(fieldHeader.getChildren().size(), field);
        viewModel.getFieldRows().put(field, newRow);
        newRow.entryEditorTextProperty().bindBidirectional(fieldEditorCell.textProperty());
        fieldEditorCell.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(fieldEditorCell, Priority.ALWAYS);
        fieldEditor.getChildren().add(fieldEditorCell);

        ChangeListener unselectToggleOnManualEdit = (observable, oldValue, newValue) -> newRow.toggleGroup.selectToggle(null);

        newRow.toggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            newRow.entryEditorTextProperty().removeListener(unselectToggleOnManualEdit);
            if (newValue != null && newValue.isSelected()) {
                DiffHighlightingEllipsingTextFlow toggleText = ((DiffHighlightingEllipsingTextFlow) ((ToggleButton) newValue).getGraphic());
                newRow.entryEditorTextProperty().set(toggleText.getFullText());
            } else {
                newRow.entryEditorTextProperty().set("");
            }
            newRow.entryEditorTextProperty().addListener(unselectToggleOnManualEdit);
        });

        newRow.entryEditorTextProperty().addListener(unselectToggleOnManualEdit);

        // setup header label
        Label fieldHeaderLabel = new Label(field.getDisplayName());
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

    /**
     * Checks if the Field can be multiline
     * @param field the field to be checked
     * @return true if the field may be multiline, false otherwise
     */
    private boolean isMultilineField(Field field) {
        if (field.equals(StandardField.DOI)) {
            return false;
        }
        return FieldFactory.isMultiLineField(field, fieldContentFormatterPreferences.getNonWrappableFields());
    }

    private class LoadingSupplierHeader extends Button {

        private ProgressIndicator loadingIndicator;

        public LoadingSupplierHeader(String title) {
            super(title);
            loadingIndicator = new ProgressIndicator(-1);
            loadingIndicator.setPrefHeight(20);
            loadingIndicator.setMinHeight(Control.USE_PREF_SIZE);
            loadingIndicator.setMaxHeight(Control.USE_PREF_SIZE);
            setGraphic(loadingIndicator);
            setDisable(true);
        }

        public void done() {
            setGraphic(null);
        }
    }

    private class Cell extends HBox {

        private final String content;

        public Cell(String content, Field field, int columnIndex) {
            this.content = content;

            MultiMergeEntriesViewModel.FieldRow row = viewModel.getFieldRows().get(field);

            prefWidthProperty().bind(((Region) supplierHeader.getChildren().get(columnIndex)).widthProperty());
            setMinWidth(Control.USE_PREF_SIZE);
            setMaxWidth(Control.USE_PREF_SIZE);

            // Button
            ToggleButton cellButton = new ToggleButton();
            cellButton.prefHeightProperty().bind(heightProperty());
            cellButton.setMinHeight(Control.USE_PREF_SIZE);
            cellButton.setMaxHeight(Control.USE_PREF_SIZE);
            getChildren().add(cellButton);
            HBox.setHgrow(cellButton, Priority.ALWAYS);

            // Text
            DiffHighlightingEllipsingTextFlow buttonText = new DiffHighlightingEllipsingTextFlow(content);
            buttonText.getChildren().addAll(DiffHighlighting.generateDiffHighlighting(row.getEntryEditorText(), content, " "));
            buttonText.maxWidthProperty().bind(cellButton.widthProperty());
            buttonText.maxHeightProperty().bind(cellButton.heightProperty());
            cellButton.setGraphic(buttonText);

            // Tooltip
            Tooltip buttonTooltip = new Tooltip(content);
            buttonTooltip.setWrapText(true);
            buttonTooltip.prefWidthProperty().bind(widthProperty());
            buttonTooltip.setTextAlignment(TextAlignment.LEFT);
            cellButton.setTooltip(buttonTooltip);

            cellButton.setToggleGroup(row.toggleGroup);
            if (row.toggleGroup.getSelectedToggle() == null) {
                cellButton.setSelected(true);
            }

            if (field.equals(StandardField.DOI)) {
                Button doiButton = IconTheme.JabRefIcons.LOOKUP_IDENTIFIER.asButton();
                HBox.setHgrow(doiButton, Priority.NEVER);
                getChildren().add(doiButton);

                doiButton.setOnAction(event -> {
                    DoiFetcher doiFetcher = new DoiFetcher(importFormatPreferences);
                    doiButton.setDisable(true);
                    addSource(Localization.lang("From DOI"), () -> {
                        try {
                            return doiFetcher.performSearchById(content);
                        } catch (FetcherException e) {
                            LOGGER.warn("Failed to fetch BibEntry for DOI {}", content, e);
                            return Optional.empty();
                        }
                    });
                });
            }
        }

        public String getContent() {
            return content;
        }

    }
}
