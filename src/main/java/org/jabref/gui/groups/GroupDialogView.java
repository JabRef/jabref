package org.jabref.gui.groups;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
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
import javafx.scene.control.ButtonBar;
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
import org.jabref.gui.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabrefIconProvider;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.search.rules.SearchRules.SearchFlags;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import jakarta.inject.Inject;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.textfield.CustomTextField;
import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.IkonProvider;
import org.kordamp.ikonli.javafx.FontIcon;

public class GroupDialogView extends BaseDialog<AbstractGroup> {

    private static boolean useAutoColoring = false;

    // Basic Settings
    @FXML private TextField nameField;
    @FXML private TextField descriptionField;
    @FXML private TextField iconField;
    @FXML private Button iconPickerButton;
    @FXML private CheckBox colorUseCheckbox;
    @FXML private ColorPicker colorField;
    @FXML private ComboBox<GroupHierarchyType> hierarchicalContextCombo;
    @FXML private CheckBox autoColorCheckbox;

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

    private final BibDatabaseContext currentDatabase;
    private final @Nullable GroupTreeNode parentNode;
    private final @Nullable AbstractGroup editedGroup;

    private GroupDialogViewModel viewModel;

    @Inject private FileUpdateMonitor fileUpdateMonitor;
    @Inject private DialogService dialogService;
    @Inject private PreferencesService preferencesService;

    public GroupDialogView(BibDatabaseContext currentDatabase,
                           @Nullable GroupTreeNode parentNode,
                           @Nullable AbstractGroup editedGroup,
                           GroupDialogHeader groupDialogHeader) {
        this.currentDatabase = currentDatabase;
        this.parentNode = parentNode;
        this.editedGroup = editedGroup;

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

        ButtonType helpButtonType = new ButtonType("", ButtonBar.ButtonData.HELP_2);
        getDialogPane().getButtonTypes().setAll(helpButtonType, ButtonType.OK, ButtonType.CANCEL);

        final Button confirmDialogButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        final Button helpButton = (Button) getDialogPane().lookupButton(helpButtonType);

        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        HelpAction helpAction = new HelpAction(HelpFile.GROUPS, dialogService, preferencesService.getFilePreferences());
        actionFactory.configureIconButton(
                StandardActions.HELP_GROUPS,
                helpAction,
                helpButton);

        // Consume the dialog close event, but execute the help action
        helpButton.addEventFilter(ActionEvent.ACTION, event -> {
            helpAction.execute();
            event.consume();
        });

        confirmDialogButton.disableProperty().bind(viewModel.validationStatus().validProperty().not());
        // handle validation before closing dialog and calling resultConverter
        confirmDialogButton.addEventFilter(ActionEvent.ACTION, viewModel::validationHandler);
    }

    private @Nullable AbstractGroup parentGroup() {
        if (parentNode == null) {
            return null;
        } else {
            return parentNode.getGroup();
        }
    }

    @FXML
    public void initialize() {
        viewModel = new GroupDialogViewModel(dialogService, currentDatabase, preferencesService, editedGroup, parentNode, fileUpdateMonitor);

        setResultConverter(viewModel::resultConverter);

        hierarchyText.put(GroupHierarchyType.INCLUDING, Localization.lang("Union"));
        hierarchyToolTip.put(GroupHierarchyType.INCLUDING, Localization.lang("Include subgroups: When selected, view entries contained in this group or its subgroups"));
        hierarchyText.put(GroupHierarchyType.REFINING, Localization.lang("Intersection"));
        hierarchyToolTip.put(GroupHierarchyType.REFINING, Localization.lang("Refine supergroup: When selected, view entries contained in both this group and its supergroup"));
        hierarchyText.put(GroupHierarchyType.INDEPENDENT, Localization.lang("Independent"));
        hierarchyToolTip.put(GroupHierarchyType.INDEPENDENT, Localization.lang("Independent group: When selected, view only this group's entries"));

        nameField.textProperty().bindBidirectional(viewModel.nameProperty());
        descriptionField.textProperty().bindBidirectional(viewModel.descriptionProperty());
        iconField.textProperty().bindBidirectional(viewModel.iconProperty());
        colorUseCheckbox.selectedProperty().bindBidirectional(viewModel.colorUseProperty());
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
        searchGroupCaseSensitive.setSelected(viewModel.searchFlagsProperty().getValue().contains(SearchFlags.CASE_SENSITIVE));
        searchGroupCaseSensitive.selectedProperty().addListener((observable, oldValue, newValue) -> {
            EnumSet<SearchFlags> searchFlags = viewModel.searchFlagsProperty().get();
            if (newValue) {
                searchFlags.add(SearchRules.SearchFlags.CASE_SENSITIVE);
            } else {
                searchFlags.remove(SearchRules.SearchFlags.CASE_SENSITIVE);
            }
            viewModel.searchFlagsProperty().set(searchFlags);
        });
        searchGroupRegex.setSelected(viewModel.searchFlagsProperty().getValue().contains(SearchFlags.REGULAR_EXPRESSION));
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

        autoColorCheckbox.setSelected(useAutoColoring);
        autoColorCheckbox.setOnAction(event -> {
            useAutoColoring = autoColorCheckbox.isSelected();
            if (!autoColorCheckbox.isSelected()) {
                return;
            }
            if (parentNode == null) {
                viewModel.colorFieldProperty().setValue(IconTheme.getDefaultGroupColor());
                return;
            }
            List<Color> colorsOfSiblings = parentNode.getChildren().stream().map(child -> child.getGroup().getColor())
                                                     .flatMap(Optional::stream)
                                                     .toList();
            Optional<Color> parentColor = parentGroup().getColor();
            Color color;
            color = parentColor.map(value -> GroupColorPicker.generateColor(colorsOfSiblings, value)).orElseGet(() -> GroupColorPicker.generateColor(colorsOfSiblings));
            viewModel.colorFieldProperty().setValue(color);
        });
    }

    @FXML
    private void texGroupBrowse() {
        viewModel.texGroupBrowse();
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
        // The issue is closed, but still appears here
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
