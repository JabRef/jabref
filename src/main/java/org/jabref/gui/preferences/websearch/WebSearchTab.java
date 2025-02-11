package org.jabref.gui.preferences.websearch;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyBooleanProperty;
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
import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.FetcherApiKey;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;

public class WebSearchTab extends AbstractPreferenceTabView<WebSearchTabViewModel> implements PreferencesTab {

    @FXML private CheckBox enableWebSearch;
    @FXML private CheckBox warnAboutDuplicatesOnImport;
    @FXML private CheckBox downloadLinkedOnlineFiles;
    @FXML private CheckBox keepDownloadUrl;
    @FXML private ComboBox<PlainCitationParserChoice> defaultPlainCitationParser;

    @FXML private CheckBox useCustomDOI;
    @FXML private TextField useCustomDOIName;

    @FXML private CheckBox grobidEnabled;
    @FXML private TextField grobidURL;

    @FXML private TableView<FetcherApiKey> apiKeySelectorTable;
    @FXML private TableColumn<FetcherApiKey, String> apiKeyName;
    @FXML private TableColumn<FetcherApiKey, String> customApiKey;
    @FXML private TableColumn<FetcherApiKey, Boolean> useCustomApiKey;
    @FXML private Button testCustomApiKey;

    @FXML private CheckBox persistApiKeys;
    @FXML private SplitPane persistentTooltipWrapper; // The disabled persistApiKeys control does not show tooltips
    @FXML private TableView<StudyCatalogItem> catalogTable;
    @FXML private TableColumn<StudyCatalogItem, Boolean> catalogEnabledColumn;
    @FXML private TableColumn<StudyCatalogItem, String> catalogColumn;

    private final ReadOnlyBooleanProperty refAiEnabled;

    public WebSearchTab(ReadOnlyBooleanProperty refAiEnabled) {
        this.refAiEnabled = refAiEnabled;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Web search");
    }

    public void initialize() {
        this.viewModel = new WebSearchTabViewModel(preferences, dialogService, refAiEnabled);

        enableWebSearch.selectedProperty().bindBidirectional(viewModel.enableWebSearchProperty());
        warnAboutDuplicatesOnImport.selectedProperty().bindBidirectional(viewModel.warnAboutDuplicatesOnImportProperty());
        downloadLinkedOnlineFiles.selectedProperty().bindBidirectional(viewModel.shouldDownloadLinkedOnlineFiles());
        keepDownloadUrl.selectedProperty().bindBidirectional(viewModel.shouldKeepDownloadUrl());

        new ViewModelListCellFactory<PlainCitationParserChoice>()
                .withText(PlainCitationParserChoice::getLocalizedName)
                .install(defaultPlainCitationParser);
        defaultPlainCitationParser.itemsProperty().bind(viewModel.plainCitationParsers());
        defaultPlainCitationParser.valueProperty().bindBidirectional(viewModel.defaultPlainCitationParserProperty());

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

        testCustomApiKey.setDisable(true);

        new ViewModelTableRowFactory<FetcherApiKey>()
                .install(apiKeySelectorTable);

        apiKeySelectorTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                updateFetcherApiKey(oldValue);
            }
            if (newValue != null) {
                viewModel.selectedApiKeyProperty().setValue(newValue);
                testCustomApiKey.disableProperty().bind(newValue.useProperty().not());
            }
        });

        apiKeyName.setCellValueFactory(param -> param.getValue().nameProperty());
        apiKeyName.setCellFactory(TextFieldTableCell.forTableColumn());
        apiKeyName.setReorderable(false);
        apiKeyName.setEditable(false);

        customApiKey.setCellValueFactory(param -> param.getValue().keyProperty());
        customApiKey.setCellFactory(TextFieldTableCell.forTableColumn());
        customApiKey.setReorderable(false);
        customApiKey.setResizable(true);
        customApiKey.setEditable(true);

        useCustomApiKey.setCellValueFactory(param -> param.getValue().useProperty());
        useCustomApiKey.setCellFactory(CheckBoxTableCell.forTableColumn(useCustomApiKey));
        useCustomApiKey.setEditable(true);
        useCustomApiKey.setResizable(true);
        useCustomApiKey.setReorderable(false);

        persistApiKeys.selectedProperty().bindBidirectional(viewModel.getApikeyPersistProperty());
        persistApiKeys.disableProperty().bind(viewModel.apiKeyPersistAvailable().not());
        EasyBind.subscribe(viewModel.apiKeyPersistAvailable(), available -> {
            if (!available) {
                persistentTooltipWrapper.setTooltip(new Tooltip(Localization.lang("Credential store not available.")));
            } else {
                persistentTooltipWrapper.setTooltip(null);
            }
        });

        apiKeySelectorTable.setItems(viewModel.fetcherApiKeys());

        // Content is set later
        viewModel.fetcherApiKeys().addListener((InvalidationListener) change -> {
            if (!apiKeySelectorTable.getItems().isEmpty()) {
                apiKeySelectorTable.getSelectionModel().selectFirst();
            }
        });
    }

    private void updateFetcherApiKey(FetcherApiKey apiKey) {
        if (apiKey != null) {
            apiKey.setKey(customApiKey.getCellData(apiKey).trim());
        }
    }

    @FXML
    void checkCustomApiKey() {
        viewModel.checkCustomApiKey();
    }
}
