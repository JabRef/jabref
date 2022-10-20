package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.StackPane;

import org.jabref.gui.maintable.columns.MainTableColumn;

public class MainTableHeaderContextMenu extends ContextMenu {

    MainTable mainTable;
    Set<TableColumn<BibEntryTableViewModel, ?>> mainTableColumnSet;
    MainTableColumnFactory factory;
    /**
     * Constructor for the right click menu
     *
     */
    public MainTableHeaderContextMenu(MainTable mainTable, MainTableColumnFactory factory) {
        super();
        this.mainTable = mainTable;
        this.factory = factory;
        mainTableColumnSet = new HashSet<>();
        constructItems(mainTable);
    }

    /**
     * Handles showing the menu in the cursors position when right-clicked.
     */
    public void show(boolean show) {
        // TODO: 20/10/2022 unknown bug where issue does not show unless parameter is passed through this method.
        mainTable.setOnContextMenuRequested(event -> {
            if (!(event.getTarget() instanceof StackPane) && show) {
                // Main table columns
                this.show(mainTable, event.getScreenX(), event.getScreenY());
            }
            event.consume();
        });
    }

    /**
     * Constructs the items for the list and places them in the menu.
     */
    private void constructItems(MainTable mainTable) {
        // Reset the right-click menu and
        this.getItems().clear();
        // Obtain the most commonly used fields and the current fields in header.
        // TODO: 17/10/2022 implement most commonly used fields
        mainTableColumnSet.addAll(commonColumns());

        // Populate the menu with the current columns in the table.
        for (TableColumn<BibEntryTableViewModel, ?> column :mainTable.getColumns()
        ) {
            if (!isInTable((MainTableColumn) column, mainTableColumnSet.stream().toList())) {
                this.mainTableColumnSet.add(column);
                RadioMenuItem itemToAdd = createMenuItem(column);
                this.getItems().add(itemToAdd);
            }
        }
    }

    /**
     * Creates an item for the menu constructed with the name/visibility of the table column.
     *
     */
    @SuppressWarnings("rawtypes")
    private RadioMenuItem createMenuItem(TableColumn<BibEntryTableViewModel, ?> column) {
        // Construct initial menuItem
        MainTableColumn tableColumn = (MainTableColumn) column;
        String itemName = tableColumn.getDisplayName();

        RadioMenuItem returnItem = new RadioMenuItem(itemName);

        // Flag item as selected if the item is already in the main table.
        returnItem.setSelected(isInTable(tableColumn, mainTable.getColumns()));

        // Set action to toggle visibility from main table when item is clicked
        returnItem.setOnAction(event -> {
            if (isInTable(tableColumn, mainTable.getColumns())) {
                removeColumn(tableColumn);
                System.out.println(tableColumn.getModel().getType());
                System.out.println(tableColumn.getModel().getQualifier());
            } else {
                addColumn(tableColumn);
            }
        });

        return returnItem;
    }

    /**
     * Adds the column into the MainTable for display.
     */
    @SuppressWarnings("rawtypes")
    private void addColumn(MainTableColumn tableColumn) {
        // Do not add duplicate if table column is already within the table.
        if (!isInTable(tableColumn, mainTable.getColumns())) {
            mainTable.getColumns().add(tableColumn);
            tableColumn.setVisible(true);
        }
    }

    /**
     * Removes the column from the MainTable to remove visibility.
     */
    @SuppressWarnings("rawtypes")
    private void removeColumn(MainTableColumn tableColumn) {
        tableColumn.setVisible(!tableColumn.isVisible());
        mainTable.getColumns().removeIf(tableCol -> tableCol == tableColumn);
    }

    private boolean isInTable(MainTableColumn tableColumn, List<TableColumn<BibEntryTableViewModel, ?>> tableColumns) {
        for (TableColumn<BibEntryTableViewModel, ?> column:
        tableColumns) {
            MainTableColumnModel model = ((MainTableColumn) column).getModel();
            if (tableColumn.getModel().getQualifier().equals(model.getQualifier()) || tableColumn.getModel().getType().equals(model.getType())) {
                return true;
            }
        }
        return false;
    }

    private void refreshMenu() {
        for (TableColumn<BibEntryTableViewModel, ?> column :mainTableColumnSet) {
            RadioMenuItem itemToAdd = createMenuItem(column);
            this.getItems().add(itemToAdd);
        }
    }

    private Set<TableColumn<BibEntryTableViewModel, ?>> commonColumns() {
        // TODO: 17/10/2022 obtain list of common columns

        String entryTypeQualifier = "entrytype";
        String authorEditQualifier = "author/editor";
        String titleQualifier = "title";
        String yearQualifier = "year";
        String journalBookQualifier = "journal/booktitle";
        String rankQualifier = "ranking";
        String readStatusQualifier = "readstatus";
        String priorityQualifier = "priority";

        // Create the MainTableColumn Models from qualifiers + types.
        List<MainTableColumnModel> commonColumns = new ArrayList<>();
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.GROUPS));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.FILES));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.LINKED_IDENTIFIER));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, entryTypeQualifier));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, authorEditQualifier));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, titleQualifier));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, yearQualifier));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, journalBookQualifier));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, rankQualifier));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, readStatusQualifier));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, priorityQualifier));

        // Create the Table Columns from the models.
        Set<TableColumn<BibEntryTableViewModel, ?>> commonTableColumns = new HashSet<>();
        for (MainTableColumnModel columnModel: commonColumns
             ) {
            TableColumn<BibEntryTableViewModel, ?> tableColumn = factory.createColumn(columnModel);
            commonTableColumns.add(tableColumn);
            this.getItems().add(createMenuItem(tableColumn));
        }

        return commonTableColumns;
    }
}
