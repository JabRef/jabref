package org.jabref.gui.collab;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;

public class ExternalChangesResolverDialog extends BaseDialog<Boolean> {
    @FXML
    public TableView<DatabaseChangeViewModel> changesTableView;
    @FXML
    private TableColumn<DatabaseChangeViewModel, String> changeName;
    private final ListView<DatabaseChangeViewModel> changesList;
    private final BorderPane infoPanel = new BorderPane();
    private final CheckBox cb = new CheckBox(Localization.lang("Accept change"));

    public ExternalChangesResolverDialog(BibDatabaseContext database, List<DatabaseChangeViewModel> changes) {
        this.setTitle(Localization.lang("External changes"));
        this.getDialogPane().setPrefSize(800, 600);

        ViewLoader.view(this)
                .load().setAsDialogPane(this);
        changesTableView.itemsProperty().set(FXCollections.observableList(changes));
        changesTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        changesList = new ListView<>(FXCollections.observableArrayList(changes));
        changesList.setPrefWidth(200);
        EasyBind.subscribe(changesList.getSelectionModel().selectedItemProperty(), this::selectedChangeChanged);

        SplitPane pane = new SplitPane();
        pane.setDividerPositions(0.2);

        Button selectAllChangesFromDisk = new Button(Localization.lang("Mark all changes as accepted"));
        selectAllChangesFromDisk.setMinWidth(Region.USE_PREF_SIZE);
        selectAllChangesFromDisk.setOnAction(evt -> {
            for (DatabaseChangeViewModel change : changes) {
                change.setAccepted(true);
            }
        });
        Button unselectAllAcceptChanges = new Button(Localization.lang("Unmark all changes"));
        unselectAllAcceptChanges.setOnAction(evt -> {
            for (DatabaseChangeViewModel change : changes) {
                change.setAccepted(false);
            }
        });

        VBox leftContent = new VBox(changesList,
                selectAllChangesFromDisk,
                unselectAllAcceptChanges);

        ScrollPane leftScroll = new ScrollPane(leftContent);
        leftScroll.setFitToHeight(true);
        leftScroll.setFitToWidth(true);

        pane.getItems().addAll(leftScroll, infoPanel);
        SplitPane.setResizableWithParent(leftScroll, false);

        // getDialogPane().setContent(pane);

        Label rootInfo = new Label(Localization.lang("Select the tree nodes to view and accept or reject changes") + '.');
        infoPanel.setCenter(rootInfo);
        changesList.getSelectionModel().selectFirst();

        ButtonType dismissChanges = new ButtonType(Localization.lang("Dismiss"), ButtonData.CANCEL_CLOSE);

        setResultConverter(button -> {
            if (button == dismissChanges) {
                return false;
            } else {
                // Perform all accepted changes
                NamedCompound ce = new NamedCompound(Localization.lang("Merged external changes"));
                for (DatabaseChangeViewModel change : changes) {
                    if (change instanceof EntryChangeViewModel) {
                        // We don't have a checkbox for accept and always get the correct merged entry, the accept property in this special case only controls the radio buttons selection
                        change.makeChange(database, ce);
                    } else if (change.isAccepted()) {
                        change.makeChange(database, ce);
                    }
                }
                ce.end();
                // TODO: panel.getUndoManager().addEdit(ce);

                return true;
            }
        });
    }

    @FXML
    private void initialize() {
        changeName.setCellValueFactory(data -> data.getValue().nameProperty());
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

    @FXML
    public void acceptYours() {
    }

    @FXML
    public void acceptTheirs() {
    }

    @FXML
    public void openAdvancedMergeDialog() {
    }
}
