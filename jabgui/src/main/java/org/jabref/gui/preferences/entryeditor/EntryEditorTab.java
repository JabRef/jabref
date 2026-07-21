package org.jabref.gui.preferences.entryeditor;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.jabref.gui.entryeditor.EntryEditorTabModel;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIconView;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.logic.importer.fetcher.citation.CitationCountFetcherType;
import org.jabref.logic.l10n.Localization;

public class EntryEditorTab extends AbstractPreferenceTabView<EntryEditorTabViewModel> {

    public EntryEditorTab() {
        this.viewModel = new EntryEditorTabViewModel(
                dialogService,
                preferences.getEntryEditorPreferences(),
                preferences.getMrDlibPreferences(),
                preferences.getAbbreviationPreferences(),
                taskExecutor);
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry editor");
    }

    private void buildView() {
        getChildren().add(form()

                .checkbox(Localization.lang("Open editor when a new entry is created"), viewModel.openOnNewEntryProperty())
                .checkbox(Localization.lang("Automatically search and show unlinked files in the entry editor"), viewModel.autoLinkFilesEnabledProperty())
                .checkbox(Localization.lang("Show validation messages"), viewModel.enableValidationProperty())
                .checkbox(Localization.lang("Allow integers in 'edition' field in BibTeX mode"), viewModel.allowIntegerEditionProperty())
                .checkbox(Localization.lang("Fetch journal information online to show"), viewModel.journalPopupProperty())
                .checkbox(Localization.lang("Enable MSC keyword descriptions"), viewModel.enableMscKeywordDescriptionsProperty())
                .checkbox(Localization.lang("Show BibTeX source by default"), viewModel.defaultSourceProperty())
                .checkbox(Localization.lang("Accept recommendations from Mr. DLib"), viewModel.acceptRecommendationsProperty())

                .comboItems(Localization.lang("Citation count fetcher:"),
                        FXCollections.observableList(List.of(CitationCountFetcherType.values())),
                        viewModel.citationCountFetcherTypeProperty(), CitationCountFetcherType::getName)

                .section(Localization.lang("Editor tabs"), tabs -> tabs
                        .custom(buildTabConfigRegion()))

                .build());
    }

    private Node buildTabConfigRegion() {
        ListView<EntryEditorTabModel> tabConfigsList = new ListView<>();
        VBox.setVgrow(tabConfigsList, Priority.ALWAYS);
        tabConfigsList.setItems(viewModel.getTabModels());
        tabConfigsList.setCellFactory(_ -> new TabConfigCell());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button resetTabsButton = new Button();
        resetTabsButton.setGraphic(new JabRefIconView(IconTheme.JabRefIcons.REFRESH));
        resetTabsButton.getStyleClass().add("icon-button");
        resetTabsButton.setTooltip(new Tooltip(Localization.lang("Reset to default tabs")));
        resetTabsButton.setOnAction(_ -> viewModel.resetToDefaults());
        HBox buttonRow = new HBox(5.0, spacer, resetTabsButton);

        VBox region = new VBox(5.0, tabConfigsList, buttonRow);
        VBox.setVgrow(region, Priority.ALWAYS);
        return region;
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
