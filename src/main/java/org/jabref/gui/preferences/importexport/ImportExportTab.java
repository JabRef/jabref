package org.jabref.gui.preferences.importexport;

import javafx.beans.InvalidationListener;
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
import org.jabref.logic.preferences.FetcherApiKey;

import com.airhacks.afterburner.views.ViewLoader;

public class ImportExportTab extends AbstractPreferenceTabView<ImportExportTabViewModel> implements PreferencesTab {

    @FXML private CheckBox generateNewKeyOnImport;
    @FXML private CheckBox useCustomDOI;
    @FXML private TextField useCustomDOIName;

    @FXML private SaveOrderConfigPanel exportOrderPanel;

    @FXML private ComboBox<FetcherApiKey> apiKeySelector;
    @FXML private TextField customApiKey;
    @FXML private CheckBox useCustomApiKey;
    @FXML private Button testCustomApiKey;

    @FXML private CheckBox grobidEnabled;
    @FXML private TextField grobidURL;

    @FXML private CheckBox warnAboutDuplicatesOnImport;

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
        this.viewModel = new ImportExportTabViewModel(preferencesService, preferencesService.getDOIPreferences(), dialogService, preferencesService.getImportExportPreferences());

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

        new ViewModelListCellFactory<FetcherApiKey>()
                .withText(FetcherApiKey::getName)
                .install(apiKeySelector);
        apiKeySelector.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                updateFetcherApiKey(oldValue);
            }
            if (newValue != null) {
                useCustomApiKey.setSelected(newValue.shouldUse());
                customApiKey.setText(newValue.getKey());
            }
        });
        customApiKey.textProperty().addListener(listener -> updateFetcherApiKey(apiKeySelector.valueProperty().get()));

        customApiKey.disableProperty().bind(useCustomApiKey.selectedProperty().not());
        testCustomApiKey.disableProperty().bind(useCustomApiKey.selectedProperty().not());

        apiKeySelector.setItems(viewModel.fetcherApiKeys());
        viewModel.selectedApiKeyProperty().bind(apiKeySelector.valueProperty());

        // Content is set later
        viewModel.fetcherApiKeys().addListener((InvalidationListener) change -> apiKeySelector.getSelectionModel().selectFirst());

        warnAboutDuplicatesOnImport.selectedProperty().bindBidirectional(viewModel.warnAboutDuplicatesOnImportProperty());
    }

    private void updateFetcherApiKey(FetcherApiKey apiKey) {
        if (apiKey != null) {
            apiKey.setUse(useCustomApiKey.isSelected());
            apiKey.setKey(customApiKey.getText().trim());
        }
    }

    @FXML
    void checkCustomApiKey() {
        viewModel.checkCustomApiKey();
    }
}
