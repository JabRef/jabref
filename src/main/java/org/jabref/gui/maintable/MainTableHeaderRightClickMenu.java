package org.jabref.gui.maintable;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.layout.StackPane;

public class MainTableHeaderRightClickMenu extends ContextMenu {

    public MainTableHeaderRightClickMenu() {
        super();

        RadioMenuItem groupColor = new RadioMenuItem("Group color");
        RadioMenuItem linkedFile = new RadioMenuItem("Linked file");
        RadioMenuItem linkedId = new RadioMenuItem("Linked id");
        RadioMenuItem entryType = new RadioMenuItem("Entrytype");
        RadioMenuItem author = new RadioMenuItem("Author/Editor");
        MenuItem columnDialog = new MenuItem("ColumnDialog");

        columnDialog.setOnAction(event -> {
            //new columnDialog
        });

        this.getItems().addAll(groupColor, linkedFile, linkedId, entryType, author, columnDialog);
    }

    public void show(MainTable mainTable) {
        mainTable.setOnContextMenuRequested(event -> {
            if (!(event.getTarget() instanceof StackPane)) {
                this.show(mainTable, event.getScreenX(), event.getScreenY());
            }
            event.consume();
        });
    }
}
