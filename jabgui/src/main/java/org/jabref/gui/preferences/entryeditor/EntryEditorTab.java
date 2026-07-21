package org.jabref.gui.preferences.entryeditor;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;

import org.jabref.gui.entryeditor.EntryEditorTabModel;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.importer.fetcher.citation.CitationCountFetcherType;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class EntryEditorTab extends AbstractPreferenceTabView<EntryEditorTabViewModel> implements PreferencesTab {

    @FXML private CheckBox openOnNewEntry;
    @FXML private CheckBox defaultSource;
    @FXML private CheckBox acceptRecommendations;
    @FXML private CheckBox enableValidation;
    @FXML private CheckBox allowIntegerEdition;
    @FXML private CheckBox journalPopupEnabled;
    @FXML private CheckBox autoLinkFilesEnabled;
    @FXML private CheckBox enableMscKeywordDescriptions;
    @FXML private ComboBox<CitationCountFetcherType> citationCountFetcherCombo;

    @FXML private ListView<EntryEditorTabModel> tabConfigsList;
    @FXML private Button resetTabsButton;

    public EntryEditorTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry editor");
    }

    public void initialize() {
        this.viewModel = new EntryEditorTabViewModel(dialogService, preferences, taskExecutor);

        openOnNewEntry.selectedProperty().bindBidirectional(viewModel.openOnNewEntryProperty());
        defaultSource.selectedProperty().bindBidirectional(viewModel.defaultSourceProperty());
        acceptRecommendations.selectedProperty().bindBidirectional(viewModel.acceptRecommendationsProperty());
        enableValidation.selectedProperty().bindBidirectional(viewModel.enableValidationProperty());
        allowIntegerEdition.selectedProperty().bindBidirectional(viewModel.allowIntegerEditionProperty());
        journalPopupEnabled.selectedProperty().bindBidirectional(viewModel.journalPopupProperty());
        autoLinkFilesEnabled.selectedProperty().bindBidirectional(viewModel.autoLinkFilesEnabledProperty());
        enableMscKeywordDescriptions.selectedProperty().bindBidirectional(viewModel.enableMscKeywordDescriptionsProperty());

        citationCountFetcherCombo.setItems(FXCollections.observableList(List.of(CitationCountFetcherType.values())));
        new ViewModelListCellFactory<CitationCountFetcherType>()
                .withText(CitationCountFetcherType::getName)
                .install(citationCountFetcherCombo);
        citationCountFetcherCombo.valueProperty().bindBidirectional(viewModel.citationCountFetcherTypeProperty());

        tabConfigsList.setItems(viewModel.getTabModels());
        tabConfigsList.setCellFactory(_ -> new TabConfigCell());
    }

    @FXML
    void resetToDefaults() {
        viewModel.resetToDefaults();
    }

    // region Cell

    /// A tab with its visibility checkbox.
    private class TabConfigCell extends ListCell<EntryEditorTabModel> {

        private final CheckBox checkBox = new CheckBox();
        private final Label nameLabel = new Label();
        private final HBox container = new HBox(checkBox, nameLabel);

        private boolean updatingCell = false;

        TabConfigCell() {
            container.getStyleClass().add("entry-editor-tab-cell");
            checkBox.selectedProperty().addListener((_, _, selected) -> {
                if (updatingCell) {
                    return;
                }
                viewModel.toggleTabVisibility(getItem());
            });
        }

        @Override
        protected void updateItem(EntryEditorTabModel config, boolean empty) {
            super.updateItem(config, empty);
            if (empty || (config == null)) {
                setGraphic(null);
                return;
            }
            updatingCell = true;
            if (config instanceof EntryEditorTabModel.BuiltInTab builtIn) {
                checkBox.setSelected(builtIn.visible());
                nameLabel.setText(builtIn.type().displayName());
            }
            updatingCell = false;
            setGraphic(container);
        }
    }

    // endregion
}
