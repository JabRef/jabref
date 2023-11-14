package org.jabref.gui.preferences.websearch;

import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.slr.StudyCatalogItem;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.FetcherApiKey;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;

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

    @FXML private CheckBox persistApiKeys;
    @FXML private SplitPane persistentTooltipWrapper; // The disabled persistApiKeys control does not show tooltips
    @FXML private TableView<StudyCatalogItem> catalogTable;
    @FXML private TableColumn<StudyCatalogItem, Boolean> catalogEnabledColumn;
    @FXML private TableColumn<StudyCatalogItem, String> catalogColumn;

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

        new ViewModelTableRowFactory<StudyCatalogItem>()
                .withOnMouseClickedEvent((entry, event) -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        entry.setEnabled(!entry.isEnabled());
                    }
                })
                .install(catalogTable);

        catalogColumn.setReorderable(false);
        catalogColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        catalogEnabledColumn.setResizable(false);
        catalogEnabledColumn.setReorderable(false);
        catalogEnabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(catalogEnabledColumn));
        catalogEnabledColumn.setCellValueFactory(param -> param.getValue().enabledProperty());

        catalogColumn.setEditable(false);
        catalogColumn.setCellValueFactory(param -> param.getValue().nameProperty());
        catalogTable.setItems(viewModel.getCatalogs());

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

        persistApiKeys.selectedProperty().bindBidirectional(viewModel.getApikeyPersistProperty());
        persistApiKeys.disableProperty().bind(viewModel.apiKeyPersistAvailable().not());
        EasyBind.subscribe(viewModel.apiKeyPersistAvailable(), available -> {
            if (!available) {
                persistentTooltipWrapper.setTooltip(new Tooltip(Localization.lang("Credential store not available.")));
            } else {
                persistentTooltipWrapper.setTooltip(null);
            }
        });

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
