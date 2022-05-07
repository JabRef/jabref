package org.jabref.gui.maintable;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.StackPane;
import org.jabref.gui.DialogService;
import org.jabref.gui.maintable.columns.MainTableColumn;
import org.jabref.gui.preferences.table.TableTab;
import org.jabref.gui.preferences.table.TableTabViewModel;
import org.jabref.preferences.PreferencesService;

import java.util.ArrayList;
import java.util.List;

public class MainTableHeaderRightClickMenu extends ContextMenu {

    public void show(MainTable mainTable) {
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

        radioMenuItem.setOnAction(event -> {

            //return the column name when we click an item from context menu
            System.out.println(((MainTableColumn) tableColumn).getModel().getName());
        });

        return radioMenuItem;
    }

    //Need help!!!
    //We know the column name but cannot pass it in the parameters of the removeColumn()
    public void removeColumns(MainTable mainTable, DialogService dialogService, PreferencesService preferencesService){
        TableTab tableTab = new TableTab();
        TableTabViewModel tableTabViewModel = new TableTabViewModel(dialogService, preferencesService);

//        tableTabViewModel.removeColumn(tableTab.getList().getSelectionModel().getSelectedItem());

    }
}
