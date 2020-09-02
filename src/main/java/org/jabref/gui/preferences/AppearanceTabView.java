package org.jabref.gui.preferences;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;

import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class AppearanceTabView extends AbstractPreferenceTabView<AppearanceTabViewModel> implements PreferencesTab {

    @FXML public CheckBox fontOverride;
    @FXML public Spinner<Integer> fontSize;
    @FXML public RadioButton themeLight;
    @FXML public RadioButton themeDark;
    @FXML public RadioButton customTheme;
    @FXML public TextField customThemePath;

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    public AppearanceTabView(PreferencesService preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Appearance");
    }

    public void initialize() {
        this.viewModel = new AppearanceTabViewModel(dialogService, preferences);

        fontOverride.selectedProperty().bindBidirectional(viewModel.fontOverrideProperty());

        // Spinner does neither support alignment nor disableProperty in FXML
        fontSize.disableProperty().bind(fontOverride.selectedProperty().not());
        fontSize.getEditor().setAlignment(Pos.CENTER_RIGHT);
        fontSize.setValueFactory(AppearanceTabViewModel.fontSizeValueFactory);
        fontSize.getEditor().textProperty().bindBidirectional(viewModel.fontSizeProperty());

        themeLight.selectedProperty().bindBidirectional(viewModel.themeLightProperty());
        themeDark.selectedProperty().bindBidirectional(viewModel.themeDarkProperty());
        customTheme.selectedProperty().bindBidirectional(viewModel.customThemeProperty());
        customThemePath.textProperty().bindBidirectional(viewModel.customPathToThemeProperty());

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> {
            validationVisualizer.initVisualization(viewModel.fontSizeValidationStatus(), fontSize);
            validationVisualizer.initVisualization(viewModel.customPathToThemeValidationStatus(), customThemePath);
        });
    }

    @FXML
    void importTheme() {
        viewModel.importCSSFile();
    }
}
