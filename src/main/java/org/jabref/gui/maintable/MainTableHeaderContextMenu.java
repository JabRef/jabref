package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.StackPane;

import org.jabref.gui.maintable.columns.MainTableColumn;

public class MainTableHeaderContextMenu extends ContextMenu {

    MainTable mainTable;
    MainTableColumnFactory factory;

    /**
     * Constructor for the right click menu
     *
     */
    public MainTableHeaderContextMenu(MainTable mainTable, MainTableColumnFactory factory) {
        super();
        this.mainTable = mainTable;
        this.factory = factory;
        constructItems(mainTable);
    }

    /**
     * Handles showing the menu in the cursors position when right-clicked.
     */
    public void show(boolean show) {
        // TODO: 20/10/2022 unknown bug where issue does not show unless parameter is passed through this method.
        mainTable.setOnContextMenuRequested(event -> {
            // Display the menu if header is clicked, otherwise, remove from display.
            if (!(event.getTarget() instanceof StackPane) && show) {
                this.show(mainTable, event.getScreenX(), event.getScreenY());
            } else if (this.isShowing()) {
                this.hide();
            }
            event.consume();
        });
    }

    /**
     * Constructs the items for the list and places them in the menu.
     */
    private void constructItems(MainTable mainTable) {
        // Reset the right-click menu
        this.getItems().clear();

        // Populate the menu with the commonly used fields
        for (TableColumn<BibEntryTableViewModel, ?> tableColumn:commonColumns()) {
            RadioMenuItem itemToAdd = createMenuItem(tableColumn);
            this.getItems().add(itemToAdd);
        }

        SeparatorMenuItem separator = new SeparatorMenuItem();
        this.getItems().add(separator);

        // Append to the menu the current remaining columns in the table.
        for (TableColumn<BibEntryTableViewModel, ?> column :mainTable.getColumns()
        ) {
            // Append only if the column has not already been added (a common column)
            if (!isACommonColumn((MainTableColumn) column)) {
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
        returnItem.setSelected(isInMainTable(tableColumn));

        // Set action to toggle visibility from main table when item is clicked
        returnItem.setOnAction(event -> {
            if (isInMainTable(tableColumn)) {
                removeColumn(tableColumn);
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
        if (!isInMainTable(tableColumn)) {
            mainTable.getColumns().add(tableColumn);
        }
    }

    /**
     * Removes the column from the MainTable to remove visibility.
     */
    @SuppressWarnings("rawtypes")
    private void removeColumn(MainTableColumn tableColumn) {
        mainTable.getColumns().removeIf(tableCol -> ((MainTableColumn) tableCol).getModel().equals(tableColumn.getModel()));
    }

    /**
     * Determines if column already exists in the MainTable.
     */
    private boolean isInMainTable(MainTableColumn tableColumn) {
        return isColumnInList(tableColumn, mainTable.getColumns());
    }

    /**
     * Checks if a column is one of the commonly used columns.
     */
    private boolean isACommonColumn(MainTableColumn tableColumn) {
        return isColumnInList(tableColumn, commonColumns());
    }

    /**
     * Determines if a list of TableColumns contains the searched column.
     */
    private boolean isColumnInList(MainTableColumn searchColumn, List<TableColumn<BibEntryTableViewModel, ?>> tableColumns) {
        for (TableColumn<BibEntryTableViewModel, ?> column:
        tableColumns) {
            MainTableColumnModel model = ((MainTableColumn) column).getModel();
            if (model.equals(searchColumn.getModel())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates the list of the "commonly used" columns (currently based on the default preferences).
     */
    private List<TableColumn<BibEntryTableViewModel, ?>> commonColumns() {
        // Qualifier strings
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

        // Create the Table Columns from the models using factory methods.
        List<TableColumn<BibEntryTableViewModel, ?>> commonTableColumns = new ArrayList<>();
        for (MainTableColumnModel columnModel: commonColumns
             ) {
            TableColumn<BibEntryTableViewModel, ?> tableColumn = factory.createColumn(columnModel);
            commonTableColumns.add(tableColumn);
        }
        return commonTableColumns;
    }
}
