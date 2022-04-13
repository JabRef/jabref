package org.jabref.gui.maintable;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.StackPane;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.maintable.columns.MainTableColumn;

import java.util.ArrayList;
import java.util.List;

public class MainTableHeaderRightClickMenu extends ContextMenu {

    public void show(MainTable mainTable, LibraryTab libraryTab, DialogService dialogService) {
        mainTable.setOnContextMenuRequested(clickEvent -> {

            // Click on the tableColumns
            if (!(clickEvent.getTarget() instanceof StackPane)) {

                // Create radioMenuItemList from tableColumnList
                List<RadioMenuItem> radioMenuItems = new ArrayList<>();
                mainTable.getColumns().forEach(tableColumn -> radioMenuItems.add(createRadioMenuItem(tableColumn)));

                // Clean items and add newItems
                this.getItems().clear();
                this.getItems().addAll(radioMenuItems);

                // Show ContextMenu
                this.show(mainTable, clickEvent.getScreenX(), clickEvent.getScreenY());
            }
            clickEvent.consume();
        });
    }

    private RadioMenuItem createRadioMenuItem(TableColumn<BibEntryTableViewModel,?> tableColumn) {
        RadioMenuItem radioMenuItem = new RadioMenuItem(((MainTableColumn<?>) tableColumn).getDisplayName());
        return radioMenuItem;
    }
}
