package org.jabref.gui.preferences.general;

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

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.theme.ThemeTypes;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import jakarta.inject.Inject;

public class GeneralTab extends AbstractPreferenceTabView<GeneralTabViewModel> implements PreferencesTab {

    @FXML private ComboBox<Language> language;
    @FXML private ComboBox<ThemeTypes> theme;
    @FXML private CheckBox themeSyncOs;
    @FXML private TextField customThemePath;
    @FXML private Button customThemeBrowse;
    @FXML private CheckBox fontOverride;
    @FXML private Spinner<Integer> fontSize;
    @FXML private CheckBox openLastStartup;
    @FXML private CheckBox showAdvancedHints;
    @FXML private CheckBox inspectionWarningDuplicate;

    @FXML private CheckBox confirmDelete;
    @FXML private CheckBox confirmHideTabBar;
    @FXML private ComboBox<BibDatabaseMode> biblatexMode;
    @FXML private CheckBox alwaysReformatBib;
    @FXML private CheckBox autosaveLocalLibraries;
    @FXML private Button autosaveLocalLibrariesHelp;
    @FXML private CheckBox createBackup;
    @FXML private TextField backupDirectory;
    @FXML private CheckBox remoteServer;
    @FXML private TextField remotePort;
    @FXML private Button remoteHelp;
    @Inject private FileUpdateMonitor fileUpdateMonitor;
    @Inject private BibEntryTypesManager entryTypesManager;

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

    public GeneralTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("General");
    }

    public void initialize() {
        this.viewModel = new GeneralTabViewModel(dialogService, preferences, fileUpdateMonitor, entryTypesManager);

        new ViewModelListCellFactory<Language>()
                .withText(Language::getDisplayName)
                .install(language);
        language.itemsProperty().bind(viewModel.languagesListProperty());
        language.valueProperty().bindBidirectional(viewModel.selectedLanguageProperty());

        fontOverride.selectedProperty().bindBidirectional(viewModel.fontOverrideProperty());

        // Spinner does neither support alignment nor disableProperty in FXML
        fontSize.disableProperty().bind(fontOverride.selectedProperty().not());
        fontSize.getEditor().setAlignment(Pos.CENTER_RIGHT);
        fontSize.setValueFactory(GeneralTabViewModel.fontSizeValueFactory);
        fontSize.getEditor().textProperty().bindBidirectional(viewModel.fontSizeProperty());
        fontSize.getEditor().setTextFormatter(fontSizeFormatter);

        new ViewModelListCellFactory<ThemeTypes>()
                .withText(ThemeTypes::getDisplayName)
                .install(theme);
        theme.itemsProperty().bind(viewModel.themesListProperty());
        theme.valueProperty().bindBidirectional(viewModel.selectedThemeProperty());
        themeSyncOs.selectedProperty().bindBidirectional(viewModel.themeSyncOsProperty());
        customThemePath.textProperty().bindBidirectional(viewModel.customPathToThemeProperty());
        EasyBind.subscribe(viewModel.selectedThemeProperty(), theme -> {
            boolean isCustomTheme = theme == ThemeTypes.CUSTOM;
            customThemePath.disableProperty().set(!isCustomTheme);
            customThemeBrowse.disableProperty().set(!isCustomTheme);
        });

        validationVisualizer.setDecoration(new IconValidationDecorator());

        openLastStartup.selectedProperty().bindBidirectional(viewModel.openLastStartupProperty());
        showAdvancedHints.selectedProperty().bindBidirectional(viewModel.showAdvancedHintsProperty());
        inspectionWarningDuplicate.selectedProperty().bindBidirectional(viewModel.inspectionWarningDuplicateProperty());
        confirmDelete.selectedProperty().bindBidirectional(viewModel.confirmDeleteProperty());
        confirmHideTabBar.selectedProperty().bindBidirectional(viewModel.confirmHideTabBarProperty());

        new ViewModelListCellFactory<BibDatabaseMode>()
                .withText(BibDatabaseMode::getFormattedName)
                .install(biblatexMode);
        biblatexMode.itemsProperty().bind(viewModel.biblatexModeListProperty());
        biblatexMode.valueProperty().bindBidirectional(viewModel.selectedBiblatexModeProperty());

        alwaysReformatBib.selectedProperty().bindBidirectional(viewModel.alwaysReformatBibProperty());
        autosaveLocalLibraries.selectedProperty().bindBidirectional(viewModel.autosaveLocalLibrariesProperty());
        ActionFactory actionFactory = new ActionFactory();
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AUTOSAVE, dialogService, preferences.getExternalApplicationsPreferences()), autosaveLocalLibrariesHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.REMOTE, dialogService, preferences.getExternalApplicationsPreferences()), remoteHelp);

        createBackup.selectedProperty().bindBidirectional(viewModel.createBackupProperty());
        backupDirectory.textProperty().bindBidirectional(viewModel.backupDirectoryProperty());
        backupDirectory.disableProperty().bind(viewModel.createBackupProperty().not());

        Platform.runLater(() -> {
            validationVisualizer.initVisualization(viewModel.remotePortValidationStatus(), remotePort);
            validationVisualizer.initVisualization(viewModel.fontSizeValidationStatus(), fontSize);
            validationVisualizer.initVisualization(viewModel.customPathToThemeValidationStatus(), customThemePath);
        });

        remoteServer.selectedProperty().bindBidirectional(viewModel.remoteServerProperty());
        remotePort.textProperty().bindBidirectional(viewModel.remotePortProperty());
        remotePort.disableProperty().bind(remoteServer.selectedProperty().not());
    }

    @FXML
    void importTheme() {
        viewModel.importCSSFile();
    }

    public void backupFileDirBrowse() {
        viewModel.backupFileDirBrowse();
    }

    @FXML
    public void openBrowser() {
        viewModel.openBrowser();
    }
}
