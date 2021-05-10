package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.maintable.columns.MainTableColumn;
import org.jabref.gui.preferences.PreferencesDialogView;
import org.jabref.logic.l10n.Localization;

public class MainTableHeaderRightClickMenu extends ContextMenu {

    public void show(MainTable mainTable, LibraryTab libraryTab, DialogService dialogService) {
        mainTable.setOnContextMenuRequested(clickEvent -> {

            // Click on the tableColumns
            if (!(clickEvent.getTarget() instanceof StackPane)) {

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

                this.getItems().clear();
                this.getItems().addAll(radioMenuItems);
                this.getItems().addAll(line, columnsPreferences);

                // Show ContextMenu
                this.show(mainTable, clickEvent.getScreenX(), clickEvent.getScreenY());
            }
            clickEvent.consume();
        });

        mainTable.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.SECONDARY && !event.isControlDown()) {
                this.hide();
            }
        });
    }

    private RadioMenuItem createRadioMenuItem(TableColumn<BibEntryTableViewModel, ?> tableColumn) {

        // Get DisplayName
        RadioMenuItem radioMenuItem = new RadioMenuItem(((MainTableColumn<?>) tableColumn).getDisplayName());

        // Get VisibleStatus
        radioMenuItem.setSelected(((MainTableColumn) tableColumn).getModel().getVisibleStatus());
        radioMenuItem.setOnAction(event -> {

            // Store VisibleStatus and setVisible
            ((MainTableColumn) tableColumn).getModel().setVisibleStatus(!((MainTableColumn) tableColumn).getModel().getVisibleStatus());
            tableColumn.setVisible(((MainTableColumn) tableColumn).getModel().getVisibleStatus());
        });
        return radioMenuItem;
    }
}
