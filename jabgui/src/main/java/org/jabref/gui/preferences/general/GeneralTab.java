package org.jabref.gui.preferences.general;

import java.util.regex.Pattern;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextFormatter;
import javafx.util.converter.IntegerStringConverter;

import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.theme.ThemeColorScheme;
import org.jabref.gui.theme.ThemeGalleryView;
import org.jabref.gui.util.URLs;
import org.jabref.http.server.manager.HttpServerManager;
import org.jabref.languageserver.controller.LanguageServerController;
import org.jabref.logic.UiMessageHandler;
import org.jabref.logic.citedrive.OAuthSessionRegistry;
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
                Injector.instantiateModelOrService(OAuthSessionRegistry.class),
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
                                .expander(Localization.lang("Theme"), buildThemeSummary(), theme -> theme
                                        .custom(buildThemeGallery())
                                        .group(custom -> custom
                                                        .stringField(Localization.lang("Custom theme"), viewModel.customPathToThemeProperty(),
                                                                path -> path.grow().browse(viewModel::importCSSFile)),
                                                custom -> custom.visibleWhen(viewModel.customThemeEnabledProperty()))
                                        .hyperlink(Localization.lang("Get more themes..."), viewModel::openBrowser))
                                .checkbox(Localization.lang("Override default font settings"), viewModel.fontOverrideProperty(),
                                        fontOverride -> fontOverride.attach(buildFontSizeSpinner(),
                                                size -> size.disableWhen(viewModel.fontOverrideProperty().not())
                                                            .validate(viewModel.fontSizeValidationStatus(), size.node())))
                                .checkbox(Localization.lang("Taller rows in lists and tables (long text wraps)"), viewModel.wrapMainTableTextProperty()),
                        appearance -> appearance.help(URLs.CUSTOM_THEME_DOC))

                .section(Localization.lang("User interface"), userInterface -> userInterface
                        .checkbox(Localization.lang("Open last edited libraries on startup"), viewModel.openLastStartupProperty())
                        .checkbox(Localization.lang("Show advanced hints (i.e. helpful tooltips, suggestions and explanation)"), viewModel.showAdvancedHintsProperty())
                        .checkbox(Localization.lang("Show confirmation dialog when deleting entries"), viewModel.confirmDeleteProperty())
                        .checkbox(Localization.lang("Ask whether to include cross-references when copying to another library"), viewModel.shouldAskForIncludingCrossReferences())
                        .checkbox(Localization.lang("Hide tab bar when single library is present"), viewModel.confirmHideTabBarProperty())
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
                        .checkWithField(Localization.lang("Create backup"), viewModel.createBackupProperty(),
                                viewModel.backupDirectoryProperty(),
                                directory -> directory
                                        .grow()
                                        .browse(viewModel::backupFileDirBrowse)))

                .build());
    }

    private Label buildThemeSummary() {
        Label summary = new Label();
        summary.textProperty().bind(viewModel.themeSummaryProperty());
        return summary;
    }

    private ThemeGalleryView buildThemeGallery() {
        ThemeGalleryView gallery = new ThemeGalleryView();
        gallery.bind(viewModel.themesListProperty(), viewModel.selectedThemeProperty(), viewModel.customThemeEnabledProperty(), viewModel.selectedThemeColorSchemeProperty());
        return gallery;
    }

    private Spinner<Integer> buildFontSizeSpinner() {
        Spinner<Integer> fontSize = new Spinner<>();
        fontSize.setValueFactory(GeneralTabViewModel.fontSizeValueFactory);
        fontSize.setEditable(true);
        fontSize.setMaxWidth(100.0);
        fontSize.getEditor().setAlignment(Pos.CENTER_RIGHT);
        fontSize.getEditor().textProperty().bindBidirectional(viewModel.fontSizeProperty());
        fontSize.getEditor().setTextFormatter(fontSizeFormatter);
        return fontSize;
    }
}
