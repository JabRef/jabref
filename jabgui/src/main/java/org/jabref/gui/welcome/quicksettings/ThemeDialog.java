package org.jabref.gui.welcome.quicksettings;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialog;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.theme.ThemeTypes;
import org.jabref.gui.util.URLs;
import org.jabref.gui.util.component.HelpButton;
import org.jabref.gui.welcome.components.ThemeWireFrame;
import org.jabref.gui.welcome.quicksettings.viewmodel.ThemeDialogViewModel;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class ThemeDialog extends FXDialog {
    @FXML private RadioButton lightRadio;
    @FXML private RadioButton darkRadio;
    @FXML private RadioButton customRadio;
    @FXML private ToggleGroup themeGroup;
    @FXML private TextField customPathField;
    @FXML private VBox customThemeContainer;
    @FXML private HelpButton helpButton;
    @FXML private ThemeWireFrame lightWireframe;
    @FXML private ThemeWireFrame darkWireframe;
    @FXML private ThemeWireFrame customWireframe;

    private ThemeDialogViewModel viewModel;
    private final GuiPreferences preferences;
    private final DialogService dialogService;

    public ThemeDialog(GuiPreferences preferences, DialogService dialogService) {
        super(Alert.AlertType.NONE, Localization.lang("Change visual theme"));

        this.preferences = preferences;
        this.dialogService = dialogService;

        setHeaderText(Localization.lang("Select your preferred theme for the application"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(button -> {
            if (button == ButtonType.OK && viewModel.isValidConfiguration()) {
                viewModel.saveSettings();
            }
            return null;
        });
    }

    @FXML
    private void initialize() {
        viewModel = new ThemeDialogViewModel(preferences, dialogService);

        lightRadio.setUserData(ThemeTypes.LIGHT);
        darkRadio.setUserData(ThemeTypes.DARK);
        customRadio.setUserData(ThemeTypes.CUSTOM);

        lightWireframe.setThemeType(ThemeTypes.LIGHT);
        darkWireframe.setThemeType(ThemeTypes.DARK);
        customWireframe.setThemeType(ThemeTypes.CUSTOM);

        customPathField.textProperty().bindBidirectional(viewModel.customPathProperty());

        themeGroup.selectedToggleProperty().addListener((_, _, newValue) -> {
            if (newValue != null) {
                ThemeTypes selectedType = (ThemeTypes) newValue.getUserData();
                viewModel.setSelectedTheme(selectedType);
                updateCustomThemeVisibility(selectedType == ThemeTypes.CUSTOM);
            }
        });

        selectInitialTheme();
        updateCustomThemeVisibility(viewModel.getSelectedTheme() == ThemeTypes.CUSTOM);

        helpButton.setHelpUrl(URLs.CUSTOM_THEME_DOC);
    }

    private void selectInitialTheme() {
        switch (viewModel.getSelectedTheme()) {
            case LIGHT ->
                    lightRadio.setSelected(true);
            case DARK ->
                    darkRadio.setSelected(true);
            case CUSTOM ->
                    customRadio.setSelected(true);
        }
    }

    private void updateCustomThemeVisibility(boolean visible) {
        customThemeContainer.setVisible(visible);
        customThemeContainer.setManaged(visible);
        if (customThemeContainer.getScene() != null) {
            customThemeContainer.getScene().getWindow().sizeToScene();
        }
    }

    @FXML
    private void selectLight() {
        lightRadio.fire();
    }

    @FXML
    private void selectDark() {
        darkRadio.fire();
    }

    @FXML
    private void selectCustom() {
        customRadio.fire();
    }

    @FXML
    private void browseThemeFile() {
        viewModel.browseForThemeFile();
    }
}
