package net.sf.jabref.gui.keyboard;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

import net.sf.jabref.gui.AbstractController;
import net.sf.jabref.gui.DialogService;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.util.RecursiveTreeItem;
import net.sf.jabref.gui.util.ViewModelTreeTableCellFactory;

import org.fxmisc.easybind.EasyBind;

public class KeyBindingsDialogController extends AbstractController<KeyBindingsDialogViewModel> {

    @FXML private TreeTableView<KeyBindingViewModel> keyBindingsTable;
    @FXML private TreeTableColumn<KeyBindingViewModel, String> actionColumn;
    @FXML private TreeTableColumn<KeyBindingViewModel, String> shortcutColumn;
    @FXML private TreeTableColumn<KeyBindingViewModel, String> resetColumn;

    @Inject private KeyBindingPreferences keyBindingPreferences;
    @Inject private DialogService dialogService;

    @FXML
    private void initialize() {
        viewModel = new KeyBindingsDialogViewModel(keyBindingPreferences, dialogService);

        keyBindingsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        viewModel.selectedKeyBindingProperty().bind(
                EasyBind.monadic(keyBindingsTable.selectionModelProperty())
                        .flatMap(SelectionModel::selectedItemProperty)
                        .selectProperty(TreeItem::valueProperty)
        );
        keyBindingsTable.setOnKeyPressed(evt -> viewModel.setNewBindingForCurrent(evt));
        keyBindingsTable.rootProperty().bind(
                EasyBind.map(viewModel.rootKeyBindingProperty(),
                        keybinding -> new RecursiveTreeItem<>(keybinding, KeyBindingViewModel::getChildren))
        );
        actionColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().nameProperty());
        shortcutColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().shownBindingProperty());
        resetColumn.setCellFactory(new ViewModelTreeTableCellFactory<KeyBindingViewModel, String>()
                .withGraphic(keyBinding -> keyBinding.getIcon().map(IconTheme.JabRefIcon::getGraphicNode).orElse(null))
                .withOnMouseClickedEvent(keyBinding -> evt -> keyBinding.resetToDefault())
        );
    }

    @FXML
    private void closeDialog() {
        getStage().close();
    }

    @FXML
    private void saveKeyBindingsAndCloseDialog() {
        viewModel.saveKeyBindings();
        closeDialog();
    }

    @FXML
    private void setDefaultBindings() {
        viewModel.resetToDefault();
    }
}
