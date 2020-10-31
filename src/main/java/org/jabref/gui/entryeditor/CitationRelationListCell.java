package org.jabref.gui.entryeditor;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.GridPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIconView;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

/**
 * Class representing a CitationRelation list cell.
 */
public class CitationRelationListCell extends ListCell<BibEntry> {

    @FXML
    Label titleLabel;
    @FXML
    Label authorLabel;
    @FXML
    Button detailsButton;
    @FXML
    Button addButton;
    @FXML
    GridPane cellPane;
    FXMLLoader fxmlLoader;
    private final BibDatabaseContext databaseContext;
    private final DialogService dialogService;

    public CitationRelationListCell(BibDatabaseContext databaseContext, DialogService dialogService) {
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
    }

    /**
     * Updates the view of a cell.
     *
     * @param item  Item update is performed on.
     * @param empty True if empty.
     */
    @Override
    protected void updateItem(BibEntry item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            if (fxmlLoader == null) {
                fxmlLoader = new FXMLLoader(getClass().getResource("CitationRelationListCell.fxml"));
                fxmlLoader.setController(this);
                try {
                    fxmlLoader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            detailsButton.setGraphic(new JabRefIconView(IconTheme.JabRefIcons.PASTE));
            addButton.setGraphic(new JabRefIconView(IconTheme.JabRefIcons.ADD));
            titleLabel.setText(item.getField(StandardField.TITLE).orElse(""));
            authorLabel.setText(item.getField(StandardField.AUTHOR).orElse(""));
            cellPane.prefWidthProperty().bind(getListView().widthProperty().subtract(25) );

            detailsButton.setOnMouseClicked(event -> {
                dialogService.showInformationDialogAndWait("Details", getItem().getField(StandardField.TITLE).orElse(""));
            });

            addButton.setOnMouseClicked(event -> {
                if (!getItem().getField(StandardField.TITLE).orElse("").equals("No articles found")) {
                    databaseContext.getDatabase().insertEntry(getItem());
                }
            });

            if (getItem().getField(StandardField.TITLE).orElse("").equals("No articles found")) {
                detailsButton.setDisable(true);
                addButton.setDisable(true);
            }

            setText(null);
            setGraphic(cellPane);
        }
    }
}
