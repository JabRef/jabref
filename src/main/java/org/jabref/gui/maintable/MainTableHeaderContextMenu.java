package org.jabref.gui.maintable;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.layout.StackPane;

public class MainTableHeaderContextMenu extends ContextMenu {

    //MainTable mainTable;


    public MainTableHeaderContextMenu(){
        super();
        //this.mainTable = mainTable;
        //Obtain the most commonly used fields and the current fields in header.
        // place in list - need method of updating.
        constructItems();

    }

    private void constructItems() {
        String entryTypeLabel = "Entrytype";
        String authorEditorLabel = "Author/Editor";
        String titleLabel = "Title";
        String yearLabel = "Year";
        String journalLabel = "Journal/Booktitle";

//        RadioMenuItem groupColor = new RadioMenuItem("Group color");
//        RadioMenuItem linkedFile = new RadioMenuItem("Linked file");
//        RadioMenuItem linkedId = new RadioMenuItem("Linked id");

        RadioMenuItem entryType = new RadioMenuItem(entryTypeLabel);
        RadioMenuItem author = new RadioMenuItem(authorEditorLabel);
        RadioMenuItem title = new RadioMenuItem(titleLabel);
        RadioMenuItem year = new RadioMenuItem(yearLabel);
        RadioMenuItem journal = new RadioMenuItem(journalLabel);
        MenuItem randomButton = new MenuItem("Click me");
        randomButton.setOnAction(event -> {
            //new columnDialog
            RadioMenuItem addTestItem = new RadioMenuItem("test");
            this.getItems().add(addTestItem);
        });

        this.getItems().addAll(entryType, author, title, year, journal, randomButton);
    }

    //Handles showing the menu in the cursors position when right clicked.
    public void show(MainTable mainTable) {
        mainTable.setOnContextMenuRequested(event -> {
            if (!(event.getTarget() instanceof StackPane)) {
                this.show(mainTable, event.getScreenX(), event.getScreenY());
            }
            event.consume();
        });
    }
}
