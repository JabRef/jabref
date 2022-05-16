package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.StackPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.maintable.columns.MainTableColumn;
import org.jabref.gui.preferences.table.TableTab;
import org.jabref.gui.preferences.table.TableTabViewModel;
import org.jabref.preferences.PreferencesService;

public class MainTableHeaderRightClickMenu extends ContextMenu {
    static MainTable mT = null;

    public void show(MainTable mainTable) {
        System.out.println("show()");
        mT = mainTable;
        mainTable.setOnContextMenuRequested(clickEvent -> {

            // Click on the tableColumns
            if (!(clickEvent.getTarget() instanceof StackPane)) {
                System.out.println("Click on the tableColumns");
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

    private RadioMenuItem createRadioMenuItem(TableColumn<BibEntryTableViewModel, ?> tableColumn) {
        RadioMenuItem radioMenuItem = new RadioMenuItem(((MainTableColumn<?>) tableColumn).getDisplayName());
        // System.out.println(((MainTableColumn<?>) tableColumn).getDisplayName());
        radioMenuItem.setOnAction(event -> {
            // Return the column name when we click an item from context menu
            // System.out.println(((MainTableColumn) tableColumn).getModel().getName());
            update(tableColumn);
        });

        return radioMenuItem;
    }

    private void update(TableColumn<BibEntryTableViewModel, ?> tableColumn) {
        System.out.println("update()");
        mT.setOnContextMenuRequested(clickEvent -> {

            // Click on the tableColumns
            if (!(clickEvent.getTarget() instanceof StackPane)) {
                System.out.println("Click on the tableColumns");
                // Create radioMenuItemList from tableColumnList
                List<RadioMenuItem> radioMenuItems = new ArrayList<>();

                // delete
                mT.getColumns().removeIf(tableCol -> tableCol == tableColumn);

                mT.getColumns().forEach(tC-> radioMenuItems.add(createRadioMenuItem(tC)));

                // Clean items and add newItems
                this.getItems().clear();

                this.getItems().addAll(radioMenuItems);

                // Show ContextMenu
                this.show(mT, clickEvent.getScreenX(), clickEvent.getScreenY());
            }
            clickEvent.consume();
        });
    }

    // Need help!!!
    // We know the column name but cannot pass it in the parameters of the removeColumn()
    public void removeColumns(DialogService dialogService, PreferencesService preferencesService) {
        TableTab tableTab = new TableTab();
        TableTabViewModel tableTabViewModel = new TableTabViewModel(dialogService, preferencesService);

//        tableTabViewModel.removeColumn(tableTab.getList().getSelectionModel().getSelectedItem());
    }
}
