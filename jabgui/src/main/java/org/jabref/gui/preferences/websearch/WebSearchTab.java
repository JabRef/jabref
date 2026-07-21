package org.jabref.gui.preferences.websearch;

import java.util.List;
import java.util.Optional;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.util.component.HelpButton;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

public class WebSearchTab extends AbstractPreferenceTabView<WebSearchTabViewModel> {

    // Multiplier for row height based on font size
    private static final double FONT_HEIGHT_MULTIPLIER = 2.5;

    // Default row height if font is not available
    private static final double DEFAULT_ROW_HEIGHT = 30.0;

    // Estimate for header height (used in table prefHeight calculation)
    private static final double HEADER_HEIGHT_ESTIMATE = 1.1;

    private final VBox fetchersContainer = new VBox();

    /// Also the source of the table's row height: it is a themed control in the tree, so its font
    /// tracks the configured font size.
    private final Label tableNote = new Label(Localization.lang("( Note: Press return to commit changes in the table! )"));

    /// @param workingAiPreferences the dialog-scoped working copy edited by the AI tab; this tab
    ///                             observes its master switch to offer or hide the LLM citation parser
    public WebSearchTab(AiPreferences workingAiPreferences) {
        this.viewModel = new WebSearchTabViewModel(
                preferences.getImporterPreferences(),
                preferences.getGrobidPreferences(),
                preferences.getDOIPreferences(),
                preferences.getFilePreferences(),
                preferences.getImportFormatPreferences(),
                preferences.getLibraryPreferences(),
                workingAiPreferences.aiFeaturesEnabledCurrentlyProperty(),
                taskExecutor);
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Web search");
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

    private void buildView() {
        getChildren().add(form()

                .section(Localization.lang("General"), general -> general
                        .flow(toggles -> toggles
                                .checkbox(Localization.lang("Enable web search"), viewModel.enableWebSearchProperty())
                                .checkbox(Localization.lang("Warn about duplicates on import"), viewModel.warnAboutDuplicatesOnImportProperty())
                                .checkbox(Localization.lang("Download referenced files (PDFs, ...)"), viewModel.shouldDownloadLinkedOnlineFiles())
                                .checkbox(Localization.lang("Store url for downloaded file"), viewModel.shouldKeepDownloadUrl()),
                            toggleRow -> toggleRow.styleClass("checkbox-flowpane"))
                        .checkWithField(Localization.lang("Add imported entries to group"), viewModel.getAddImportedEntries(), viewModel.getAddImportedEntriesGroupName(),
                                groupName -> groupName.grow())
                        .combo(Localization.lang("Default plain citation parser"), viewModel.plainCitationParsers(), viewModel.defaultPlainCitationParserProperty(), PlainCitationParserChoice::getLocalizedName)
                        .field(Localization.lang("Citations relations local storage time-to-live (in days)"), buildStoreTtlField()))

                .section(Localization.lang("Custom DOI URI"), doi -> doi
                        .checkWithField(Localization.lang("Use custom DOI base URI for article access"), viewModel.useCustomDOIProperty(), viewModel.useCustomDOINameProperty(),
                                baseUri -> baseUri.grow()))

                .section(Localization.lang("Remote services"), remote -> remote
                        .checkbox(Localization.lang("Allow sending PDF files and raw citation strings to a JabRef online service (Grobid) to determine Metadata. This produces better results."), viewModel.grobidEnabledProperty())
                        .stringField(Localization.lang("Grobid URL"), viewModel.grobidURLProperty(),
                                url -> url.disableWhen(viewModel.grobidEnabledProperty().not())))

                .section(Localization.lang("Search Engine URL Templates"), searchEngines -> searchEngines
                        .custom(tableNote)
                        .custom(buildSearchEngineTable()))

                .section(Localization.lang("Pre-selected fetchers"), fetchers -> fetchers
                        .custom(fetchersContainer))

                .build());

        // The fetcher list is filled in setValues(), i.e. after this view exists.
        InvalidationListener listener = _ -> fetchersContainer
                .getChildren()
                .setAll(viewModel.getFetchers()
                                 .stream()
                                 .map(this::createFetcherNode)
                                 .toList());
        viewModel.getFetchers().addListener(listener);
    }

    /// Whole-day counts only: non-digits are stripped as they are typed, so the field never holds a
    /// value the view model cannot parse.
    private TextField buildStoreTtlField() {
        TextField field = new TextField();
        field.setPrefWidth(60.0);
        field.setMaxWidth(60.0);

        viewModel.citationsRelationsStoreTTLProperty().addListener((_, _, newValue) -> {
            if (newValue != null && !newValue.toString().equals(field.getText())) {
                field.setText(newValue.toString());
            }
        });
        field.textProperty().addListener((_, _, newValue) -> {
            if (StringUtil.isBlank(newValue)) {
                return;
            }
            if (!newValue.matches("\\d*")) {
                field.setText(newValue.replaceAll("\\D", ""));
                return;
            }
            viewModel.citationsRelationsStoreTTLProperty().set(Integer.parseInt(newValue));
        });
        return field;
    }

    private Node buildSearchEngineTable() {
        TableView<SearchEngineItem> table = new TableView<>();
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(viewModel.getSearchEngines());

        TableColumn<SearchEngineItem, String> name = new TableColumn<>(Localization.lang("Search Engine"));
        name.setMinWidth(120.0);
        name.setEditable(false);
        name.setCellValueFactory(param -> param.getValue().nameProperty());
        name.setCellFactory(TextFieldTableCell.forTableColumn());

        TableColumn<SearchEngineItem, String> urlTemplate = new TableColumn<>(Localization.lang("URL Template"));
        urlTemplate.setMinWidth(300.0);
        urlTemplate.setEditable(true);
        urlTemplate.setCellValueFactory(param -> param.getValue().urlTemplateProperty());
        urlTemplate.setCellFactory(TextFieldTableCell.forTableColumn());

        table.getColumns().add(name);
        table.getColumns().add(urlTemplate);

        // Size the table to its content so it never scrolls inside the already scrolling dialog.
        DoubleBinding rowHeight = Bindings.createDoubleBinding(
                () -> tableNote.getFont() != null ? tableNote.getFont().getSize() * FONT_HEIGHT_MULTIPLIER : DEFAULT_ROW_HEIGHT,
                tableNote.fontProperty());
        table.fixedCellSizeProperty().bind(rowHeight);
        table.prefHeightProperty().bind(
                Bindings.size(table.getItems())
                        .add(HEADER_HEIGHT_ESTIMATE)
                        .multiply(rowHeight));
        return table;
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
