package org.jabref.gui.preferences.general;

import java.util.regex.Pattern;

import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextFormatter;
import javafx.util.Callback;
import javafx.util.converter.IntegerStringConverter;

import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.theme.ThemeColorScheme;
import org.jabref.gui.theme.ThemePreset;
import org.jabref.gui.theme.ThemePreviewView;
import org.jabref.gui.util.URLs;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.http.manager.HttpServerManager;
import org.jabref.languageserver.controller.LanguageServerController;
import org.jabref.logic.UiMessageHandler;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.remote.server.RemoteListenerServerManager;
import org.jabref.model.database.BibDatabaseMode;

import com.airhacks.afterburner.injection.Injector;

public class GeneralTab extends AbstractPreferenceTabView<GeneralTabViewModel> {

    // Formats the font-size input so that only integers can be entered.
    private final TextFormatter<Integer> fontSizeFormatter = new TextFormatter<>(new IntegerStringConverter(), 9,
            change -> {
                if (Pattern.matches("\\d*", change.getText())) {
                    return change;
                }
                change.setText("0");
                return change;
            });

    public GeneralTab() {
        this.viewModel = new GeneralTabViewModel(
                dialogService,
                preferences,
                Injector.instantiateModelOrService(HttpServerManager.class),
                Injector.instantiateModelOrService(LanguageServerController.class),
                Injector.instantiateModelOrService(UiMessageHandler.class),
                Injector.instantiateModelOrService(RemoteListenerServerManager.class),
                Injector.instantiateModelOrService(StateManager.class));
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("General");
    }

    private void buildView() {
        setContent(form()

                .section(Localization.lang("Appearance"), appearance -> appearance
                                .searchableCombo(Localization.lang("Language"),
                                        viewModel.languagesListProperty(),
                                        viewModel.selectedLanguageProperty(),
                                        Language::getDisplayName)
                                .combo(Localization.lang("Color scheme"),
                                        viewModel.colorSchemeListProperty(),
                                        viewModel.selectedThemeColorSchemeProperty(),
                                        ThemeColorScheme::getLocalizedName,
                                        colorScheme -> colorScheme.validate(viewModel.themeColorSchemeValidationStatus()))
                                .combo(Localization.lang("Theme"),
                                        viewModel.themesListProperty(),
                                        viewModel.selectedThemeProperty(),
                                        themeName(),
                                        theme -> {
                                            theme.validate(viewModel.themeValidationStatus());
                                            refreshThemeNameOnColorSchemeChange(theme.getNode());
                                        })
                                .field(Localization.lang("Preview"), buildThemePreview())
                                .checkWithField(Localization.lang("Custom theme"), viewModel.customThemeEnabledProperty(), viewModel.customPathToThemeProperty(),
                                        path -> path
                                                .browse(viewModel::importCSSFile)
                                                .disableWhen(viewModel.customThemeEnabledProperty().not())
                                                .grow())
                                .hyperlink(Localization.lang("Get more themes..."), viewModel::openBrowser)
                                .checkbox(Localization.lang("Override default font settings"), viewModel.fontOverrideProperty())
                                .field(Localization.lang("Size"), buildFontSizeSpinner(),
                                        size -> size.validate(viewModel.fontSizeValidationStatus())),
                        appearance -> appearance.help(URLs.CUSTOM_THEME_DOC))

                .section(Localization.lang("User interface"), userInterface -> userInterface
                        .checkbox(Localization.lang("Open last edited libraries on startup"), viewModel.openLastStartupProperty())
                        .checkbox(Localization.lang("Show advanced hints (i.e. helpful tooltips, suggestions and explanation)"), viewModel.showAdvancedHintsProperty())
                        .checkbox(Localization.lang("Show confirmation dialog when deleting entries"), viewModel.confirmDeleteProperty())
                        .checkbox(Localization.lang("Ask whether to include cross-references when copying to another library"), viewModel.shouldAskForIncludingCrossReferences())
                        .checkbox(Localization.lang("Hide tab bar when single library is present"), viewModel.confirmHideTabBarProperty())
                        .checkbox(Localization.lang("Do not show donation prompt again"), viewModel.donationNeverShowProperty())
                        .checkbox(Localization.lang("Experimental search (Postgres)"), viewModel.usePostgresSearchProperty()))

                .section(Localization.lang("Single instance"), singleInstance -> singleInstance
                        .checkWithField(Localization.lang("Enforce single JabRef instance (and allow remote operations) using port"),
                                viewModel.remoteServerProperty(), viewModel.remotePortProperty(),
                                port -> port.validate(viewModel.remotePortValidationStatus())
                                            .help(HelpFile.REMOTE)))

                .section(Localization.lang("HTTP Server"), httpServer -> httpServer
                        .checkWithField(Localization.lang("Enable HTTP Server (e.g., for JabMap) on port"),
                                viewModel.enableHttpServerProperty(), viewModel.httpPortProperty(),
                                port -> port.validate(viewModel.httpPortValidationStatus()))
                        .checkbox(Localization.lang("Skip import dialog for entries received from browser extensions"), viewModel.directHttpImportProperty()))

                .section(Localization.lang("LSP Server"), lspServer -> lspServer
                        .checkWithField(Localization.lang("Enable LSP Server on port"),
                                viewModel.enableLanguageServerProperty(), viewModel.languageServerPortProperty(),
                                port -> port.validate(viewModel.languageServerPortValidationStatus())))

                .section(Localization.lang("Libraries"), libraries -> libraries
                        .combo(Localization.lang("Default library mode"),
                                viewModel.biblatexModeListProperty(), viewModel.selectedBiblatexModeProperty(), BibDatabaseMode::getFormattedName))

                .section(Localization.lang("Saving"), saving -> saving
                        .checkbox(Localization.lang("Always reformat library on save and export"), viewModel.alwaysReformatBibProperty())
                        .checkbox(Localization.lang("Autosave local libraries"), viewModel.autosaveLocalLibrariesProperty(),
                                autosave -> autosave.help(HelpFile.AUTOSAVE))
                        .checkbox(Localization.lang("Create backup"), viewModel.createBackupProperty())
                        .stringField(null, viewModel.backupDirectoryProperty(),
                                directory -> directory
                                        .browse(viewModel::backupFileDirBrowse)
                                        .disableWhen(viewModel.createBackupProperty().not())))

                .build());
    }

    /// Themes that pair a dark and a light hue are named after the hue of the selected color scheme.
    private Callback<ThemePreset, String> themeName() {
        return theme -> theme.getLocalizedName(viewModel.selectedThemeColorSchemeProperty().get());
    }

    /// A [ComboBox] renders its button cell -- the collapsed row showing the selection -- only when that
    /// cell's item changes. Names that follow the color scheme therefore need a fresh cell; the entries in
    /// the popup are re-rendered by the view model refreshing the item list.
    private void refreshThemeNameOnColorSchemeChange(ComboBox<ThemePreset> themes) {
        viewModel.selectedThemeColorSchemeProperty().addListener(_ -> themes.setButtonCell(
                new ViewModelListCellFactory<ThemePreset>()
                        .withText(themeName())
                        .call(null)));
    }

    private ThemePreviewView buildThemePreview() {
        ThemePreviewView preview = new ThemePreviewView();
        preview.bind(viewModel.selectedThemeProperty(), viewModel.selectedThemeColorSchemeProperty());
        return preview;
    }

    private Spinner<Integer> buildFontSizeSpinner() {
        Spinner<Integer> fontSize = new Spinner<>();
        fontSize.setValueFactory(GeneralTabViewModel.fontSizeValueFactory);
        fontSize.setEditable(true);
        fontSize.setMaxWidth(100.0);
        fontSize.getEditor().setAlignment(Pos.CENTER_RIGHT);
        fontSize.getEditor().textProperty().bindBidirectional(viewModel.fontSizeProperty());
        fontSize.getEditor().setTextFormatter(fontSizeFormatter);
        fontSize.disableProperty().bind(viewModel.fontOverrideProperty().not());
        return fontSize;
    }
}
