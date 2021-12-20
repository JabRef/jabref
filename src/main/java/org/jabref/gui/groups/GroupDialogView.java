package org.jabref.gui.groups;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.ServiceLoader;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabrefIconProvider;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.search.rules.SearchRules.SearchFlags;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.textfield.CustomTextField;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.IkonProvider;
import org.kordamp.ikonli.javafx.FontIcon;

public class GroupDialogView extends BaseDialog<AbstractGroup> {

    // Basic Settings
    @FXML private TextField nameField;
    @FXML private TextField descriptionField;
    @FXML private TextField iconField;
    @FXML private Button iconPickerButton;
    @FXML private ColorPicker colorField;
    @FXML private ComboBox<GroupHierarchyType> hierarchicalContextCombo;

    // Type
    @FXML private RadioButton explicitRadioButton;
    @FXML private RadioButton keywordsRadioButton;
    @FXML private RadioButton searchRadioButton;
    @FXML private RadioButton autoRadioButton;
    @FXML private RadioButton texRadioButton;

    // Option Groups
    @FXML private TextField keywordGroupSearchTerm;
    @FXML private TextField keywordGroupSearchField;
    @FXML private CheckBox keywordGroupCaseSensitive;
    @FXML private CheckBox keywordGroupRegex;

    @FXML private TextField searchGroupSearchTerm;
    @FXML private CheckBox searchGroupCaseSensitive;
    @FXML private CheckBox searchGroupRegex;

    @FXML private RadioButton autoGroupKeywordsOption;
    @FXML private TextField autoGroupKeywordsField;
    @FXML private TextField autoGroupKeywordsDeliminator;
    @FXML private TextField autoGroupKeywordsHierarchicalDeliminator;
    @FXML private RadioButton autoGroupPersonsOption;
    @FXML private TextField autoGroupPersonsField;

    @FXML private TextField texGroupFilePath;

    private final EnumMap<GroupHierarchyType, String> hierarchyText = new EnumMap<>(GroupHierarchyType.class);
    private final EnumMap<GroupHierarchyType, String> hierarchyToolTip = new EnumMap<>(GroupHierarchyType.class);

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();
    private final GroupDialogViewModel viewModel;

    public GroupDialogView(DialogService dialogService,
                           BibDatabaseContext currentDatabase,
                           PreferencesService preferencesService,
                           AbstractGroup editedGroup,
                           GroupDialogHeader groupDialogHeader) {
        viewModel = new GroupDialogViewModel(dialogService, currentDatabase, preferencesService, editedGroup, groupDialogHeader);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        if (editedGroup == null) {
            if (groupDialogHeader == GroupDialogHeader.GROUP) {
                this.setTitle(Localization.lang("Add group"));
            } else if (groupDialogHeader == GroupDialogHeader.SUBGROUP) {
                this.setTitle(Localization.lang("Add subgroup"));
            }
        } else {
            this.setTitle(Localization.lang("Edit group") + " " + editedGroup.getName());
        }

        setResultConverter(viewModel::resultConverter);
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        final Button confirmDialogButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        confirmDialogButton.disableProperty().bind(viewModel.validationStatus().validProperty().not());
        // handle validation before closing dialog and calling resultConverter
        confirmDialogButton.addEventFilter(ActionEvent.ACTION, viewModel::validationHandler);
    }

    @FXML
    public void initialize() {
        hierarchyText.put(GroupHierarchyType.INCLUDING, Localization.lang("Union"));
        hierarchyToolTip.put(GroupHierarchyType.INCLUDING, Localization.lang("Include subgroups: When selected, view entries contained in this group or its subgroups"));
        hierarchyText.put(GroupHierarchyType.REFINING, Localization.lang("Intersection"));
        hierarchyToolTip.put(GroupHierarchyType.REFINING, Localization.lang("Refine supergroup: When selected, view entries contained in both this group and its supergroup"));
        hierarchyText.put(GroupHierarchyType.INDEPENDENT, Localization.lang("Independent"));
        hierarchyToolTip.put(GroupHierarchyType.INDEPENDENT, Localization.lang("Independent group: When selected, view only this group's entries"));

        nameField.textProperty().bindBidirectional(viewModel.nameProperty());
        descriptionField.textProperty().bindBidirectional(viewModel.descriptionProperty());
        iconField.textProperty().bindBidirectional(viewModel.iconProperty());
        colorField.valueProperty().bindBidirectional(viewModel.colorFieldProperty());
        hierarchicalContextCombo.itemsProperty().bind(viewModel.groupHierarchyListProperty());
        new ViewModelListCellFactory<GroupHierarchyType>()
                .withText(hierarchyText::get)
                .withStringTooltip(hierarchyToolTip::get)
                .install(hierarchicalContextCombo);
        hierarchicalContextCombo.valueProperty().bindBidirectional(viewModel.groupHierarchySelectedProperty());

        explicitRadioButton.selectedProperty().bindBidirectional(viewModel.typeExplicitProperty());
        keywordsRadioButton.selectedProperty().bindBidirectional(viewModel.typeKeywordsProperty());
        searchRadioButton.selectedProperty().bindBidirectional(viewModel.typeSearchProperty());
        autoRadioButton.selectedProperty().bindBidirectional(viewModel.typeAutoProperty());
        texRadioButton.selectedProperty().bindBidirectional(viewModel.typeTexProperty());

        keywordGroupSearchTerm.textProperty().bindBidirectional(viewModel.keywordGroupSearchTermProperty());
        keywordGroupSearchField.textProperty().bindBidirectional(viewModel.keywordGroupSearchFieldProperty());
        keywordGroupCaseSensitive.selectedProperty().bindBidirectional(viewModel.keywordGroupCaseSensitiveProperty());
        keywordGroupRegex.selectedProperty().bindBidirectional(viewModel.keywordGroupRegexProperty());

        searchGroupSearchTerm.textProperty().bindBidirectional(viewModel.searchGroupSearchTermProperty());
        searchGroupCaseSensitive.selectedProperty().addListener((observable, oldValue, newValue) -> {
            EnumSet<SearchFlags> searchFlags = viewModel.searchFlagsProperty().get();
            if (newValue) {
                searchFlags.add(SearchRules.SearchFlags.CASE_SENSITIVE);
            } else {
                searchFlags.remove(SearchRules.SearchFlags.CASE_SENSITIVE);
            }
            viewModel.searchFlagsProperty().set(searchFlags);
        });
        searchGroupRegex.selectedProperty().addListener((observable, oldValue, newValue) -> {
            EnumSet<SearchFlags> searchFlags = viewModel.searchFlagsProperty().get();
            if (newValue) {
                searchFlags.add(SearchRules.SearchFlags.REGULAR_EXPRESSION);
            } else {
                searchFlags.remove(SearchRules.SearchFlags.REGULAR_EXPRESSION);
            }
            viewModel.searchFlagsProperty().set(searchFlags);
        });

        autoGroupKeywordsOption.selectedProperty().bindBidirectional(viewModel.autoGroupKeywordsOptionProperty());
        autoGroupKeywordsField.textProperty().bindBidirectional(viewModel.autoGroupKeywordsFieldProperty());
        autoGroupKeywordsDeliminator.textProperty().bindBidirectional(viewModel.autoGroupKeywordsDeliminatorProperty());
        autoGroupKeywordsHierarchicalDeliminator.textProperty().bindBidirectional(viewModel.autoGroupKeywordsHierarchicalDeliminatorProperty());
        autoGroupPersonsOption.selectedProperty().bindBidirectional(viewModel.autoGroupPersonsOptionProperty());
        autoGroupPersonsField.textProperty().bindBidirectional(viewModel.autoGroupPersonsFieldProperty());

        texGroupFilePath.textProperty().bindBidirectional(viewModel.texGroupFilePathProperty());

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> {
            validationVisualizer.initVisualization(viewModel.nameValidationStatus(), nameField);
            validationVisualizer.initVisualization(viewModel.nameContainsDelimiterValidationStatus(), nameField, false);
            validationVisualizer.initVisualization(viewModel.sameNameValidationStatus(), nameField);
            validationVisualizer.initVisualization(viewModel.searchRegexValidationStatus(), searchGroupSearchTerm);
            validationVisualizer.initVisualization(viewModel.searchSearchTermEmptyValidationStatus(), searchGroupSearchTerm);
            validationVisualizer.initVisualization(viewModel.keywordRegexValidationStatus(), keywordGroupSearchTerm);
            validationVisualizer.initVisualization(viewModel.keywordSearchTermEmptyValidationStatus(), keywordGroupSearchTerm);
            validationVisualizer.initVisualization(viewModel.keywordFieldEmptyValidationStatus(), keywordGroupSearchField);
            validationVisualizer.initVisualization(viewModel.texGroupFilePathValidatonStatus(), texGroupFilePath);
            nameField.requestFocus();
        });
    }

    @FXML
    private void texGroupBrowse() {
        viewModel.texGroupBrowse();
    }

    @FXML
    private void openHelp() {
        viewModel.openHelpPage();
    }

    @FXML
    private void openIconPicker() {
        ObservableList<Ikon> ikonList = FXCollections.observableArrayList();
        FilteredList<Ikon> filteredList = new FilteredList<>(ikonList);

        for (IkonProvider provider : ServiceLoader.load(IkonProvider.class.getModule().getLayer(), IkonProvider.class)) {
            if (provider.getClass() != JabrefIconProvider.class) {
                ikonList.addAll(EnumSet.allOf(provider.getIkon()));
            }
        }

        CustomTextField searchBox = new CustomTextField();
        searchBox.setPromptText(Localization.lang("Search") + "...");
        searchBox.setLeft(IconTheme.JabRefIcons.SEARCH.getGraphicNode());
        searchBox.textProperty().addListener((obs, oldValue, newValue) ->
                filteredList.setPredicate(ikon -> newValue.isEmpty() || ikon.getDescription().toLowerCase()
                                                                            .contains(newValue.toLowerCase())));

        GridView<Ikon> ikonGridView = new GridView<>(FXCollections.observableArrayList());
        ikonGridView.setCellFactory(gridView -> new IkonliCell());
        ikonGridView.setPrefWidth(520);
        ikonGridView.setPrefHeight(400);
        ikonGridView.setHorizontalCellSpacing(4);
        ikonGridView.setVerticalCellSpacing(4);

        VBox vBox = new VBox(10, searchBox, ikonGridView);
        vBox.setPadding(new Insets(10));

        // Necessary because of a bug in controlsfx GridView
        // https://github.com/controlsfx/controlsfx/issues/1400
        Platform.runLater(() -> {
            ikonGridView.setItems(filteredList);
        });

        PopOver popOver = new PopOver(vBox);
        popOver.setDetachable(false);
        popOver.setArrowSize(0);
        popOver.setCornerRadius(0);
        popOver.setTitle("Icon picker");
        popOver.show(iconPickerButton);

    }

    public class IkonliCell extends GridCell<Ikon> {
        @Override
        protected void updateItem(Ikon ikon, boolean empty) {
            super.updateItem(ikon, empty);
            if (empty || (ikon == null)) {
                setText(null);
                setGraphic(null);
            } else {
                FontIcon fontIcon = FontIcon.of(ikon);
                fontIcon.getStyleClass().setAll("font-icon");
                fontIcon.setIconSize(22);
                setGraphic(fontIcon);
                setAlignment(Pos.BASELINE_CENTER);
                setPadding(new Insets(1));
                setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));

                setOnMouseClicked(event -> {
                    iconField.textProperty().setValue(String.valueOf(fontIcon.getIconCode()));
                    PopOver stage = (PopOver) this.getGridView().getParent().getScene().getWindow();
                    stage.hide();
                });
            }
        }
    }
}
