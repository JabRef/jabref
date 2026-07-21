package org.jabref.gui.preferences.protectedterms;

import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.protectedterms.ProtectedTermsList;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;

import com.airhacks.afterburner.injection.Injector;

/// Tab for managing term list files.
public class ProtectedTermsTab extends AbstractPreferenceTabView<ProtectedTermsTabViewModel> {

    public ProtectedTermsTab() {
        ProtectedTermsLoader termsLoader = Injector.instantiateModelOrService(ProtectedTermsLoader.class);
        viewModel = new ProtectedTermsTabViewModel(termsLoader, dialogService, preferences);
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Protected terms files");
    }

    private void buildView() {
        getChildren().add(form()
                .title(Localization.lang("Protected terms files"))
                .custom(buildFilesTable(), table -> table.configure(t -> VBox.setVgrow(t, Priority.ALWAYS)))
                .custom(buildButtonRow())
                .build());
    }

    private TableView<ProtectedTermsListItemModel> buildFilesTable() {
        TableView<ProtectedTermsListItemModel> filesTable = new TableView<>();
        filesTable.setEditable(true);
        filesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ProtectedTermsListItemModel, Boolean> enabledColumn = new TableColumn<>(Localization.lang("Enabled"));
        enabledColumn.setMinWidth(90.0);
        enabledColumn.setPrefWidth(70.0);
        enabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(enabledColumn));
        enabledColumn.setCellValueFactory(data -> data.getValue().enabledProperty());

        TableColumn<ProtectedTermsListItemModel, String> descriptionColumn = new TableColumn<>(Localization.lang("Description"));
        descriptionColumn.setCellValueFactory(data -> BindingsHelper.constantOf(data.getValue().getTermsList().getDescription()));

        TableColumn<ProtectedTermsListItemModel, String> fileColumn = new TableColumn<>(Localization.lang("File"));
        fileColumn.setCellValueFactory(data -> {
            ProtectedTermsList list = data.getValue().getTermsList();
            if (list.isInternalList()) {
                return BindingsHelper.constantOf(Localization.lang("Internal list"));
            } else {
                return BindingsHelper.constantOf(list.getLocation());
            }
        });

        TableColumn<ProtectedTermsListItemModel, Boolean> editColumn = new TableColumn<>();
        editColumn.setMinWidth(35.0);
        editColumn.setMaxWidth(35.0);
        editColumn.setReorderable(false);
        editColumn.setCellValueFactory(data -> data.getValue().internalProperty().not());
        new ValueTableCellFactory<ProtectedTermsListItemModel, Boolean>()
                .withGraphic(_ -> IconTheme.JabRefIcons.EDIT.getGraphicNode())
                .withVisibleExpression(ReadOnlyBooleanWrapper::new)
                .withOnMouseClickedEvent((item, _) -> _ -> viewModel.edit(item))
                .install(editColumn);

        TableColumn<ProtectedTermsListItemModel, Boolean> deleteColumn = new TableColumn<>();
        deleteColumn.setMinWidth(35.0);
        deleteColumn.setMaxWidth(35.0);
        deleteColumn.setReorderable(false);
        deleteColumn.setCellValueFactory(data -> data.getValue().internalProperty().not());
        new ValueTableCellFactory<ProtectedTermsListItemModel, Boolean>()
                .withGraphic(_ -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withVisibleExpression(ReadOnlyBooleanWrapper::new)
                .withTooltip(_ -> Localization.lang("Remove protected terms file"))
                .withOnMouseClickedEvent((item, _) -> _ -> viewModel.removeList(item))
                .install(deleteColumn);

        filesTable.getColumns().add(enabledColumn);
        filesTable.getColumns().add(descriptionColumn);
        filesTable.getColumns().add(fileColumn);
        filesTable.getColumns().add(editColumn);
        filesTable.getColumns().add(deleteColumn);

        new ViewModelTableRowFactory<ProtectedTermsListItemModel>()
                .withContextMenu(this::createContextMenu)
                .install(filesTable);

        filesTable.itemsProperty().set(viewModel.termsFilesProperty());
        return filesTable;
    }

    private Node buildButtonRow() {
        HBox row = new HBox(10.0);
        row.setAlignment(Pos.BASELINE_RIGHT);
        row.getChildren().addAll(
                iconButton(Localization.lang("Add protected terms file"), IconTheme.JabRefIcons.OPEN_LIST, viewModel::addFile),
                iconButton(Localization.lang("New protected terms file"), IconTheme.JabRefIcons.ADD_NOBOX, viewModel::createNewFile));
        return row;
    }

    private Button iconButton(String text, IconTheme.JabRefIcons icon, Runnable action) {
        Button button = new Button(text);
        button.setGraphic(icon.getGraphicNode());
        button.setOnAction(_ -> action.run());
        return button;
    }

    private ContextMenu createContextMenu(ProtectedTermsListItemModel file) {
        ActionFactory factory = new ActionFactory();
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

    private class ContextAction extends SimpleCommand {

        private final StandardActions command;
        private final ProtectedTermsListItemModel itemModel;

        public ContextAction(StandardActions command, ProtectedTermsListItemModel itemModel) {
            this.command = command;
            this.itemModel = itemModel;

            this.executable.bind(BindingsHelper.constantOf(
                    switch (command) {
                        case EDIT_LIST,
                             REMOVE_LIST,
                             RELOAD_LIST ->
                                !itemModel.getTermsList().isInternalList();
                        default ->
                                true;
                    }));
        }

        @Override
        public void execute() {
            switch (command) {
                case EDIT_LIST ->
                        viewModel.edit(itemModel);
                case VIEW_LIST ->
                        viewModel.displayContent(itemModel);
                case REMOVE_LIST ->
                        viewModel.removeList(itemModel);
                case RELOAD_LIST ->
                        viewModel.reloadList(itemModel);
            }
        }
    }
}
