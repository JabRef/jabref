package org.jabref.gui.preferences.externalfiletypes;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.util.BindingsHelper;
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
        getChildren().add(form()
                .title(Localization.lang("External file types"))
                .custom(buildFileTypesTable(), table -> table.configure(t -> VBox.setVgrow(t, Priority.ALWAYS)))
                .custom(buildButtonRow())
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

    private Node buildButtonRow() {
        Button addNewType = new Button(Localization.lang("Add new file type"));
        addNewType.setGraphic(IconTheme.JabRefIcons.ADD_NOBOX.getGraphicNode());
        addNewType.setOnAction(_ -> addNewType());

        Button resetToDefault = new Button(Localization.lang("Reset to default"));
        resetToDefault.setGraphic(IconTheme.JabRefIcons.REFRESH.getGraphicNode());
        resetToDefault.setOnAction(_ -> viewModel.resetToDefaults());

        HBox row = new HBox(10.0, addNewType, resetToDefault);
        row.setAlignment(Pos.BASELINE_RIGHT);
        return row;
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
