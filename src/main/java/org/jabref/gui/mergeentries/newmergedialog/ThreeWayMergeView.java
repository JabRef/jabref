package org.jabref.gui.mergeentries.newmergedialog;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;

import org.jabref.gui.mergeentries.newmergedialog.cell.GroupsFieldNameCell;
import org.jabref.gui.mergeentries.newmergedialog.toolbar.ThreeWayMergeToolbar;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryTypeFactory;

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
    private final List<ThreeFieldValues> threeFieldValuesList = new ArrayList<>();

    private MergedGroups mergedGroupsRecord;

    public ThreeWayMergeView(BibEntry leftEntry, BibEntry rightEntry, String leftHeader, String rightHeader) {
        getStylesheets().add(ThreeWayMergeView.class.getResource("ThreeWayMergeView.css").toExternalForm());
        viewModel = new ThreeWayMergeViewModel(leftEntry, rightEntry, leftHeader, rightHeader);

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
            threeFieldValuesList.forEach(fieldRow -> fieldRow.showDiff(new ShowDiffConfig(toolbar.getDiffView(), toolbar.getDiffHighlightingMethod())));
        } else {
            threeFieldValuesList.forEach(ThreeFieldValues::hideDiff);
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
            addFieldValues(viewModel.allFields().get(fieldIndex), fieldIndex);
        }
    }

    private void addFieldName(Field field, int rowIndex) {

    }

    private Field getFieldAtIndex(int index) {
        return viewModel.allFields().get(index);
    }

    private void addFieldValues(Field field, int index) {
        String leftEntryValue;
        String rightEntryValue;
        if (field.equals(InternalField.TYPE_HEADER)) {
            leftEntryValue = viewModel.getLeftEntry().getType().getDisplayName();
            rightEntryValue = viewModel.getRightEntry().getType().getDisplayName();
        } else {
            leftEntryValue = viewModel.getLeftEntry().getField(field).orElse("");
            rightEntryValue = viewModel.getRightEntry().getField(field).orElse("");
        }

        ThreeFieldValues fieldRow = new ThreeFieldValues(field, leftEntryValue, rightEntryValue, index, mergedGroupsRecord != null);
        threeFieldValuesList.add(fieldRow);

        if (field.equals(StandardField.GROUPS)) {
            // attach listener
            GroupsFieldNameCell groupsField = (GroupsFieldNameCell) fieldRow.getFieldNameCell();
            groupsField.setOnMergeGroups(() -> {
                if (!fieldRow.hasEqualLeftAndRightValues()) {
                    removeRow(index);
                    String mergedGroups = mergeEntryGroups();
                    viewModel.getLeftEntry().setField(field, mergedGroups);
                    viewModel.getRightEntry().setField(field, mergedGroups);
                    addFieldValues(field, index);
                    System.out.println("Groups merged: " + mergedGroups);
                    mergedGroupsRecord = new MergedGroups(leftEntryValue, rightEntryValue, mergedGroups);
                } else {
                    System.out.println("Groups already have the same value");
                }
            });

            groupsField.setOnUnmergeGroups(() -> {
                if (fieldRow.hasEqualLeftAndRightValues()) {
                    if (mergedGroupsRecord != null) {
                        viewModel.getLeftEntry().setField(field, mergedGroupsRecord.leftEntryGroups());
                        viewModel.getRightEntry().setField(field, mergedGroupsRecord.rightEntryGroups());
                        removeRow(index);
                        addFieldValues(field, index);
                        mergedGroupsRecord = null;
                    }
                }
            });
        }

        fieldRow.mergedValueProperty().addListener((observable, old, mergedValue) -> {
            if (field.equals(InternalField.TYPE_HEADER)) {
                getMergedEntry().setType(EntryTypeFactory.parse(mergedValue));
            } else {
                getMergedEntry().setField(field, mergedValue);
            }
        });
        if (field.equals(InternalField.TYPE_HEADER)) {
            getMergedEntry().setType(EntryTypeFactory.parse(fieldRow.getMergedValue()));
        } else {
            getMergedEntry().setField(field, fieldRow.getMergedValue());
        }

        if (fieldRow.hasEqualLeftAndRightValues()) {
            mergeGridPane.add(fieldRow.getFieldNameCell(), 0, index, 1, 1);
            mergeGridPane.add(fieldRow.getLeftValueCell(), 1, index, 2, 1);
            mergeGridPane.add(fieldRow.getMergedValueCell(), 3, index, 1, 1);
        } else {
            mergeGridPane.addRow(index, fieldRow.getFieldNameCell(), fieldRow.getLeftValueCell(), fieldRow.getRightValueCell(), fieldRow.getMergedValueCell());
        }
    }

    public void removeRow(int index) {
        threeFieldValuesList.remove(index);
        mergeGridPane.getChildren().removeIf(node -> GridPane.getRowIndex(node) == index);
    }

    private String mergeEntryGroups() {
        // TODO: Update the merging logic
        return viewModel.getLeftEntry().getField(StandardField.GROUPS).orElse("") +
                viewModel.getRightEntry().getField(StandardField.GROUPS).orElse("");
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
        threeFieldValuesList.forEach(ThreeFieldValues::selectLeftValue);
    }

    public void selectRightEntryValues() {
        threeFieldValuesList.forEach(ThreeFieldValues::selectRightValue);
    }

    public void showDiff(ShowDiffConfig diffConfig) {
        toolbar.setDiffView(diffConfig.diffView());
        toolbar.setDiffHighlightingMethod(diffConfig.diffHighlightingMethod());
        toolbar.setShowDiff(true);
    }
}
