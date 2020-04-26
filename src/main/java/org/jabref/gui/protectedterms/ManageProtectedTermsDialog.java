package org.jabref.gui.protectedterms;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.protectedterms.ProtectedTermsList;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * Dialog for managing term list files.
 */
public class ManageProtectedTermsDialog extends BaseDialog<Void> {

    @FXML private TableView<ProtectedTermsList> filesTable;
    @FXML private TableColumn<ProtectedTermsList, Boolean> filesTableEnabledColumn;
    @FXML private TableColumn<ProtectedTermsList, String> filesTableDescriptionColumn;
    @FXML private TableColumn<ProtectedTermsList, String> filesTableFileColumn;
    @FXML private TableColumn<ProtectedTermsList, Boolean> filesTableEditColumn;
    @FXML private TableColumn<ProtectedTermsList, Boolean> filesTableDeleteColumn;

    @Inject private ProtectedTermsLoader termsLoader;
    @Inject private DialogService dialogService;
    @Inject private PreferencesService preferences;
    private ManageProtectedTermsViewModel viewModel;

    public ManageProtectedTermsDialog() {
        this.setTitle(Localization.lang("Manage protected terms files"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(button -> {
            if (button.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                viewModel.save();
            }
            return null;
        });
    }

    @FXML
    public void initialize() {
        viewModel = new ManageProtectedTermsViewModel(termsLoader, dialogService, preferences);

        filesTable.setItems(viewModel.getTermsFiles());
        new ViewModelTableRowFactory<ProtectedTermsList>()
                .withContextMenu(this::createContextMenu)
                .install(filesTable);
        filesTableEnabledColumn.setCellValueFactory(data -> BindingsHelper.constantOf(data.getValue().isEnabled()));
        filesTableEnabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(filesTableEnabledColumn));
        filesTableDescriptionColumn.setCellValueFactory(data -> BindingsHelper.constantOf(data.getValue().getDescription()));
        filesTableFileColumn.setCellValueFactory(data -> {
            ProtectedTermsList list = data.getValue();
            if (list.isInternalList()) {
                return BindingsHelper.constantOf(Localization.lang("Internal list"));
            } else {
                return BindingsHelper.constantOf(data.getValue().getLocation());
            }
        });
        filesTableEditColumn.setCellValueFactory(data -> BindingsHelper.constantOf(true));
        filesTableDeleteColumn.setCellValueFactory(data -> BindingsHelper.constantOf(true));

        new ValueTableCellFactory<ProtectedTermsList, Boolean>()
                .withGraphic(none -> IconTheme.JabRefIcons.EDIT.getGraphicNode())
                .withOnMouseClickedEvent((file, none) -> event -> viewModel.edit(file))
                .install(filesTableEditColumn);
        new ValueTableCellFactory<ProtectedTermsList, Boolean>()
                .withGraphic(none -> IconTheme.JabRefIcons.REMOVE.getGraphicNode())
                .withTooltip(none -> Localization.lang("Remove protected terms file"))
                .withOnMouseClickedEvent((file, none) -> event -> viewModel.removeFile(file))
                .install(filesTableDeleteColumn);
    }

    private ContextMenu createContextMenu(ProtectedTermsList file) {
        MenuItem edit = new MenuItem(Localization.lang("Edit"));
        edit.setOnAction(event -> viewModel.edit(file));
        MenuItem show = new MenuItem(Localization.lang("View"));
        show.setOnAction(event -> viewModel.displayContent(file));
        MenuItem remove = new MenuItem(Localization.lang("Remove"));
        remove.setOnAction(event -> viewModel.removeFile(file));
        MenuItem reload = new MenuItem(Localization.lang("Reload"));
        reload.setOnAction(event -> viewModel.reloadFile(file));

        // Enable/disable context menu items
        if (file.isInternalList()) {
            edit.setDisable(true);
            show.setDisable(false);
            remove.setDisable(true);
            reload.setDisable(true);
        } else {
            edit.setDisable(false);
            show.setDisable(false);
            remove.setDisable(false);
            reload.setDisable(false);
        }

        final ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(edit, show, remove, reload);
        return contextMenu;
    }

    @FXML
    private void addFile() {
        viewModel.addFile();
    }

    @FXML
    private void createNewFile() {
        viewModel.createNewFile();
    }
}
