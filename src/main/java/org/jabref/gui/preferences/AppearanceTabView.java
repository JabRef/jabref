package org.jabref.gui.preferences;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class AppearanceTabView extends AbstractPreferenceTabView<AppearanceTabViewModel> implements PreferencesTab {

    @FXML public CheckBox fontOverride;
    @FXML public TextField fontSize;
    @FXML public RadioButton themeLight;
    @FXML public RadioButton themeDark;

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    public AppearanceTabView(JabRefPreferences preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                .root(this)
                .load();
    }

    @Override
    public String getTabName() { return Localization.lang("Appearance"); }

    public void initialize () {
        this.viewModel = new AppearanceTabViewModel(dialogService, preferences);

        fontOverride.selectedProperty().bindBidirectional(viewModel.fontOverrideProperty());
        fontSize.setTextFormatter(ControlHelper.getIntegerTextFormatter());
        fontSize.textProperty().bindBidirectional(viewModel.fontSizeProperty());

        themeLight.selectedProperty().bindBidirectional(viewModel.themeLightProperty());
        themeDark.selectedProperty().bindBidirectional(viewModel.themeDarkProperty());

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> validationVisualizer.initVisualization(viewModel.fontSizeValidationStatus(), fontSize));
    }
}
