package org.jabref.gui.groups;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.TextFlow;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.search.rules.describer.SearchDescribers;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class GroupDialogView extends BaseDialog<AbstractGroup> {

    // Basic Settings
    @FXML private TextField nameField;
    @FXML private TextField descriptionField;
    @FXML private TextField iconField;
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

    // Description text
    @FXML private TextFlow hintTextFlow;

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();
    private final GroupDialogViewModel viewModel;

    private final boolean showHints;

    public GroupDialogView(DialogService dialogService, BasePanel basePanel, JabRefPreferences prefs, AbstractGroup editedGroup) {
        viewModel = new GroupDialogViewModel(dialogService, basePanel, prefs, editedGroup);
        this.showHints = prefs.getBoolean(JabRefPreferences.SHOW_ADVANCED_HINTS);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        if (editedGroup == null) {
            this.setTitle(Localization.lang("Add subgroup"));
        } else {
            this.setTitle(Localization.lang("Edit group"));
        }

        if (showHints) {
            hintTextFlow.setPrefHeight(0d);
        }
        hintTextFlow.setVisible(showHints);

        setResultConverter(viewModel::resultConverter);

        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        getDialogPane().getScene().getWindow().sizeToScene();
    }

    public GroupDialogView(DialogService dialogService, AbstractGroup editedGroup) {
        this(dialogService, JabRefGUI.getMainFrame().getCurrentBasePanel(), Globals.prefs, editedGroup);
    }

    public GroupDialogView(DialogService dialogService) {
        this(dialogService, JabRefGUI.getMainFrame().getCurrentBasePanel(), Globals.prefs, null);
    }

    private String getHierarchyDisplayText(GroupHierarchyType type) {
        switch (type) {
            case INCLUDING:
                return Localization.lang("Union");
            case REFINING:
                return Localization.lang("Intersection");
            default:
            case INDEPENDENT:
                return Localization.lang("Independent");
        }
    }

    private String getHierarchyToolTip(GroupHierarchyType type) {
        switch (type) {
            case INCLUDING:
                return Localization.lang("Include subgroups: When selected, view entries contained in this group or its subgroups");
            case REFINING:
                return Localization.lang("Refine supergroup: When selected, view entries contained in both this group and its supergroup");
            default:
            case INDEPENDENT:
                return Localization.lang("Independent group: When selected, view only this group's entries");
        }
    }

    @FXML
    public void initialize() {
        nameField.textProperty().bindBidirectional(viewModel.nameProperty());
        descriptionField.textProperty().bindBidirectional(viewModel.descriptionProperty());
        iconField.textProperty().bindBidirectional(viewModel.iconProperty());
        colorField.valueProperty().bindBidirectional(viewModel.colorFieldProperty());
        hierarchicalContextCombo.itemsProperty().bind(viewModel.groupHierarchyListProperty());
        new ViewModelListCellFactory<GroupHierarchyType>()
                .withText(this::getHierarchyDisplayText)
                .withStringTooltip(this::getHierarchyToolTip)
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
        searchGroupCaseSensitive.selectedProperty().bindBidirectional(viewModel.searchGroupCaseSensitiveProperty());
        searchGroupRegex.selectedProperty().bindBidirectional(viewModel.searchGroupRegexProperty());

        autoGroupKeywordsOption.selectedProperty().bindBidirectional(viewModel.autoGroupKeywordsOptionProperty());
        autoGroupKeywordsField.textProperty().bindBidirectional(viewModel.autoGroupKeywordsFieldProperty());
        autoGroupKeywordsDeliminator.textProperty().bindBidirectional(viewModel.autoGroupKeywordsDeliminatorProperty());
        autoGroupKeywordsHierarchicalDeliminator.textProperty().bindBidirectional(viewModel.autoGroupKeywordsHierarchicalDeliminatorProperty());
        autoGroupPersonsOption.selectedProperty().bindBidirectional(viewModel.autoGroupPersonsOptionProperty());
        autoGroupPersonsField.textProperty().bindBidirectional(viewModel.autoGroupPersonsFieldProperty());

        texGroupFilePath.textProperty().bindBidirectional(viewModel.texGroupFilePathProperty());

        hintTextFlow.accessibleTextProperty().bindBidirectional(viewModel.hintTextProperty());

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> {
            validationVisualizer.initVisualization(viewModel.nameValidationStatus(), nameField);
            validationVisualizer.initVisualization(viewModel.nameContainsDelimiterValidationStatus(), nameField, false);
            validationVisualizer.initVisualization(viewModel.sameNameValidationStatus(), nameField);
            validationVisualizer.initVisualization(viewModel.searchRegexValidationStatus(), searchGroupSearchTerm);
            validationVisualizer.initVisualization(viewModel.searchSearchTermEmptyValidationStatus(), searchGroupSearchTerm);
            validationVisualizer.initVisualization(viewModel.keywordRegexValidationStatus(), keywordGroupSearchTerm);
            validationVisualizer.initVisualization(viewModel.keywordSearchTermEmptyValidationStatus(), keywordGroupSearchTerm);
        });

        // ToDo: This comes straight from hell

        nameField.textProperty().addListener((obs, oldval, newval) -> updateComponents());
        keywordGroupSearchTerm.textProperty().addListener((obs, oldval, newval) -> updateComponents());
        searchGroupSearchTerm.textProperty().addListener((obs, oldval, newval) -> updateComponents());

        nameField.addEventHandler(KeyEvent.KEY_RELEASED, e -> updateComponents());
        descriptionField.addEventHandler(KeyEvent.KEY_RELEASED, e -> updateComponents());
        iconField.addEventHandler(KeyEvent.KEY_RELEASED, e -> updateComponents());
        keywordGroupSearchField.addEventHandler(KeyEvent.KEY_RELEASED, e -> updateComponents());
        keywordGroupSearchTerm.addEventHandler(KeyEvent.KEY_RELEASED, e -> updateComponents());
        keywordGroupCaseSensitive.addEventHandler(ActionEvent.ANY, e -> updateComponents());
        keywordGroupRegex.addEventHandler(ActionEvent.ANY, e -> updateComponents());
        searchGroupSearchTerm.addEventHandler(ActionEvent.ANY, e -> updateComponents());
        searchGroupCaseSensitive.addEventHandler(ActionEvent.ANY, e -> updateComponents());
        searchGroupRegex.addEventHandler(ActionEvent.ANY, e -> updateComponents());

        // NPE
        // getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(viewModel.validationStatus().validProperty().not());
    }

    @FXML
    private void texGroupBrowse() {
        viewModel.texGroupBrowse();
    }

    // ToDo: The following lines NEED to be changed!

    private void updateComponents() {
        if (!showHints) {
            return;
        }

        String searchField;
        String searchTerm;
        if (keywordsRadioButton.isSelected()) {
            searchField = keywordGroupSearchField.getText().trim();
            searchTerm = keywordGroupSearchTerm.getText().trim();
            if (searchField.matches("\\w+") && !searchTerm.isEmpty()) {
                if (keywordGroupRegex.isSelected()) {
                    try {
                        Pattern.compile(searchTerm);
                        setDescription(GroupDescriptions.getDescriptionForPreview(searchField, searchTerm, keywordGroupCaseSensitive.isSelected(),
                                keywordGroupRegex.isSelected()));
                    } catch (PatternSyntaxException e) {
                        setDescription(GroupDescriptions.formatRegexException(searchTerm, e));
                    }
                } else {
                    setDescription(GroupDescriptions.getDescriptionForPreview(searchField, searchTerm, keywordGroupCaseSensitive.isSelected(),
                            keywordGroupRegex.isSelected()));
                }
            } else {
                setDescription(Localization.lang(
                        "Please enter the field to search (e.g. <b>keywords</b>) and the keyword to search it for (e.g. <b>electrical</b>)."));
            }
        } else if (searchRadioButton.isSelected()) {
            searchTerm = searchGroupSearchTerm.getText().trim();
            if (!searchTerm.isEmpty()) {
                setDescription(GroupDescriptions.fromTextFlowToHTMLString(SearchDescribers
                        .getSearchDescriberFor(new SearchQuery(
                                searchTerm,
                                searchGroupCaseSensitive.isSelected(),
                                searchGroupRegex.isSelected()))
                        .getDescription()));
                if (searchGroupRegex.isSelected()) {
                    try {
                        Pattern.compile(searchTerm);
                    } catch (PatternSyntaxException e) {
                        setDescription(GroupDescriptions.formatRegexException(searchTerm, e));
                    }
                }
            } else {
                setDescription(Localization
                        .lang("Please enter a search term. For example, to search all fields for <b>Smith</b>, enter:<p>"
                                + "<tt>smith</tt><p>"
                                + "To search the field <b>Author</b> for <b>Smith</b> and the field <b>Title</b> for <b>electrical</b>, enter:<p>"
                                + "<tt>author=smith and title=electrical</tt>"));
            }
        } else if (explicitRadioButton.isSelected()) {
            setDescription(GroupDescriptions.getDescriptionForPreview());
        }
    }

    private void setDescription(String description) {
        hintTextFlow.getChildren().setAll(GroupDescriptions.createFormattedDescription(description));
    }
}
