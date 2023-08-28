package org.jabref.gui.preferences.websearch;

import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.FetcherApiKey;

import com.airhacks.afterburner.views.ViewLoader;

public class WebSearchTab extends AbstractPreferenceTabView<WebSearchTabViewModel> implements PreferencesTab {

    @FXML private CheckBox enableWebSearch;
    @FXML private CheckBox generateNewKeyOnImport;
    @FXML private CheckBox warnAboutDuplicatesOnImport;
    @FXML private CheckBox downloadLinkedOnlineFiles;

    @FXML private CheckBox useCustomDOI;
    @FXML private TextField useCustomDOIName;

    @FXML private CheckBox grobidEnabled;
    @FXML private TextField grobidURL;

    @FXML private ComboBox<FetcherApiKey> apiKeySelector;
    @FXML private TextField customApiKey;
    @FXML private CheckBox useCustomApiKey;
    @FXML private Button testCustomApiKey;

    public WebSearchTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Web search");
    }

    public void initialize() {
        this.viewModel = new WebSearchTabViewModel(preferencesService, dialogService);

        enableWebSearch.selectedProperty().bindBidirectional(viewModel.enableWebSearchProperty());
        generateNewKeyOnImport.selectedProperty().bindBidirectional(viewModel.generateKeyOnImportProperty());
        warnAboutDuplicatesOnImport.selectedProperty().bindBidirectional(viewModel.warnAboutDuplicatesOnImportProperty());
        downloadLinkedOnlineFiles.selectedProperty().bindBidirectional(viewModel.shouldDownloadLinkedOnlineFiles());

        grobidEnabled.selectedProperty().bindBidirectional(viewModel.grobidEnabledProperty());
        grobidURL.textProperty().bindBidirectional(viewModel.grobidURLProperty());
        grobidURL.disableProperty().bind(grobidEnabled.selectedProperty().not());

        useCustomDOI.selectedProperty().bindBidirectional(viewModel.useCustomDOIProperty());
        useCustomDOIName.textProperty().bindBidirectional(viewModel.useCustomDOINameProperty());
        useCustomDOIName.disableProperty().bind(useCustomDOI.selectedProperty().not());

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
