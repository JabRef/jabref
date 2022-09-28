package org.jabref.gui.mergeentries.newmergedialog;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;

import org.jabref.gui.Globals;
import org.jabref.gui.mergeentries.newmergedialog.fieldsmerger.FieldMergerFactory;
import org.jabref.gui.mergeentries.newmergedialog.toolbar.ThreeWayMergeToolbar;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;

public class ThreeWayMergeView extends VBox {
    public static final int GRID_COLUMN_MIN_WIDTH = 250;

    public static final String LEFT_DEFAULT_HEADER = Localization.lang("Left Entry");
    public static final String RIGHT_DEFAULT_HEADER = Localization.lang("Right Entry");

    private final ColumnConstraints fieldNameColumnConstraints = new ColumnConstraints(150);
    private final ColumnConstraints leftEntryColumnConstraints = new ColumnConstraints(GRID_COLUMN_MIN_WIDTH, 256, Double.MAX_VALUE);
    private final ColumnConstraints rightEntryColumnConstraints = new ColumnConstraints(GRID_COLUMN_MIN_WIDTH, 256, Double.MAX_VALUE);
    private final ColumnConstraints mergedEntryColumnConstraints = new ColumnConstraints(GRID_COLUMN_MIN_WIDTH, 256, Double.MAX_VALUE);
    private final ThreeWayMergeToolbar toolbar;
    private final ThreeWayMergeHeaderView headerView;
    private final ScrollPane scrollPane;
    private final GridPane mergeGridPane;

    private final ThreeWayMergeViewModel viewModel;
    private final List<FieldRowView> fieldRows = new ArrayList<>();

    private final FieldMergerFactory fieldMergerFactory;

    public ThreeWayMergeView(BibEntry leftEntry, BibEntry rightEntry, String leftHeader, String rightHeader) {
        getStylesheets().add(ThreeWayMergeView.class.getResource("ThreeWayMergeView.css").toExternalForm());
        viewModel = new ThreeWayMergeViewModel((BibEntry) leftEntry.clone(), (BibEntry) rightEntry.clone(), leftHeader, rightHeader);
        // TODO: Inject 'preferenceService' into the constructor
        this.fieldMergerFactory = new FieldMergerFactory(Globals.prefs);

        mergeGridPane = new GridPane();
        scrollPane = new ScrollPane();
        headerView = new ThreeWayMergeHeaderView(leftHeader, rightHeader);
        toolbar = new ThreeWayMergeToolbar();

        initializeColumnConstraints();
        initializeMergeGridPane();
        initializeScrollPane();
        initializeHeaderView();
        initializeToolbar();

        this.setPrefHeight(Screen.getPrimary().getBounds().getHeight() * 0.76);
        this.setPrefWidth(Screen.getPrimary().getBounds().getWidth() * 0.97);

        getChildren().addAll(toolbar, headerView, scrollPane);
    }

    public ThreeWayMergeView(BibEntry leftEntry, BibEntry rightEntry) {
        this(leftEntry, rightEntry, LEFT_DEFAULT_HEADER, RIGHT_DEFAULT_HEADER);
    }

    private void initializeToolbar() {
        toolbar.setOnSelectLeftEntryValuesButtonClicked(this::selectLeftEntryValues);
        toolbar.setOnSelectRightEntryValuesButtonClicked(this::selectRightEntryValues);

        toolbar.showDiffProperty().addListener(e -> updateDiff());
        toolbar.diffViewProperty().addListener(e -> updateDiff());
        toolbar.diffHighlightingMethodProperty().addListener(e -> updateDiff());
    }

    private void updateDiff() {
        if (toolbar.isShowDiffEnabled()) {
            fieldRows.forEach(row -> row.showDiff(new ShowDiffConfig(toolbar.getDiffView(), toolbar.getDiffHighlightingMethod())));
        } else {
            fieldRows.forEach(FieldRowView::hideDiff);
        }
    }

    private void initializeHeaderView() {
        headerView.getColumnConstraints().addAll(fieldNameColumnConstraints,
                                                 leftEntryColumnConstraints,
                                                 rightEntryColumnConstraints,
                                                 mergedEntryColumnConstraints);
    }

    private void initializeScrollPane() {
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setContent(mergeGridPane);
    }

    private void initializeColumnConstraints() {
        fieldNameColumnConstraints.setHgrow(Priority.NEVER);
        leftEntryColumnConstraints.setHgrow(Priority.ALWAYS);
        rightEntryColumnConstraints.setHgrow(Priority.ALWAYS);
        mergedEntryColumnConstraints.setHgrow(Priority.ALWAYS);
    }

    private void initializeMergeGridPane() {
        mergeGridPane.getColumnConstraints().addAll(fieldNameColumnConstraints, leftEntryColumnConstraints, rightEntryColumnConstraints, mergedEntryColumnConstraints);

        for (int fieldIndex = 0; fieldIndex < viewModel.numberOfVisibleFields(); fieldIndex++) {
            addRow(fieldIndex);

            mergeGridPane.getRowConstraints().add(new RowConstraints());
        }
    }

    private Field getFieldAtIndex(int index) {
        return viewModel.getVisibleFields().get(index);
    }

    private void addRow(int fieldIndex) {
        Field field = getFieldAtIndex(fieldIndex);

        FieldRowView fieldRow;
        if (field.getProperties().contains(FieldProperty.PERSON_NAMES)) {
            fieldRow = new PersonsNameFieldRowView(field, getLeftEntry(), getRightEntry(), getMergedEntry(), fieldMergerFactory, fieldIndex);
        } else {
            fieldRow = new FieldRowView(field, getLeftEntry(), getRightEntry(), getMergedEntry(), fieldMergerFactory, fieldIndex);
        }

        fieldRows.add(fieldIndex, fieldRow);

        mergeGridPane.add(fieldRow.getFieldNameCell(), 0, fieldIndex);
        mergeGridPane.add(fieldRow.getLeftValueCell(), 1, fieldIndex);
        mergeGridPane.add(fieldRow.getRightValueCell(), 2, fieldIndex);
        mergeGridPane.add(fieldRow.getMergedValueCell(), 3, fieldIndex);
    }

    public BibEntry getMergedEntry() {
        return viewModel.getMergedEntry();
    }

    public void setLeftHeader(String leftHeader) {
        headerView.setLeftHeader(leftHeader);
    }

    public void setRightHeader(String rightHeader) {
        headerView.setRightHeader(rightHeader);
    }

    public void selectLeftEntryValues() {
        fieldRows.forEach(FieldRowView::selectLeftValue);
    }

    public void selectRightEntryValues() {
        fieldRows.forEach(FieldRowView::selectRightValue);
    }

    public void showDiff(ShowDiffConfig diffConfig) {
        toolbar.setDiffView(diffConfig.diffView());
        toolbar.setDiffHighlightingMethod(diffConfig.diffHighlightingMethod());
        toolbar.setShowDiff(true);
    }

    public BibEntry getLeftEntry() {
        return viewModel.getLeftEntry();
    }

    public BibEntry getRightEntry() {
        return viewModel.getRightEntry();
    }
}
