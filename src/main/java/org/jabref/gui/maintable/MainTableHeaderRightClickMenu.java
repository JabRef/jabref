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
import org.jabref.gui.preferences.PreferencesDialogView;

public class MainTableHeaderRightClickMenu extends ContextMenu {

    public void show(MainTable mainTable, LibraryTab libraryTab, DialogService dialogService) {
        mainTable.setOnContextMenuRequested(event -> {
            if (!(event.getTarget() instanceof StackPane)) {
                updateContextMenu(mainTable, libraryTab, dialogService);
                this.show(mainTable, event.getScreenX(), event.getScreenY());
            }
            event.consume();
        });
    }

    private void updateContextMenu(MainTable mainTable, LibraryTab libraryTab, DialogService dialogService) {
        List<RadioMenuItem> radioMenuItems = new ArrayList<>();
        mainTable.getColumns().forEach(tableColumn -> radioMenuItems.add(createRadioMenuItem(tableColumn)));

        SeparatorMenuItem line = new SeparatorMenuItem();
        MenuItem columnsPreferences = new MenuItem("Columns preferences");
        columnsPreferences.setOnAction(event -> {
            PreferencesDialogView preferencesDialogView = new PreferencesDialogView(libraryTab.frame());
            preferencesDialogView.getPreferenceTabList().getSelectionModel().select(3);
            dialogService.showCustomDialog(preferencesDialogView);
        });

        this.getItems().clear();
        this.getItems().addAll(radioMenuItems);
        this.getItems().addAll(line, columnsPreferences);
    }

    private RadioMenuItem createRadioMenuItem(TableColumn<BibEntryTableViewModel, ?> tableColumn) {
        RadioMenuItem radioMenuItem = new RadioMenuItem(tableColumn.getText());
        return radioMenuItem;
    }
}
