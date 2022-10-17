package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.StackPane;

import org.jabref.gui.maintable.columns.MainTableColumn;

public class MainTableHeaderContextMenu extends ContextMenu {

    public MainTableHeaderContextMenu() {
        super();
    }

    /**
     * Handles showing the menu in the cursors position when right-clicked.
     */
    public void show(MainTable mainTable) {
        mainTable.setOnContextMenuRequested(event -> {
            if (!(event.getTarget() instanceof StackPane)) {
                // Main table columns
                constructItems(mainTable);
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
        // Populate the menu with the current columns in the table.
        for (TableColumn<BibEntryTableViewModel, ?> column :mainTable.getColumns()
        ) {
            RadioMenuItem itemToAdd = createMenuItem(column);
            this.getItems().add(itemToAdd);
        }
    }

    /**
     * Creates an item for the menu constructed with the name/visibility of the table column.
     *
     */
    @SuppressWarnings("rawtypes")
    private RadioMenuItem createMenuItem(TableColumn<BibEntryTableViewModel, ?> column) {
        // Construct initial menuItem and flag if item is visible in main table
        String itemName = ((MainTableColumn<?>) column).getDisplayName();
        MainTableColumn tableColumn = (MainTableColumn) column;

        RadioMenuItem returnItem = new RadioMenuItem(itemName);

        returnItem.setSelected(tableColumn.isVisible());

        // Set action to toggle visibility from main table when item is clicked
        returnItem.setOnAction(event -> tableColumn.setVisible(!tableColumn.isVisible()));

        return returnItem;
    }

    private List<RadioMenuItem> commonColumns() {
        // TODO: 17/10/2022 obtain list of common columns
        List<RadioMenuItem> items = new ArrayList<>();
        String entryTypeLabel = "Entrytype";
        String authorEditorLabel = "Author/Editor";
        String titleLabel = "Title";
        String yearLabel = "Year";
        String journalLabel = "Journal/Booktitle";

        // add special items

        RadioMenuItem entryType = new RadioMenuItem(entryTypeLabel);
        RadioMenuItem author = new RadioMenuItem(authorEditorLabel);
        RadioMenuItem title = new RadioMenuItem(titleLabel);
        RadioMenuItem year = new RadioMenuItem(yearLabel);
        RadioMenuItem journal = new RadioMenuItem(journalLabel);
        items.add(entryType);
        items.add(author);
        items.add(title);
        items.add(year);
        items.add(journal);

        return items;
    }
}
