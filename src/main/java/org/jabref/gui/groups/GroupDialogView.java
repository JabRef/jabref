package org.jabref.gui.groups;

import java.util.EnumMap;
import java.util.EnumSet;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.jabref.gui.DialogService;
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
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignG;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignJ;
import org.kordamp.ikonli.materialdesign2.MaterialDesignK;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignN;
import org.kordamp.ikonli.materialdesign2.MaterialDesignO;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignQ;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.kordamp.ikonli.materialdesign2.MaterialDesignU;
import org.kordamp.ikonli.materialdesign2.MaterialDesignV;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;
import org.kordamp.ikonli.materialdesign2.MaterialDesignX;
import org.kordamp.ikonli.materialdesign2.MaterialDesignY;
import org.kordamp.ikonli.materialdesign2.MaterialDesignZ;

import static java.util.EnumSet.allOf;

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

    private final EnumMap<GroupHierarchyType, String> hierarchyText = new EnumMap<>(GroupHierarchyType.class);
    private final EnumMap<GroupHierarchyType, String> hierarchyToolTip = new EnumMap<>(GroupHierarchyType.class);

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();
    private final GroupDialogViewModel viewModel;

    public GroupDialogView(DialogService dialogService, BibDatabaseContext currentDatabase, PreferencesService preferencesService, AbstractGroup editedGroup, GroupDialogHeader groupDialogHeader) {
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

        // Binding to the button throws a NPE, since it doesn't exist yet. Working around.
        viewModel.validationStatus().validProperty().addListener((obs, _oldValue, validationStatus) ->
                getDialogPane().lookupButton(ButtonType.OK).setDisable(!validationStatus));
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
        System.out.println("clicking the icon picker button");

        TabPane tabPane = new TabPane();

        tabPane.getTabs().add(new IconTab(MaterialDesignA.class, allOf(MaterialDesignA.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignB.class, allOf(MaterialDesignB.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignC.class, allOf(MaterialDesignC.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignD.class, allOf(MaterialDesignD.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignE.class, allOf(MaterialDesignE.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignF.class, allOf(MaterialDesignF.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignG.class, allOf(MaterialDesignG.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignH.class, allOf(MaterialDesignH.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignI.class, allOf(MaterialDesignI.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignJ.class, allOf(MaterialDesignJ.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignK.class, allOf(MaterialDesignK.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignL.class, allOf(MaterialDesignL.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignM.class, allOf(MaterialDesignM.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignN.class, allOf(MaterialDesignN.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignO.class, allOf(MaterialDesignO.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignP.class, allOf(MaterialDesignP.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignQ.class, allOf(MaterialDesignQ.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignR.class, allOf(MaterialDesignR.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignS.class, allOf(MaterialDesignS.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignT.class, allOf(MaterialDesignT.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignU.class, allOf(MaterialDesignU.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignV.class, allOf(MaterialDesignV.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignW.class, allOf(MaterialDesignW.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignX.class, allOf(MaterialDesignX.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignY.class, allOf(MaterialDesignY.class)));
        tabPane.getTabs().add(new IconTab(MaterialDesignZ.class, allOf(MaterialDesignZ.class)));

        //create Scene
        Scene scene = new Scene(tabPane);
        scene.setCursor(Cursor.CLOSED_HAND);

        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setX(700);
        stage.setY(550);
        stage.setTitle("Icon Picker");
        stage.setScene(scene);
        stage.setWidth(512);
        stage.setHeight(400);
        stage.show();
    }

    private class IconTab extends Tab {
        private IconTab(Class<? extends Ikon> iconFontClass, EnumSet<? extends Ikon> enumSet) {
            super(iconFontClass.getSimpleName());
            setClosable(false);
            GridPane pane = new GridPane();

            int column = 0;
            int row = 0;
            int index = 0;
            for (Ikon value : enumSet) {
                FontIcon icon = FontIcon.of(value);
                icon.getStyleClass().setAll("font-icon");
                Button button = new Button();
                button.setGraphic(icon);
                button.setOnAction(event -> {
                    iconField.textProperty().setValue(String.valueOf(icon.getIconCode()));
                    Stage stage = (Stage) button.getScene().getWindow();
                    stage.close();
                });
                pane.add(button, column++, row);
                GridPane.setMargin(button, new Insets(10, 10, 10, 10));
                if (++index % 10 == 0) {
                    column = 0;
                    row++;
                }
            }
            ScrollPane scrollPane = new ScrollPane(pane);
            setContent(scrollPane);
        }
    }
}
