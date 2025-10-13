package org.jabref.gui.preferences.websearch;

import java.util.List;
import java.util.Optional;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.gui.util.component.HelpButton;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

import com.airhacks.afterburner.views.ViewLoader;

public class WebSearchTab extends AbstractPreferenceTabView<WebSearchTabViewModel> implements PreferencesTab {
    @FXML private CheckBox enableWebSearch;
    @FXML private CheckBox warnAboutDuplicatesOnImport;
    @FXML private CheckBox downloadLinkedOnlineFiles;
    @FXML private CheckBox keepDownloadUrl;
    @FXML private CheckBox addImportedEntries;
    @FXML private TextField addImportedEntriesGroupName;
    @FXML private ComboBox<PlainCitationParserChoice> defaultPlainCitationParser;
    @FXML private TextField citationsRelationStoreTTL;

    @FXML private CheckBox useCustomDOI;
    @FXML private TextField useCustomDOIName;

    @FXML private CheckBox grobidEnabled;
    @FXML private TextField grobidURL;

    @FXML private CheckBox preferInspireTexkeys;

    @FXML private VBox fetchersContainer;

    private final ReadOnlyBooleanProperty refAiEnabled;

    public WebSearchTab(ReadOnlyBooleanProperty refAiEnabled) {
        this.refAiEnabled = refAiEnabled;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public List<String> getSearchKeywords() {
        return List.of(
                getTabName(),
                Localization.lang("Configure API key"),
                Localization.lang("Custom API key"),
                "api",
                "api key",
                "apikey"
        );
    }

    @Override
    public String getTabName() {
        return Localization.lang("Web search");
    }

    public void initialize() {
        this.viewModel = new WebSearchTabViewModel(preferences, refAiEnabled, taskExecutor);

        enableWebSearch.selectedProperty().bindBidirectional(viewModel.enableWebSearchProperty());
        warnAboutDuplicatesOnImport.selectedProperty().bindBidirectional(viewModel.warnAboutDuplicatesOnImportProperty());
        downloadLinkedOnlineFiles.selectedProperty().bindBidirectional(viewModel.shouldDownloadLinkedOnlineFiles());
        keepDownloadUrl.selectedProperty().bindBidirectional(viewModel.shouldKeepDownloadUrl());

        addImportedEntries.selectedProperty().bindBidirectional(viewModel.getAddImportedEntries());
        addImportedEntriesGroupName.textProperty().bindBidirectional(viewModel.getAddImportedEntriesGroupName());
        addImportedEntriesGroupName.disableProperty().bind(addImportedEntries.selectedProperty().not());

        new ViewModelListCellFactory<PlainCitationParserChoice>()
                .withText(PlainCitationParserChoice::getLocalizedName)
                .install(defaultPlainCitationParser);
        defaultPlainCitationParser.itemsProperty().bind(viewModel.plainCitationParsers());
        defaultPlainCitationParser.valueProperty().bindBidirectional(viewModel.defaultPlainCitationParserProperty());

        viewModel.citationsRelationsStoreTTLProperty()
                 .addListener((_, _, newValue) -> {
                     if (newValue != null && !newValue.toString().equals(citationsRelationStoreTTL.getText())) {
                         citationsRelationStoreTTL.setText(newValue.toString());
                     }
                 });
        citationsRelationStoreTTL
                .textProperty()
                .addListener((_, _, newValue) -> {
                    if (StringUtil.isBlank(newValue)) {
                        return;
                    }
                    if (!newValue.matches("\\d*")) {
                        citationsRelationStoreTTL.setText(newValue.replaceAll("\\D", ""));
                        return;
                    }
                    viewModel.citationsRelationsStoreTTLProperty().set(Integer.parseInt(newValue));
                });

        grobidEnabled.selectedProperty().bindBidirectional(viewModel.grobidEnabledProperty());
        grobidURL.textProperty().bindBidirectional(viewModel.grobidURLProperty());
        grobidURL.disableProperty().bind(grobidEnabled.selectedProperty().not());

        useCustomDOI.selectedProperty().bindBidirectional(viewModel.useCustomDOIProperty());
        useCustomDOIName.textProperty().bindBidirectional(viewModel.useCustomDOINameProperty());
        useCustomDOIName.disableProperty().bind(useCustomDOI.selectedProperty().not());

        preferInspireTexkeys.selectedProperty().bindBidirectional(viewModel.preferInspireTexkeysProperty());

        InvalidationListener listener = _ -> fetchersContainer
                .getChildren()
                .setAll(viewModel.getFetchers()
                                 .stream()
                                 .map(this::createFetcherNode).toList());
        viewModel.getFetchers().addListener(listener);
    }

    private Node createFetcherNode(WebSearchTabViewModel.FetcherViewModel item) {
        HBox container = new HBox();
        container.getStyleClass().add("fetcher-list-cell");
        container.setAlignment(Pos.CENTER_LEFT);

        CheckBox enabledCheckBox = new CheckBox();
        enabledCheckBox.selectedProperty().bindBidirectional(item.enabledProperty());

        Label nameLabel = new Label(item.getName());
        nameLabel.getStyleClass().add("fetcher-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HelpButton helpButton = new HelpButton();
        Optional<HelpFile> helpFile = item.getFetcher().getHelpPage();
        if (helpFile.isPresent() && !helpFile.get().getPageName().isEmpty()) {
            helpButton.setHelpFile(helpFile.get(), dialogService, preferences.getExternalApplicationsPreferences());
            helpButton.setVisible(true);
        } else {
            helpButton.setVisible(false);
        }

        Button configureButton = new Button(Localization.lang("Configure API key"));
        configureButton.getStyleClass().add("configure-button");
        configureButton.setOnAction(_ -> showApiKeyDialog(item));
        configureButton.setVisible(item.isCustomizable());

        container.getChildren().addAll(enabledCheckBox, nameLabel, spacer, helpButton, configureButton);
        return container;
    }

    private void showApiKeyDialog(WebSearchTabViewModel.FetcherViewModel fetcherViewModel) {
        dialogService.showCustomDialogAndWait(new ApiKeyDialog(viewModel, fetcherViewModel));
    }
}
