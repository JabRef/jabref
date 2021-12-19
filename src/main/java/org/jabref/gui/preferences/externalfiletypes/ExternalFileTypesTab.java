package org.jabref.gui.preferences.externalfiletypes;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
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

    @FXML private TableColumn<ExternalFileType, JabRefIcon> fileTypesTableIconColumn;
    @FXML private TableColumn<ExternalFileType, String> fileTypesTableNameColumn;
    @FXML private TableColumn<ExternalFileType, String> fileTypesTableExtensionColumn;
    @FXML private TableColumn<ExternalFileType, String> fileTypesTableTypeColumn;
    @FXML private TableColumn<ExternalFileType, String> fileTypesTableApplicationColumn;
    @FXML private TableColumn<ExternalFileType, Boolean> fileTypesTableEditColumn;
    @FXML private TableColumn<ExternalFileType, Boolean> fileTypesTableDeleteColumn;
    @FXML private TableView<ExternalFileType> fileTypesTable;

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
        viewModel = new ExternalFileTypesTabViewModel(ExternalFileTypes.getInstance());

        fileTypesTable.setItems(viewModel.getFileTypes());

        fileTypesTableIconColumn.setCellValueFactory(data -> BindingsHelper.constantOf(data.getValue().getIcon()));
        fileTypesTableNameColumn.setCellValueFactory(data -> BindingsHelper.constantOf(data.getValue().getName()));
        fileTypesTableExtensionColumn.setCellValueFactory(data -> BindingsHelper.constantOf(data.getValue().getExtension()));
        fileTypesTableTypeColumn.setCellValueFactory(data -> BindingsHelper.constantOf(data.getValue().getMimeType()));
        fileTypesTableApplicationColumn.setCellValueFactory(data -> BindingsHelper.constantOf(data.getValue().getOpenWithApplication()));
        fileTypesTableEditColumn.setCellValueFactory(data -> BindingsHelper.constantOf(true));
        fileTypesTableDeleteColumn.setCellValueFactory(data -> BindingsHelper.constantOf(true));

        new ValueTableCellFactory<ExternalFileType, JabRefIcon>()
                .withGraphic(JabRefIcon::getGraphicNode)
                .install(fileTypesTableIconColumn);
        new ValueTableCellFactory<ExternalFileType, Boolean>()
                .withGraphic(none -> IconTheme.JabRefIcons.EDIT.getGraphicNode())
                .withOnMouseClickedEvent((type, none) -> event -> viewModel.edit(type))
                .install(fileTypesTableEditColumn);
        new ValueTableCellFactory<ExternalFileType, Boolean>()
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
