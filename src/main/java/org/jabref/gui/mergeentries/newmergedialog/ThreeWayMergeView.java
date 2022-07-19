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
import org.jabref.model.entry.field.StandardField;

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
    private final List<ThreeFieldValuesView> fieldValuesList = new ArrayList<>();

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
            fieldValuesList.forEach(fieldValues -> fieldValues.showDiff(new ShowDiffConfig(toolbar.getDiffView(), toolbar.getDiffHighlightingMethod())));
        } else {
            fieldValuesList.forEach(ThreeFieldValuesView::hideDiff);
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

        for (int fieldIndex = 0; fieldIndex < viewModel.allFieldsSize(); fieldIndex++) {
            // addFieldName(getFieldAtIndex(fieldIndex), fieldIndex);
            addFieldValues(fieldIndex);

            // Removing this will cause UI to lag when updating field values
            if (getFieldAtIndex(fieldIndex).equals(StandardField.GROUPS)) {
                mergeGridPane.getRowConstraints().add(fieldIndex, new RowConstraints(56, 56, Double.MAX_VALUE));
            } else {
                mergeGridPane.getRowConstraints().add(new RowConstraints());
            }
        }
    }

    private Field getFieldAtIndex(int index) {
        return viewModel.allFields().get(index);
    }

    private void addFieldValues(int fieldIndex) {
        Field field = getFieldAtIndex(fieldIndex);

        ThreeFieldValuesView fieldValues = new ThreeFieldValuesView(field, getLeftEntry(), getRightEntry(), getMergedEntry(), fieldMergerFactory, fieldIndex);

        fieldValuesList.add(fieldIndex, fieldValues);

        mergeGridPane.add(fieldValues.getFieldNameCell(), 0, fieldIndex);
        mergeGridPane.add(fieldValues.getLeftValueCell(), 1, fieldIndex);
        mergeGridPane.add(fieldValues.getRightValueCell(), 2, fieldIndex);
        mergeGridPane.add(fieldValues.getMergedValueCell(), 3, fieldIndex);
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
        fieldValuesList.forEach(ThreeFieldValuesView::selectLeftValue);
    }

    public void selectRightEntryValues() {
        fieldValuesList.forEach(ThreeFieldValuesView::selectRightValue);
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
