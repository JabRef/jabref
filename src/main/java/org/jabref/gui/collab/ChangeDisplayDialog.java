package org.jabref.gui.collab;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import org.fxmisc.easybind.EasyBind;

class ChangeDisplayDialog extends BaseDialog<Boolean> {

    private final ListView<DatabaseChangeViewModel> tree;
    private final BorderPane infoPanel = new BorderPane();
    private final CheckBox cb = new CheckBox(Localization.lang("Accept change"));

    public ChangeDisplayDialog(BibDatabaseContext database, List<DatabaseChangeViewModel> changes) {
        this.setTitle(Localization.lang("External changes"));
        this.getDialogPane().setPrefSize(800, 600);

        tree = new ListView<>(FXCollections.observableArrayList(changes));
        tree.setPrefWidth(160);
        EasyBind.subscribe(tree.getSelectionModel().selectedItemProperty(), this::selectedChangeChanged);

        SplitPane pane = new SplitPane();
        pane.setDividerPositions(0.2);
        ScrollPane scroll = new ScrollPane(tree);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(true);
        pane.getItems().addAll(scroll, infoPanel);
        pane.setResizableWithParent(scroll, false);

        getDialogPane().setContent(pane);

        Label rootInfo = new Label(Localization.lang("Select the tree nodes to view and accept or reject changes") + '.');
        infoPanel.setCenter(rootInfo);
        tree.getSelectionModel().select(0);

        ButtonType dismissChanges = new ButtonType(Localization.lang("Use all current JabRef changes"), ButtonData.CANCEL_CLOSE);
        ButtonType selectAllChangesFromJabRef = new ButtonType(Localization.lang("Select all changes from JabRef"), ButtonData.APPLY);
        ButtonType selectAllChangesFromDisk = new ButtonType(Localization.lang("Select all changes from disk"), ButtonData.APPLY);

        getDialogPane().getButtonTypes().setAll(
                                                selectAllChangesFromJabRef,
                                                new ButtonType(Localization.lang("Accept selected changes"), ButtonBar.ButtonData.APPLY),
                                                selectAllChangesFromDisk,
                                                dismissChanges);

        ControlHelper.setAction(selectAllChangesFromJabRef, getDialogPane(), evt -> {
            for (DatabaseChangeViewModel change : changes) {
                change.setAccepted(false);
            }
        });

        ControlHelper.setAction(selectAllChangesFromDisk, getDialogPane(), evt -> {
            for (DatabaseChangeViewModel change : changes) {
                change.setAccepted(true);
            }
        });

        setResultConverter(button -> {
            if (button == dismissChanges) {
                return false;

            } else {
                // Perform all accepted changes
                NamedCompound ce = new NamedCompound(Localization.lang("Merged external changes"));
                for (DatabaseChangeViewModel change : changes) {
                    if (change instanceof EntryChangeViewModel) {
                        change.makeChange(database, ce); //We don't have a checkbox for accept and always get the correct merged entry, the accept property in this special case only controls the radio buttons selection
                    } else if (change.isAccepted()) {
                        change.makeChange(database, ce);
                    }
                }
                ce.end();
                //TODO: panel.getUndoManager().addEdit(ce);

                return true;
            }
        });

    }

    private void selectedChangeChanged(DatabaseChangeViewModel currentChange) {
        if (currentChange != null) {
            infoPanel.setCenter(currentChange.description());

            if (!(currentChange instanceof EntryChangeViewModel)) {
                cb.setManaged(true);
                infoPanel.setBottom(cb);
                cb.selectedProperty().bindBidirectional(currentChange.acceptedProperty());

            } else {
                cb.setManaged(false);
            }

        }
    }
}
