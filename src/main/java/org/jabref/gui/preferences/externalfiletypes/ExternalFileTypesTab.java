package org.jabref.gui.preferences.externalfiletypes;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * Editor for external file types.
 */
public class ExternalFileTypesTab extends AbstractPreferenceTabView<ExternalFileTypesTabViewModel> implements PreferencesTab {

    @FXML private TableColumn<ExternalFileTypeItemViewModel, JabRefIcon> fileTypesTableIconColumn;
    @FXML private TableColumn<ExternalFileTypeItemViewModel, String> fileTypesTableNameColumn;
    @FXML private TableColumn<ExternalFileTypeItemViewModel, String> fileTypesTableExtensionColumn;
    @FXML private TableColumn<ExternalFileTypeItemViewModel, String> fileTypesTableMimeTypeColumn;
    @FXML private TableColumn<ExternalFileTypeItemViewModel, String> fileTypesTableApplicationColumn;
    @FXML private TableColumn<ExternalFileTypeItemViewModel, Boolean> fileTypesTableEditColumn;
    @FXML private TableColumn<ExternalFileTypeItemViewModel, Boolean> fileTypesTableDeleteColumn;
    @FXML private TableView<ExternalFileTypeItemViewModel> fileTypesTable;

    public ExternalFileTypesTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("External file types");
    }

    @FXML
    public void initialize() {
        viewModel = new ExternalFileTypesTabViewModel(preferencesService.getFilePreferences(), dialogService);

        fileTypesTable.setItems(viewModel.getFileTypes());

        fileTypesTableIconColumn.setCellValueFactory(cellData -> cellData.getValue().iconProperty());
        new ValueTableCellFactory<ExternalFileTypeItemViewModel, JabRefIcon>()
                .withGraphic(JabRefIcon::getGraphicNode)
                .install(fileTypesTableIconColumn);

        fileTypesTableNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        new ValueTableCellFactory<ExternalFileTypeItemViewModel, String>()
                .withText(name -> name)
                .install(fileTypesTableNameColumn);

        fileTypesTableExtensionColumn.setCellValueFactory(cellData -> cellData.getValue().extensionProperty());
        new ValueTableCellFactory<ExternalFileTypeItemViewModel, String>()
                .withText(extension -> extension)
                .install(fileTypesTableExtensionColumn);

        fileTypesTableMimeTypeColumn.setCellValueFactory(cellData -> cellData.getValue().mimetypeProperty());
        new ValueTableCellFactory<ExternalFileTypeItemViewModel, String>()
                .withText(mimetype -> mimetype)
                .install(fileTypesTableMimeTypeColumn);

        fileTypesTableApplicationColumn.setCellValueFactory(cellData -> cellData.getValue().applicationProperty());
        new ValueTableCellFactory<ExternalFileTypeItemViewModel, String>()
                .withText(extension -> extension)
                .install(fileTypesTableApplicationColumn);

        fileTypesTableEditColumn.setCellValueFactory(data -> BindingsHelper.constantOf(true));
        fileTypesTableDeleteColumn.setCellValueFactory(data -> BindingsHelper.constantOf(true));

        new ValueTableCellFactory<ExternalFileTypeItemViewModel, JabRefIcon>()
                .withGraphic(JabRefIcon::getGraphicNode)
                .install(fileTypesTableIconColumn);
        new ValueTableCellFactory<ExternalFileTypeItemViewModel, Boolean>()
                .withGraphic(none -> IconTheme.JabRefIcons.EDIT.getGraphicNode())
                .withOnMouseClickedEvent((type, none) -> event -> viewModel.edit(type))
                .install(fileTypesTableEditColumn);
        new ValueTableCellFactory<ExternalFileTypeItemViewModel, Boolean>()
                .withGraphic(none -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withOnMouseClickedEvent((type, none) -> event -> viewModel.remove(type))
                .install(fileTypesTableDeleteColumn);
    }

    @FXML
    private void addNewType() {
        viewModel.addNewType();
        fileTypesTable.getSelectionModel().selectLast();
        fileTypesTable.scrollTo(viewModel.getFileTypes().size() - 1);
    }

    @FXML
    private void resetToDefault() {
        viewModel.resetToDefaults();
    }
}
