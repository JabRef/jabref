package org.jabref.gui.preferences.appearance;

import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.converter.IntegerStringConverter;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class AppearanceTab extends AbstractPreferenceTabView<AppearanceTabViewModel> implements PreferencesTab {

    @FXML private ComboBox<Language> language;
    @FXML private ComboBox<AppearanceTabViewModel.ThemeTypes> theme;
    @FXML private TextField customThemePath;
    @FXML private Button customThemeBrowse;
    @FXML private CheckBox fontOverride;
    @FXML private Spinner<Integer> fontSize;
    @FXML private CheckBox openLastStartup;
    @FXML private CheckBox showAdvancedHints;
    @FXML private CheckBox inspectionWarningDuplicate;
    @FXML private CheckBox confirmDelete;
    @FXML private CheckBox collectTelemetry;

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    // The fontSizeFormatter formats the input given to the fontSize spinner so that non valid values cannot be entered.
    private final TextFormatter<Integer> fontSizeFormatter = new TextFormatter<>(new IntegerStringConverter(), 9,
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

        new ViewModelListCellFactory<Language>()
                .withText(Language::getDisplayName)
                .install(language);
        language.itemsProperty().bind(viewModel.languagesListProperty());
        language.valueProperty().bindBidirectional(viewModel.selectedLanguageProperty());

        fontOverride.selectedProperty().bindBidirectional(viewModel.fontOverrideProperty());

        // Spinner does neither support alignment nor disableProperty in FXML
        fontSize.disableProperty().bind(fontOverride.selectedProperty().not());
        fontSize.getEditor().setAlignment(Pos.CENTER_RIGHT);
        fontSize.setValueFactory(AppearanceTabViewModel.fontSizeValueFactory);
        fontSize.getEditor().textProperty().bindBidirectional(viewModel.fontSizeProperty());
        fontSize.getEditor().setTextFormatter(fontSizeFormatter);

        new ViewModelListCellFactory<AppearanceTabViewModel.ThemeTypes>()
                .withText(AppearanceTabViewModel.ThemeTypes::getDisplayName)
                .install(theme);
        theme.itemsProperty().bind(viewModel.themesListProperty());
        theme.valueProperty().bindBidirectional(viewModel.selectedThemeProperty());
        customThemePath.textProperty().bindBidirectional(viewModel.customPathToThemeProperty());
        EasyBind.subscribe(viewModel.selectedThemeProperty(), theme -> {
            boolean isCustomTheme = theme == AppearanceTabViewModel.ThemeTypes.CUSTOM;
            customThemePath.disableProperty().set(!isCustomTheme);
            customThemeBrowse.disableProperty().set(!isCustomTheme);
        });

        validationVisualizer.setDecoration(new IconValidationDecorator());

        openLastStartup.selectedProperty().bindBidirectional(viewModel.openLastStartupProperty());
        showAdvancedHints.selectedProperty().bindBidirectional(viewModel.showAdvancedHintsProperty());
        inspectionWarningDuplicate.selectedProperty().bindBidirectional(viewModel.inspectionWarningDuplicateProperty());
        confirmDelete.selectedProperty().bindBidirectional(viewModel.confirmDeleteProperty());

        collectTelemetry.selectedProperty().bindBidirectional(viewModel.collectTelemetryProperty());

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
