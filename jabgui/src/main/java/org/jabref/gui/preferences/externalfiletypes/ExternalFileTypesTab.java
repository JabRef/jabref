package org.jabref.gui.preferences.externalfiletypes;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;

/// Editor for external file types.
public class ExternalFileTypesTab extends AbstractPreferenceTabView<ExternalFileTypesTabViewModel> {

    private TableView<ExternalFileTypeItemViewModel> fileTypesTable;

    public ExternalFileTypesTab() {
        viewModel = new ExternalFileTypesTabViewModel(preferences.getExternalApplicationsPreferences(), dialogService);
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("External file types");
    }

    private void buildView() {
        setContent(form()
                .table(buildFileTypesTable())
                .buttonRow(
                        ControlHelper.labelledIconButton(IconTheme.JabRefIcons.ADD_NOBOX, Localization.lang("Add new file type"), this::addNewType),
                        ControlHelper.labelledIconButton(IconTheme.JabRefIcons.REFRESH, Localization.lang("Reset to default"), viewModel::resetToDefaults))
                .build());
    }

    private TableView<ExternalFileTypeItemViewModel> buildFileTypesTable() {
        fileTypesTable = new TableView<>();
        fileTypesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        fileTypesTable.setItems(viewModel.getFileTypes());

        TableColumn<ExternalFileTypeItemViewModel, JabRefIcon> iconColumn = new TableColumn<>();
        iconColumn.setMinWidth(40.0);
        iconColumn.setMaxWidth(40.0);
        iconColumn.setCellValueFactory(cellData -> cellData.getValue().iconProperty());
        new ValueTableCellFactory<ExternalFileTypeItemViewModel, JabRefIcon>()
                .withGraphic(JabRefIcon::getGraphicNode)
                .install(iconColumn);

        TableColumn<ExternalFileTypeItemViewModel, String> nameColumn = new TableColumn<>(Localization.lang("Name"));
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        new ValueTableCellFactory<ExternalFileTypeItemViewModel, String>()
                .withText(name -> name)
                .install(nameColumn);

        TableColumn<ExternalFileTypeItemViewModel, String> extensionColumn = new TableColumn<>(Localization.lang("Extension"));
        extensionColumn.setPrefWidth(120.0);
        extensionColumn.setCellValueFactory(cellData -> cellData.getValue().extensionProperty());
        new ValueTableCellFactory<ExternalFileTypeItemViewModel, String>()
                .withText(extension -> extension)
                .install(extensionColumn);

        TableColumn<ExternalFileTypeItemViewModel, String> mimeTypeColumn = new TableColumn<>(Localization.lang("MIME type"));
        mimeTypeColumn.setPrefWidth(150.0);
        mimeTypeColumn.setCellValueFactory(cellData -> cellData.getValue().mimetypeProperty());
        new ValueTableCellFactory<ExternalFileTypeItemViewModel, String>()
                .withText(mimetype -> mimetype)
                .install(mimeTypeColumn);

        TableColumn<ExternalFileTypeItemViewModel, String> applicationColumn = new TableColumn<>(Localization.lang("Application"));
        applicationColumn.setPrefWidth(100.0);
        applicationColumn.setCellValueFactory(cellData -> cellData.getValue().applicationProperty());
        new ValueTableCellFactory<ExternalFileTypeItemViewModel, String>()
                .withText(application -> application)
                .install(applicationColumn);

        TableColumn<ExternalFileTypeItemViewModel, Boolean> editColumn = new TableColumn<>();
        editColumn.setMinWidth(40.0);
        editColumn.setMaxWidth(40.0);
        editColumn.setCellValueFactory(_ -> BindingsHelper.constantOf(true));
        new ValueTableCellFactory<ExternalFileTypeItemViewModel, Boolean>()
                .withGraphic(_ -> IconTheme.JabRefIcons.EDIT.getGraphicNode())
                .withOnMouseClickedEvent((type, _) -> _ -> editType(type))
                .install(editColumn);

        TableColumn<ExternalFileTypeItemViewModel, Boolean> deleteColumn = new TableColumn<>();
        deleteColumn.setMinWidth(40.0);
        deleteColumn.setMaxWidth(40.0);
        deleteColumn.setCellValueFactory(_ -> BindingsHelper.constantOf(true));
        new ValueTableCellFactory<ExternalFileTypeItemViewModel, Boolean>()
                .withGraphic(_ -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withOnMouseClickedEvent((type, _) -> _ -> viewModel.remove(type))
                .install(deleteColumn);

        fileTypesTable.getColumns().add(iconColumn);
        fileTypesTable.getColumns().add(nameColumn);
        fileTypesTable.getColumns().add(extensionColumn);
        fileTypesTable.getColumns().add(mimeTypeColumn);
        fileTypesTable.getColumns().add(applicationColumn);
        fileTypesTable.getColumns().add(editColumn);
        fileTypesTable.getColumns().add(deleteColumn);

        return fileTypesTable;
    }

    private void editType(ExternalFileTypeItemViewModel type) {
        if (viewModel.edit(type)) {
            fileTypesTable.getSelectionModel().selectLast();
            fileTypesTable.scrollTo(viewModel.getFileTypes().size() - 1);
        }
    }

    private void addNewType() {
        if (viewModel.addNewType()) {
            fileTypesTable.getSelectionModel().selectLast();
            fileTypesTable.scrollTo(viewModel.getFileTypes().size() - 1);
        }
    }
}
