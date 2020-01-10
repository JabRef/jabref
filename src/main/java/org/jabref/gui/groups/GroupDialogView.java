package org.jabref.gui.groups;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.text.TextFlow;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class GroupDialogView extends BaseDialog<AbstractGroup> {

    // Basic Settings
    @FXML private TextField nameField;
    @FXML private TextField descriptionField;
    @FXML private TextField iconField;
    @FXML private ColorPicker colorField;

    // Hierarchical Context
    @FXML private RadioButton independentButton;
    @FXML private RadioButton intersectionButton;
    @FXML private RadioButton unionButton;

    // Type
    @FXML private RadioButton explicitRadioButton;
    @FXML private RadioButton keywordsRadioButton;
    @FXML private RadioButton searchRadioButton;
    @FXML private RadioButton autoRadioButton;
    @FXML private RadioButton texRadioButton;

    // Option Groups
    @FXML private TextField keyWordGroupSearchTerm;
    @FXML private TextField keyWordGroupSearchField;
    @FXML private CheckBox keywordGroupCaseSensitive;
    @FXML private CheckBox keywordGroupRegExp;

    @FXML private TextField searchGroupSearchExpression;
    @FXML private CheckBox searchGroupCaseSensitive;
    @FXML private CheckBox searchGroupRegExp;

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

    public GroupDialogView(DialogService dialogService, BasePanel basePanel, JabRefPreferences prefs, AbstractGroup editedGroup) {
        viewModel = new GroupDialogViewModel(dialogService, basePanel, prefs, editedGroup);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        if (editedGroup == null) {
            this.setTitle(Localization.lang("Add subgroup"));
        } else {
            this.setTitle(Localization.lang("Edit group"));
        }

        setResultConverter(viewModel::resultConverter);
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
    }

    public GroupDialogView(DialogService dialogService, AbstractGroup editedGroup) {
        this(dialogService, JabRefGUI.getMainFrame().getCurrentBasePanel(), Globals.prefs, editedGroup);
    }

    public GroupDialogView(DialogService dialogService) {
        this(dialogService, JabRefGUI.getMainFrame().getCurrentBasePanel(), Globals.prefs, null);
    }

    @FXML
    public void initialize() {
        nameField.textProperty().bindBidirectional(viewModel.nameProperty());
        descriptionField.textProperty().bindBidirectional(viewModel.descriptionProperty());
        iconField.textProperty().bindBidirectional(viewModel.iconProperty());
        colorField.valueProperty().bindBidirectional(viewModel.colorFieldProperty());

        independentButton.selectedProperty().bindBidirectional(viewModel.hierarchyIndependentProperty());
        intersectionButton.selectedProperty().bindBidirectional(viewModel.hierarchyIntersectionProperty());
        unionButton.selectedProperty().bindBidirectional(viewModel.hierarchyUnionProperty());

        explicitRadioButton.selectedProperty().bindBidirectional(viewModel.typeExplicitProperty());
        keywordsRadioButton.selectedProperty().bindBidirectional(viewModel.typeKeywordsProperty());
        searchRadioButton.selectedProperty().bindBidirectional(viewModel.typeSearchProperty());
        autoRadioButton.selectedProperty().bindBidirectional(viewModel.typeAutoProperty());
        texRadioButton.selectedProperty().bindBidirectional(viewModel.typeTexProperty());

        keyWordGroupSearchTerm.textProperty().bindBidirectional(viewModel.keywordGroupSearchTermProperty());
        keyWordGroupSearchField.textProperty().bindBidirectional(viewModel.keywordGroupSearchFieldProperty());
        keywordGroupCaseSensitive.selectedProperty().bindBidirectional(viewModel.keywordGroupCaseSensitiveProperty());
        keywordGroupRegExp.selectedProperty().bindBidirectional(viewModel.keywordGroupRegExpProperty());

        searchGroupSearchExpression.textProperty().bindBidirectional(viewModel.searchGroupSearchExpressionProperty());
        searchGroupCaseSensitive.selectedProperty().bindBidirectional(viewModel.searchGroupCaseSensitiveProperty());
        searchGroupRegExp.selectedProperty().bindBidirectional(viewModel.searchGroupRegExpProperty());

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
            validationVisualizer.initVisualization(viewModel.sameNameValidationStatus(), nameField);
            validationVisualizer.initVisualization(viewModel.searchRegexValidationStatus(), searchGroupSearchExpression);
            validationVisualizer.initVisualization(viewModel.keywordRegexValidationStatus(), keyWordGroupSearchTerm);
        });

        // getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(viewModel.validationStatus().validProperty().not());
    }

    @FXML
    private void texGroupBrowse() {
        viewModel.texGroupBrowse();
    }

}
