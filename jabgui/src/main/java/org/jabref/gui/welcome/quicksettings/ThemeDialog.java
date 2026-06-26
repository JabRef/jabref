package org.jabref.gui.welcome.quicksettings;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialog;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.theme.ThemeColorScheme;
import org.jabref.gui.theme.ThemePreset;
import org.jabref.gui.util.URLs;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.gui.util.component.HelpButton;
import org.jabref.gui.welcome.quicksettings.viewmodel.ThemeDialogViewModel;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class ThemeDialog extends FXDialog {
    @FXML private ComboBox<ThemePreset> theme;
    @FXML private ComboBox<ThemeColorScheme> themeColorScheme;
    @FXML private HelpButton helpButton;
    @FXML private CheckBox customTheme;
    @FXML private TextField customThemePath;
    private ThemeDialogViewModel viewModel;
    private final GuiPreferences preferences;
    private final DialogService dialogService;

    public ThemeDialog(GuiPreferences preferences, DialogService dialogService) {
        super(Alert.AlertType.NONE, Localization.lang("Change visual appearance"));

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

        new ViewModelListCellFactory<ThemePreset>()
                .withText(ThemePreset::getLocalizedName)
                .install(theme);
        theme.itemsProperty().bind(viewModel.themesListProperty());
        theme.valueProperty().bindBidirectional(viewModel.selectedThemeProperty());

        new ViewModelListCellFactory<ThemeColorScheme>()
                .withText(ThemeColorScheme::getLocalizedName)
                .install(themeColorScheme);
        themeColorScheme.itemsProperty().bind(viewModel.colorSchemeListProperty());
        themeColorScheme.valueProperty().bindBidirectional(viewModel.selectedThemeColorSchemeProperty());

        customTheme.selectedProperty().bindBidirectional(viewModel.customThemeEnabledProperty());
        customThemePath.textProperty().bindBidirectional(viewModel.customPathToThemeProperty());

        helpButton.setHelpUrl(URLs.CUSTOM_THEME_DOC);
    }

    @FXML
    void importTheme() {
        viewModel.importCSSFile();
    }
}
