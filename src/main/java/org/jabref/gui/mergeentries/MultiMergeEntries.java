package org.jabref.gui.mergeentries;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BackgroundTask;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiMergeEntries extends SplitPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiMergeEntries.class);

    private final FieldContentFormatterPreferences fieldContentFormatterPreferences;
    private final ImportFormatPreferences importFormatPreferences;
    private final TaskExecutor taskExecutor;
    private HashMap<Field, FieldRow> fieldRows = new HashMap<>();

    private VBox labelColumn;
    private VBox optionsColumn;
    private VBox entryColumn;

    private ScrollPane topScrollPane;
    private ScrollPane leftScrollPane;
    private ScrollPane centerScrollPane;
    private ScrollPane rightScrollPane;

    private HBox supplierHeader;
    private VBox fieldHeader;
    private GridPane optionsGrid;
    private VBox fieldEditor;

    public MultiMergeEntries(FieldContentFormatterPreferences fieldContentFormatterPreferences, ImportFormatPreferences importFormatPreferences, TaskExecutor taskExecutor) {
        this.fieldContentFormatterPreferences = fieldContentFormatterPreferences;
        this.importFormatPreferences = importFormatPreferences;
        this.taskExecutor = taskExecutor;
        init();
    }

    private void init() {

        labelColumn = new VBox();
        optionsColumn = new VBox();
        entryColumn = new VBox();

        supplierHeader = new HBox();
        fieldHeader = new VBox();
        optionsGrid = new GridPane();
        fieldEditor = new VBox();

        topScrollPane = new ScrollPane(supplierHeader);
        leftScrollPane = new ScrollPane(fieldHeader);
        centerScrollPane = new ScrollPane(optionsGrid);
        rightScrollPane = new ScrollPane(fieldEditor);
        topScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        topScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        centerScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        topScrollPane.hvalueProperty().bindBidirectional(centerScrollPane.hvalueProperty());
        leftScrollPane.vvalueProperty().bindBidirectional(centerScrollPane.vvalueProperty());
        rightScrollPane.vvalueProperty().bindBidirectional(centerScrollPane.vvalueProperty());

        Label supplierHeaderLabel = new Label(Localization.lang("Supplier"));
        Label entryHeaderLabel = new Label(Localization.lang("Entry"));
        supplierHeaderLabel.prefHeightProperty().bind(supplierHeader.heightProperty());
        supplierHeaderLabel.setMinHeight(Control.USE_PREF_SIZE);
        supplierHeaderLabel.setMaxHeight(Control.USE_PREF_SIZE);
        entryHeaderLabel.prefHeightProperty().bind(supplierHeader.heightProperty());
        entryHeaderLabel.setMinHeight(Control.USE_PREF_SIZE);
        entryHeaderLabel.setMaxHeight(Control.USE_PREF_SIZE);

        labelColumn.getChildren().addAll(supplierHeaderLabel, leftScrollPane);
        optionsColumn.getChildren().addAll(topScrollPane, centerScrollPane);
        entryColumn.getChildren().addAll(entryHeaderLabel, rightScrollPane);

        setResizableWithParent(labelColumn, false);
        setDividerPositions(0.1, 0.6, 0.3);
        getItems().addAll(labelColumn, optionsColumn, entryColumn);
    }

    public void addEntry(String title, BibEntry entry) {
        Button sourceButton = new Button(title);
        int column = addColumn(sourceButton);
        setupSourceButtonAction(sourceButton, column);
        writeBibEntryToColumn(entry, column);
    }

    public void addEntry(String title, Supplier<Optional<BibEntry>> entrySupplier) {
        HBox header = new HBox();
        Button sourceButton = new Button(title);
        ProgressIndicator loadingIndicator = new ProgressIndicator(-1);
        header.getChildren().addAll(sourceButton, loadingIndicator);
        HBox.setHgrow(sourceButton, Priority.ALWAYS);
        sourceButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(loadingIndicator, Priority.NEVER);
        int column = addColumn(header);
        setupSourceButtonAction(sourceButton, column);
        sourceButton.setDisable(true);
        loadingIndicator.prefHeightProperty().bind(sourceButton.heightProperty());
        loadingIndicator.setMinHeight(Control.USE_PREF_SIZE);
        loadingIndicator.setMaxHeight(Control.USE_PREF_SIZE);

        BackgroundTask.wrap(() -> {
            Optional<BibEntry> entry = entrySupplier.get();
            DefaultTaskExecutor.runInJavaFXThread(() -> {
                if (entry.isPresent()) {
                    sourceButton.setDisable(false);
                    writeBibEntryToColumn(entry.get(), column);
                }
                header.getChildren().remove(loadingIndicator);
            });
        }).executeWith(taskExecutor);
    }

    private void setupSourceButtonAction(Button sourceButton, int column) {
        sourceButton.setOnAction(event -> optionsGrid.getChildrenUnmodifiable().stream()
                                                     .filter(node -> GridPane.getColumnIndex(node) == column)
                                                     .filter(node -> node instanceof HBox)
                                                     .forEach(hbox -> ((HBox) hbox).getChildrenUnmodifiable().stream()
                                                                                   .filter(node -> node instanceof ToggleButton)
                                                                                   .forEach(toggleButton -> ((ToggleButton) toggleButton).setSelected(true))));
    }

    private void writeBibEntryToColumn(BibEntry bibEntry, int column) {
        for (Map.Entry<Field, String> fieldEntry : bibEntry.getFieldMap().entrySet()) {
            if (!fieldRows.containsKey(fieldEntry.getKey())) {
                addField(fieldEntry.getKey());
            }
            fieldRows.get(fieldEntry.getKey()).addValue(column, fieldEntry.getValue());
        }
    }

    public BibEntry getMergeEntry() {
        BibEntry mergedEntry = new BibEntry();
        for (Map.Entry<Field, FieldRow> fieldRowEntry : fieldRows.entrySet()) {
            String fieldValue = ((TextInputControl) fieldEditor.getChildren().get(fieldRowEntry.getValue().rowIndex)).getText();
            if (fieldValue != null) {
                mergedEntry.setField(fieldRowEntry.getKey(), fieldValue);
            }
        }
        return mergedEntry;
    }

    private int addColumn(Region header) {
        int columnIndex = supplierHeader.getChildren().size();
        HBox.setHgrow(header, Priority.ALWAYS);
        supplierHeader.getChildren().add(header);
        header.setMinWidth(250);

        optionsGrid.add(new Label(), columnIndex, 0);
        ColumnConstraints constraint = new ColumnConstraints();
        constraint.setMinWidth(Control.USE_PREF_SIZE);
        constraint.setMaxWidth(Control.USE_PREF_SIZE);
        constraint.prefWidthProperty().bind(header.widthProperty());
        optionsGrid.getColumnConstraints().add(constraint);

        return columnIndex;
    }

    private boolean isMultilineField(Field field) {
        if (field.equals(StandardField.DOI)) {
            return false;
        }
        return FieldFactory.isMultiLineField(field, fieldContentFormatterPreferences.getNonWrappableFields());
    }

    public void addField(Field field) {
        boolean isMultiLine = isMultilineField(field);
        TextInputControl fieldEditorCell = null;
        if (isMultiLine) {
            fieldEditorCell = new TextArea();
            ((TextArea) fieldEditorCell).setWrapText(true);
        } else {
            fieldEditorCell = new TextField();
        }
        VBox.setVgrow(fieldEditorCell, Priority.ALWAYS);
        FieldRow newRow = new FieldRow(fieldHeader.getChildren().size(), fieldEditorCell, field);
        fieldRows.put(field, newRow);
        Label fieldHeaderLabel = new Label(field.getDisplayName());
        fieldHeaderLabel.prefHeightProperty().bind(fieldEditorCell.heightProperty());
        fieldHeaderLabel.setMaxWidth(Control.USE_PREF_SIZE);
        fieldHeaderLabel.setMinWidth(Control.USE_PREF_SIZE);
        fieldEditorCell.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(fieldEditorCell, Priority.ALWAYS);
        fieldHeader.getChildren().add(fieldHeaderLabel);
        fieldEditor.getChildren().add(fieldEditorCell);

        optionsGrid.add(new Label(), 0, newRow.rowIndex);
        RowConstraints constraint = new RowConstraints();
        constraint.setMinHeight(Control.USE_PREF_SIZE);
        constraint.setMaxHeight(Control.USE_PREF_SIZE);
        constraint.prefHeightProperty().bind(fieldEditorCell.heightProperty());

        optionsGrid.getRowConstraints().add(constraint);
    }

    private class FieldRow {
        public int rowIndex;
        public ToggleGroup toggleGroup = new ToggleGroup();
        public TextInputControl entryEditorField;

        private Field field;

        public FieldRow(int rowIndex, TextInputControl entryEditorField, Field field) {
            this.rowIndex = rowIndex;
            this.entryEditorField = entryEditorField;
            this.field = field;
            entryEditorField.prefWidthProperty().bind(rightScrollPane.widthProperty());
            entryEditorField.setMinWidth(Control.USE_PREF_SIZE);
            entryEditorField.setMaxWidth(Control.USE_PREF_SIZE);

            ChangeListener textChangeListener = new ChangeListener() {
                @Override
                public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                    if (toggleGroup.getSelectedToggle() != null) {
                        toggleGroup.getSelectedToggle().setSelected(false);
                        entryEditorField.textProperty().removeListener(this);
                    }
                }
            };

            toggleGroup.selectedToggleProperty().addListener(
                    (observable, oldValue, newValue) -> {
                        if (observable != null && newValue != null && newValue.isSelected()) {
                            ToggleButton toggle = (ToggleButton) observable.getValue();
                            if (toggle.getGraphic() instanceof DiffHighlightingEllipsingTextFlow) {
                                entryEditorField.setText(((DiffHighlightingEllipsingTextFlow) toggle.getGraphic()).getFullText());
                            } else {
                                entryEditorField.setText(toggle.getText());
                            }
                            entryEditorField.textProperty().addListener(textChangeListener);
                        } else {
                            entryEditorField.textProperty().removeListener(textChangeListener);
                        }
                    }
            );
        }

        public void addValue(int columnIndex, String value) {
            boolean isMultiLine = isMultilineField(field);
            ToggleButton cellButton = new ToggleButton();
            HBox cell = new HBox();
            cell.prefWidthProperty().bind(((Region) supplierHeader.getChildren().get(columnIndex)).widthProperty());
            cell.setMinWidth(Control.USE_PREF_SIZE);
            cell.setMaxWidth(Control.USE_PREF_SIZE);
            cell.setSpacing(0);
            // cell.setBorder(new Bor);
            if (isMultiLine) {
                DiffHighlightingEllipsingTextFlow buttonText = new DiffHighlightingEllipsingTextFlow(value);
                buttonText.getChildren().add(new Text(value));
                // buttonText.maxWidthProperty().bind(((Region) supplierHeader.getChildren().get(columnIndex)).widthProperty());
                // buttonText.maxHeightProperty().bind(entryEditorField.heightProperty());
                buttonText.maxWidthProperty().bind(cellButton.widthProperty());
                buttonText.maxHeightProperty().bind(cellButton.heightProperty());
                cellButton.setGraphic(buttonText);
                Tooltip buttonTooltip = new Tooltip(value);
                buttonTooltip.setWrapText(true);
                buttonTooltip.prefWidthProperty().bind(cellButton.widthProperty());
                buttonTooltip.setTextAlignment(TextAlignment.LEFT);
                cellButton.setTooltip(buttonTooltip);
            } else {
                cellButton.setText(value);
            }
            HBox.setHgrow(cellButton, Priority.ALWAYS);
            cell.getChildren().add(cellButton);
            optionsGrid.add(cell, columnIndex, rowIndex);

            if (toggleGroup.getSelectedToggle() == null) {
                cellButton.setSelected(true);
            }
            for (Toggle otherToggle : toggleGroup.getToggles()) {
                if (otherToggle instanceof ToggleButton) {
                    ToggleButton otherToggleButton = (ToggleButton) otherToggle;
                    if (otherToggleButton.getText().equals(value)) {
                        cellButton.selectedProperty().bindBidirectional(otherToggleButton.selectedProperty());
                        return;
                    }
                }
            }
            cellButton.setToggleGroup(toggleGroup);

            entryEditorField.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    // highlight text
                }
            });

            if (field.equals(StandardField.DOI)) {
                Button doiButton = IconTheme.JabRefIcons.LOOKUP_IDENTIFIER.asButton();
                HBox.setHgrow(doiButton, Priority.NEVER);
                cell.getChildren().add(doiButton);
                doiButton.setOnAction(event -> {
                    DoiFetcher doiFetcher = new DoiFetcher(importFormatPreferences);
                    doiButton.setDisable(true);
                    addEntry(Localization.lang("From DOI"), () -> {
                        try {
                            return doiFetcher.performSearchById(value);
                        } catch (FetcherException e) {
                            LOGGER.warn("Failed to fetch BibEntry for DOI {}", value, e);
                            return Optional.empty();
                        }
                    });
                });
            }
        }
    }
}
