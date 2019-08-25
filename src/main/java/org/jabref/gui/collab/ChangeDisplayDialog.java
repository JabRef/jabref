package org.jabref.gui.collab;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.BaseDialog;
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
        tree.setPrefWidth(190);
        EasyBind.subscribe(tree.getSelectionModel().selectedItemProperty(), this::selectedChangeChanged);

        SplitPane pane = new SplitPane();
        pane.setDividerPositions(0.25);
        pane.getItems().addAll(new ScrollPane(tree), infoPanel);
        getDialogPane().setContent(pane);

        infoPanel.setBottom(cb);
        Label rootInfo = new Label(Localization.lang("Select the tree nodes to view and accept or reject changes") + '.');
        infoPanel.setCenter(rootInfo);

        getDialogPane().getButtonTypes().setAll(
                new ButtonType(Localization.lang("Accept changes"), ButtonBar.ButtonData.APPLY),
                ButtonType.CANCEL
        );

        setResultConverter(button -> {
            if (button == ButtonType.CANCEL) {
                return false;
            } else {
                // Perform all accepted changes
                NamedCompound ce = new NamedCompound(Localization.lang("Merged external changes"));
                for (DatabaseChangeViewModel change : changes) {
                    if (change.isAccepted()) {
                        change.makeChange(database, ce);
                    }
                }
                ce.end();
                //TODO: panel.getUndoManager().addEdit(ce);

                return true;
            }
        });

        EasyBind.subscribe(cb.selectedProperty(), selected -> {
            if (selected != null && tree.getSelectionModel().getSelectedItem() != null) {
                tree.getSelectionModel().getSelectedItem().setAccepted(selected);
            }
        });
    }

    private void selectedChangeChanged(DatabaseChangeViewModel currentChange) {
        if (currentChange != null) {
            infoPanel.setCenter(currentChange.description());
            cb.setSelected(currentChange.isAccepted());
        }
    }
}
