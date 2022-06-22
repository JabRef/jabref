package org.jabref.gui.preferences.protectedterms;

import javax.inject.Inject;

import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;

import org.jabref.gui.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.protectedterms.ProtectedTermsList;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * Dialog for managing term list files.
 */
public class ProtectedTermsTab extends AbstractPreferenceTabView<ProtectedTermsTabViewModel> implements PreferencesTab {
    @FXML private TableView<ProtectedTermsListItemModel> filesTable;
    @FXML private TableColumn<ProtectedTermsListItemModel, Boolean> filesTableEnabledColumn;
    @FXML private TableColumn<ProtectedTermsListItemModel, String> filesTableDescriptionColumn;
    @FXML private TableColumn<ProtectedTermsListItemModel, String> filesTableFileColumn;
    @FXML private TableColumn<ProtectedTermsListItemModel, Boolean> filesTableEditColumn;
    @FXML private TableColumn<ProtectedTermsListItemModel, Boolean> filesTableDeleteColumn;

    @Inject private ProtectedTermsLoader termsLoader;

    public ProtectedTermsTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Protected terms files");
    }

    @FXML
    public void initialize() {
        viewModel = new ProtectedTermsTabViewModel(termsLoader, dialogService, preferencesService);

        new ViewModelTableRowFactory<ProtectedTermsListItemModel>()
                .withContextMenu(this::createContextMenu)
                .install(filesTable);
        filesTableEnabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(filesTableEnabledColumn));
        filesTableEnabledColumn.setCellValueFactory(data -> data.getValue().enabledProperty());
        filesTableDescriptionColumn.setCellValueFactory(data -> BindingsHelper.constantOf(data.getValue().getTermsList().getDescription()));

        filesTableFileColumn.setCellValueFactory(data -> {
            ProtectedTermsList list = data.getValue().getTermsList();
            if (list.isInternalList()) {
                return BindingsHelper.constantOf(Localization.lang("Internal list"));
            } else {
                return BindingsHelper.constantOf(list.getLocation());
            }
        });

        filesTableEditColumn.setCellValueFactory(data -> data.getValue().internalProperty().not());
        new ValueTableCellFactory<ProtectedTermsListItemModel, Boolean>()
                .withGraphic(none -> IconTheme.JabRefIcons.EDIT.getGraphicNode())
                .withVisibleExpression(ReadOnlyBooleanWrapper::new)
                .withOnMouseClickedEvent((item, none) -> event -> viewModel.edit(item))
                .install(filesTableEditColumn);

        filesTableDeleteColumn.setCellValueFactory(data -> data.getValue().internalProperty().not());
        new ValueTableCellFactory<ProtectedTermsListItemModel, Boolean>()
                .withGraphic(none -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withVisibleExpression(ReadOnlyBooleanWrapper::new)
                .withTooltip(none -> Localization.lang("Remove protected terms file"))
                .withOnMouseClickedEvent((item, none) -> event -> viewModel.removeList(item))
                .install(filesTableDeleteColumn);

        filesTable.itemsProperty().set(viewModel.termsFilesProperty());
    }

    private ContextMenu createContextMenu(ProtectedTermsListItemModel file) {
        ActionFactory factory = new ActionFactory(Globals.getKeyPrefs());
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.EDIT_LIST, new ProtectedTermsTab.ContextAction(StandardActions.EDIT_LIST, file)),
                factory.createMenuItem(StandardActions.VIEW_LIST, new ProtectedTermsTab.ContextAction(StandardActions.VIEW_LIST, file)),
                factory.createMenuItem(StandardActions.REMOVE_LIST, new ProtectedTermsTab.ContextAction(StandardActions.REMOVE_LIST, file)),
                factory.createMenuItem(StandardActions.RELOAD_LIST, new ProtectedTermsTab.ContextAction(StandardActions.RELOAD_LIST, file))
        );
        contextMenu.getItems().forEach(item -> item.setGraphic(null));
        contextMenu.getStyleClass().add("context-menu");

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

    private class ContextAction extends SimpleCommand {

        private final StandardActions command;
        private final ProtectedTermsListItemModel itemModel;

        public ContextAction(StandardActions command, ProtectedTermsListItemModel itemModel) {
            this.command = command;
            this.itemModel = itemModel;

            this.executable.bind(BindingsHelper.constantOf(
                    switch (command) {
                        case EDIT_LIST, REMOVE_LIST, RELOAD_LIST -> !itemModel.getTermsList().isInternalList();
                        default -> true;
                    }));
        }

        @Override
        public void execute() {
            switch (command) {
                case EDIT_LIST -> viewModel.edit(itemModel);
                case VIEW_LIST -> viewModel.displayContent(itemModel);
                case REMOVE_LIST -> viewModel.removeList(itemModel);
                case RELOAD_LIST -> viewModel.reloadList(itemModel);
            }
        }
    }
}
