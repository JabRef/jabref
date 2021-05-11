package org.jabref.gui.preferences.customization;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CustomApiKeyPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class CustomizationTab extends AbstractPreferenceTabView<CustomizationTabViewModel> implements PreferencesTab {

    @FXML private ComboBox<CustomApiKeyPreferences> customApiKeyNameComboBox;
    @FXML private TextField useCustomApiKeyText;
    @FXML private CheckBox useCustomApiKeyCheckBox;
    @FXML private Button checkCustomApiKeyButton;

    @FXML private CheckBox useCustomDOI;
    @FXML private TextField useCustomDOIName;

    public CustomizationTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Customization");
    }

    /**
     * Initialize CustomizationTab
     */
    public void initialize() {
        this.viewModel = new CustomizationTabViewModel(dialogService, preferencesService);

        useCustomDOI.selectedProperty().bindBidirectional(viewModel.useCustomDOIProperty());
        useCustomDOIName.textProperty().bindBidirectional(viewModel.useCustomDOINameProperty());
        useCustomDOIName.disableProperty().bind(useCustomDOI.selectedProperty().not());

        viewModel.customApiKeyText().bind(useCustomApiKeyText.textProperty());
        viewModel.useCustomApiKeyProperty().bind(useCustomApiKeyCheckBox.selectedProperty());
        useCustomApiKeyText.disableProperty().bind(useCustomApiKeyCheckBox.selectedProperty().not());
        checkCustomApiKeyButton.disableProperty().bind(useCustomApiKeyCheckBox.selectedProperty().not());

        new ViewModelListCellFactory<CustomApiKeyPreferences>()
                .withText(CustomApiKeyPreferences::getName)
                .install(customApiKeyNameComboBox);

        customApiKeyNameComboBox.itemsProperty().bind(viewModel.customApiKeyPrefsProperty());
        customApiKeyNameComboBox.valueProperty().bindBidirectional(viewModel.selectedCustomApiKeyPrefProperty());
        customApiKeyNameComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.useCustom(useCustomApiKeyCheckBox.isSelected());
                oldValue.setDefaultApiKey(useCustomApiKeyText.getText().trim());
            }
            if (newValue != null) {
                useCustomApiKeyCheckBox.setSelected(newValue.isUseCustom());
                useCustomApiKeyText.setText(newValue.getDefaultApiKey());
            }
        });

    }

    /**
     * Trigger check custom API Key when clicking the check connection button
     */
    @FXML
    void checkCustomApiKey() {
        viewModel.checkCustomApiKey();
    }

}
