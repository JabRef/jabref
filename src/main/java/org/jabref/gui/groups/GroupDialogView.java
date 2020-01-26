package org.jabref.gui.groups;

import java.util.EnumMap;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.preferences.PreferencesService;

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

    private final EnumMap<GroupHierarchyType,String> hierarchyText = new EnumMap<>(GroupHierarchyType.class);
    private final EnumMap<GroupHierarchyType,String> hierarchyToolTip = new EnumMap<>(GroupHierarchyType.class);

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();
    private final GroupDialogViewModel viewModel;

    public GroupDialogView(DialogService dialogService, BibDatabaseContext currentDatabase, PreferencesService preferencesService, AbstractGroup editedGroup) {
        viewModel = new GroupDialogViewModel(dialogService, currentDatabase, preferencesService, editedGroup);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        if (editedGroup == null) {
            this.setTitle(Localization.lang("Add subgroup"));
        } else {
            this.setTitle(Localization.lang("Edit group") + " " + editedGroup.getName());
        }

        setResultConverter(viewModel::resultConverter);
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
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
        searchGroupCaseSensitive.selectedProperty().bindBidirectional(viewModel.searchGroupCaseSensitiveProperty());
        searchGroupRegex.selectedProperty().bindBidirectional(viewModel.searchGroupRegexProperty());

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
        });

        // Binding to the button throws a NPE, since it doesn't exist yet. Working around.
        viewModel.validationStatus().validProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                getDialogPane().lookupButton(ButtonType.OK).setDisable(false);
            } else {
                getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
            }
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
}
