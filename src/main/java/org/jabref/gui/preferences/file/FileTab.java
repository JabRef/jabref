package org.jabref.gui.preferences.file;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class FileTab extends AbstractPreferenceTabView<FileTabViewModel> implements PreferencesTab {
    @FXML private CheckBox resolveStrings;
    @FXML private TextField resolveStringsForFields;
    @FXML private TextField nonWrappableFields;

    public FileTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("File");
    }

    public void initialize() {
        this.viewModel = new FileTabViewModel(preferencesService.getFieldPreferences());

        resolveStrings.selectedProperty().bindBidirectional(viewModel.resolveStringsProperty());
        resolveStringsForFields.textProperty().bindBidirectional(viewModel.resolveStringsForFieldsProperty());
        nonWrappableFields.textProperty().bindBidirectional(viewModel.nonWrappableFieldsProperty());
    }
}
