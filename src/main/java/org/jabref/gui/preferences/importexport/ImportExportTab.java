package org.jabref.gui.preferences.importexport;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.commonfxcontrols.SaveOrderConfigPanel;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CustomApiKeyPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class ImportExportTab extends AbstractPreferenceTabView<ImportExportTabViewModel> implements PreferencesTab {

    @FXML private CheckBox generateNewKeyOnImport;
    @FXML private CheckBox useCustomDOI;
    @FXML private TextField useCustomDOIName;

    @FXML private SaveOrderConfigPanel exportOrderPanel;

    @FXML private ComboBox<CustomApiKeyPreferences> customApiKeyNameComboBox;
    @FXML private TextField useCustomApiKeyText;
    @FXML private CheckBox useCustomApiKeyCheckBox;
    @FXML private Button checkCustomApiKeyButton;

    @FXML private CheckBox grobidEnabled;
    @FXML private TextField grobidURL;

    public ImportExportTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Import and Export");
    }

    public void initialize() {
        this.viewModel = new ImportExportTabViewModel(preferencesService, preferencesService.getDOIPreferences());

        useCustomDOI.selectedProperty().bindBidirectional(viewModel.useCustomDOIProperty());
        useCustomDOIName.textProperty().bindBidirectional(viewModel.useCustomDOINameProperty());
        useCustomDOIName.disableProperty().bind(useCustomDOI.selectedProperty().not());

        generateNewKeyOnImport.selectedProperty().bindBidirectional(viewModel.generateKeyOnImportProperty());

        exportOrderPanel.saveInOriginalProperty().bindBidirectional(viewModel.saveInOriginalProperty());
        exportOrderPanel.saveInTableOrderProperty().bindBidirectional(viewModel.saveInTableOrderProperty());
        exportOrderPanel.saveInSpecifiedOrderProperty().bindBidirectional(viewModel.saveInSpecifiedOrderProperty());
        exportOrderPanel.sortableFieldsProperty().bind(viewModel.sortableFieldsProperty());
        exportOrderPanel.sortCriteriaProperty().bindBidirectional(viewModel.sortCriteriaProperty());
        exportOrderPanel.setCriteriaLimit(3);

        grobidEnabled.selectedProperty().bindBidirectional(viewModel.grobidEnabledProperty());
        grobidURL.textProperty().bindBidirectional(viewModel.grobidURLProperty());
        grobidURL.disableProperty().bind(grobidEnabled.selectedProperty().not());

        viewModel.customApiKeyText().bind(useCustomApiKeyText.textProperty());
        viewModel.useCustomApiKeyProperty().bind(useCustomApiKeyCheckBox.selectedProperty());
        useCustomApiKeyText.disableProperty().bind(useCustomApiKeyCheckBox.selectedProperty().not());
        checkCustomApiKeyButton.disableProperty().bind(useCustomApiKeyCheckBox.selectedProperty().not());

        new ViewModelListCellFactory<CustomApiKeyPreferences>()
                .withText(CustomApiKeyPreferences::getName)
                .install(customApiKeyNameComboBox);

        customApiKeyNameComboBox.itemsProperty().bind(viewModel.customApiKeyPrefsProperty());
        customApiKeyNameComboBox.valueProperty().bindBidirectional(viewModel.selectedCustomApiKeyPreferencesProperty());
        customApiKeyNameComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.shouldUseCustomKey(useCustomApiKeyCheckBox.isSelected());
                oldValue.setCustomApiKey(useCustomApiKeyText.getText().trim());
            }
            if (newValue != null) {
                useCustomApiKeyCheckBox.setSelected(newValue.shouldUseCustom());
                useCustomApiKeyText.setText(newValue.getCustomApiKey());
            }
        });
    }

    @FXML
    void checkCustomApiKey() {
        viewModel.checkCustomApiKey();
    }

}
