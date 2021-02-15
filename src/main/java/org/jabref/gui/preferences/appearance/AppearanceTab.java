package org.jabref.gui.preferences.appearance;

import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.converter.IntegerStringConverter;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class AppearanceTab extends AbstractPreferenceTabView<AppearanceTabViewModel> implements PreferencesTab {

    @FXML private CheckBox fontOverride;
    @FXML private Spinner<Integer> fontSize;
    @FXML private RadioButton themeLight;
    @FXML private RadioButton themeDark;
    @FXML private RadioButton customTheme;
    @FXML private TextField customThemePath;

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    // The fontSizeFormatter formats the input given to the fontSize spinner so that non valid values cannot be entered.
    private TextFormatter<Integer> fontSizeFormatter = new TextFormatter<Integer>(new IntegerStringConverter(), 9,
            c -> {
                if (Pattern.matches("\\d*", c.getText())) {
                    return c;
                }
                c.setText("0");
                return c;
            });

    public AppearanceTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Appearance");
    }

    public void initialize() {
        this.viewModel = new AppearanceTabViewModel(dialogService, preferencesService);

        fontOverride.selectedProperty().bindBidirectional(viewModel.fontOverrideProperty());

        // Spinner does neither support alignment nor disableProperty in FXML
        fontSize.disableProperty().bind(fontOverride.selectedProperty().not());
        fontSize.getEditor().setAlignment(Pos.CENTER_RIGHT);
        fontSize.setValueFactory(AppearanceTabViewModel.fontSizeValueFactory);
        fontSize.getEditor().textProperty().bindBidirectional(viewModel.fontSizeProperty());
        fontSize.getEditor().setTextFormatter(fontSizeFormatter);

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
