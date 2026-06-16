package org.jabref.gui.preferences.entryeditor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.entryeditor.DeprecatedFieldsTab;
import org.jabref.gui.entryeditor.DetailOptionalFieldsTab;
import org.jabref.gui.entryeditor.EntryEditorTabModel;
import org.jabref.gui.entryeditor.ImportantOptionalFieldsTab;
import org.jabref.gui.entryeditor.OtherFieldsTab;
import org.jabref.gui.entryeditor.RequiredFieldsTab;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.fetcher.citation.CitationCountFetcherType;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.gemsfx.TagsField;
import com.tobiasdiez.easybind.EasyBind;

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
    @FXML private Button addTabButton;
    @FXML private Button removeTabButton;
    @FXML private Button resetTabsButton;
    @FXML private Button generalFieldsHelp;

    @FXML private VBox tabEditorPanel;
    @FXML private Label tabNameLabel;
    @FXML private TextField tabNameField;
    @FXML private Label tabFieldsLabel;

    private boolean syncingFields = false;

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

        setupTabList();
        setupTabEditor();

        ActionFactory actionFactory = new ActionFactory();
        actionFactory.configureIconButton(
                StandardActions.HELP,
                new HelpAction(HelpFile.GENERAL_FIELDS, dialogService, preferences.getExternalApplicationsPreferences()),
                generalFieldsHelp);
    }

    private void setupTabList() {
        tabConfigsList.setItems(viewModel.getTabConfigs());
        tabConfigsList.setCellFactory(_ -> new TabConfigCell());

        // Sync list selection → viewModel
        EasyBind.subscribe(tabConfigsList.getSelectionModel().selectedItemProperty(), newItem -> {
            if (!Objects.equals(newItem, viewModel.selectedTabProperty().get())) {
                viewModel.selectedTabProperty().set(newItem);
            }
        });

        // Sync viewModel → list selection (e.g. after addFieldSetTab)
        EasyBind.subscribe(viewModel.selectedTabProperty(), newItem -> {
            if (!Objects.equals(newItem, tabConfigsList.getSelectionModel().getSelectedItem())) {
                tabConfigsList.getSelectionModel().select(newItem);
            }
        });

        removeTabButton.disableProperty().bind(viewModel.canRemoveSelectedTabProperty().not());
    }

    private void setupTabEditor() {
        // Right panel visibility tied to field-set selection
        tabNameLabel.visibleProperty().bind(viewModel.fieldSetTabSelectedProperty());
        tabNameLabel.managedProperty().bind(viewModel.fieldSetTabSelectedProperty());
        tabNameField.visibleProperty().bind(viewModel.fieldSetTabSelectedProperty());
        tabNameField.managedProperty().bind(viewModel.fieldSetTabSelectedProperty());
        tabFieldsLabel.visibleProperty().bind(viewModel.fieldSetTabSelectedProperty());
        tabFieldsLabel.managedProperty().bind(viewModel.fieldSetTabSelectedProperty());

        tabNameField.textProperty().bindBidirectional(viewModel.selectedTabNameProperty());

        TagsField<Field> tabFieldsTagsField = buildFieldTagsField();
        tabFieldsTagsField.visibleProperty().bind(viewModel.fieldSetTabSelectedProperty());
        tabFieldsTagsField.managedProperty().bind(viewModel.fieldSetTabSelectedProperty());
        VBox.setVgrow(tabFieldsTagsField, Priority.ALWAYS);
        tabEditorPanel.getChildren().add(tabFieldsTagsField);

        // Bidirectional sync: viewModel staging ↔ TagsField display.
        // JavaFX has no bindContentBidirectional for lists; use guarded listeners.
        viewModel.getSelectedTabFields().addListener((ListChangeListener<Field>) _ -> {
            if (!syncingFields) {
                syncingFields = true;
                tabFieldsTagsField.getTags().setAll(viewModel.getSelectedTabFields());
                syncingFields = false;
            }
        });
        tabFieldsTagsField.getTags().addListener((ListChangeListener<Field>) _ -> {
            if (!syncingFields) {
                syncingFields = true;
                viewModel.getSelectedTabFields().setAll(tabFieldsTagsField.getTags());
                syncingFields = false;
            }
        });
    }

    private TagsField<Field> buildFieldTagsField() {
        TagsField<Field> tagsField = new TagsField<>();

        StringConverter<Field> converter = new StringConverter<>() {
            @Override
            public String toString(Field field) {
                return field != null ? field.getName() : "";
            }

            @Override
            public Field fromString(String name) {
                return FieldFactory.parseField(name);
            }
        };

        List<Field> suggestions = viewModel.getAllKnownFields();

        tagsField.setConverter(converter);
        tagsField.setCellFactory(new ViewModelListCellFactory<Field>().withText(Field::getName));
        tagsField.setSuggestionProvider(request ->
                suggestions.stream()
                           .filter(f -> f.getName().toLowerCase().startsWith(
                                   request.getUserText().toLowerCase()))
                           .collect(Collectors.toCollection(ArrayList::new)));
        tagsField.setMatcher((field, searchText) ->
                field.getName().toLowerCase().startsWith(searchText.toLowerCase()));
        tagsField.setComparator(Comparator.comparing(Field::getName));
        tagsField.setNewItemProducer(converter::fromString);
        tagsField.setTagViewFactory(field -> buildFieldTag(field, tagsField));
        tagsField.setShowSearchIcon(false);

        return tagsField;
    }

    private Node buildFieldTag(Field field, TagsField<Field> tagsField) {
        Label label = new Label(field.getName());
        label.setGraphic(IconTheme.JabRefIcons.REMOVE_TAGS.getGraphicNode());
        label.setContentDisplay(ContentDisplay.RIGHT);
        label.getGraphic().setOnMouseClicked(_ -> tagsField.removeTags(field));
        return label;
    }

    private static String featureTabDisplayName(EntryEditorTabModel.StaticTab type) {
        return switch (type) {
            case RELATED_ARTICLES ->
                    Localization.lang("Related articles");
            case AI_SUMMARY ->
                    Localization.lang("AI Summary");
            case AI_CHAT ->
                    Localization.lang("AI Chat");
            case FILE_ANNOTATIONS ->
                    Localization.lang("File annotations");
            case LATEX_CITATIONS ->
                    Localization.lang("LaTeX citations");
            case CITATION_INFORMATION ->
                    Localization.lang("Citation information");
            case USER_COMMENTS ->
                    Localization.lang("User comments");
        };
    }

    /// Display name for a built-in {@link EntryEditorTabModel.FieldSet}, matched by its {@code NAME} constant.
    private static String builtInFieldSetDisplayName(String name) {
        return switch (name) {
            case RequiredFieldsTab.NAME -> Localization.lang("Required fields");
            case ImportantOptionalFieldsTab.NAME -> Localization.lang("Optional fields");
            case DetailOptionalFieldsTab.NAME -> Localization.lang("Optional fields 2");
            case DeprecatedFieldsTab.NAME -> Localization.lang("Deprecated fields");
            case OtherFieldsTab.NAME -> Localization.lang("Other fields");
            default -> name;
        };
    }

    @FXML
    void addFieldSetTab() {
        viewModel.addFieldSetTab();
    }

    @FXML
    void removeFieldSetTab() {
        viewModel.removeSelectedFieldSetTab();
    }

    @FXML
    void resetToDefaults() {
        viewModel.resetToDefaults();
    }

    // region Cell

    private class TabConfigCell extends ListCell<EntryEditorTabModel> {

        private final CheckBox checkBox = new CheckBox();
        private final Label nameLabel = new Label();
        private final HBox container = new HBox(6, checkBox, nameLabel);

        private boolean updatingCell = false;

        TabConfigCell() {
            checkBox.selectedProperty().addListener((_, _, selected) -> {
                if (updatingCell) {
                    return;
                }
                viewModel.toggleFeatureTabVisibility(getItem());
            });
        }

        @Override
        protected void updateItem(EntryEditorTabModel config, boolean empty) {
            super.updateItem(config, empty);
            if (empty || config == null) {
                setGraphic(null);
                return;
            }
            updatingCell = true;
            switch (config) {
                case EntryEditorTabModel.Feature feature -> {
                    checkBox.setVisible(true);
                    checkBox.setManaged(true);
                    checkBox.setSelected(feature.visible());
                    nameLabel.setText(featureTabDisplayName(feature.type()));
                }
                case EntryEditorTabModel.FieldSet fieldSet -> {
                    checkBox.setVisible(true);
                    checkBox.setManaged(true);
                    checkBox.setSelected(fieldSet.visible());
                    nameLabel.setText(builtInFieldSetDisplayName(fieldSet.name()));
                }
                case EntryEditorTabModel.CustomizedFieldSet customizedFieldSet -> {
                    checkBox.setVisible(false);
                    checkBox.setManaged(false);
                    nameLabel.setText(customizedFieldSet.name());
                }
            }
            updatingCell = false;
            setGraphic(container);
        }
    }

    // endregion
}
