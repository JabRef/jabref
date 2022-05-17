package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.StackPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.maintable.columns.MainTableColumn;
import org.jabref.gui.preferences.PreferencesDialogView;
import org.jabref.logic.l10n.Localization;

public class MainTableHeaderRightClickMenu extends ContextMenu {
    static MainTable mT = null;

    public void show(MainTable mainTable, LibraryTab libraryTab, DialogService dialogService) {
        System.out.println("show()");
        mT = mainTable;
        mainTable.setOnContextMenuRequested(clickEvent -> {

            // Click on the tableColumns
            if (!(clickEvent.getTarget() instanceof StackPane)) {
                System.out.println("Click on the tableColumns");
                // Create radioMenuItemList from tableColumnList
                List<RadioMenuItem> radioMenuItems = new ArrayList<>();
                mainTable.getColumns().forEach(tableColumn -> radioMenuItems.add(createRadioMenuItem(tableColumn)));

                SeparatorMenuItem line = new SeparatorMenuItem();

                // Show preferences Button
                MenuItem columnsPreferences = new MenuItem(Localization.lang("Show preferences"));
                columnsPreferences.setOnAction(event -> {

                    // Show Entry table
                    PreferencesDialogView preferencesDialogView = new PreferencesDialogView(libraryTab.frame());
                    preferencesDialogView.getPreferenceTabList().getSelectionModel().select(3);
                    dialogService.showCustomDialog(preferencesDialogView);
                });

                // Clean items and add newItems
                this.getItems().clear();

                this.getItems().addAll(radioMenuItems);
                this.getItems().addAll(line, columnsPreferences);

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
            removeColumns(tableColumn);
        });

        return radioMenuItem;
    }

    public void removeColumns(TableColumn<BibEntryTableViewModel, ?> tableColumn) {
        mT.getColumns().removeIf(tableCol -> tableCol == tableColumn);
    }
}
