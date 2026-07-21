package org.jabref.gui.preferences.general;

import java.util.regex.Pattern;

import javafx.geometry.Pos;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextFormatter;
import javafx.util.converter.IntegerStringConverter;

import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.forms.AbstractFormTabView;
import org.jabref.gui.theme.ThemeTypes;
import org.jabref.http.manager.HttpServerManager;
import org.jabref.languageserver.controller.LanguageServerController;
import org.jabref.logic.UiMessageHandler;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.remote.server.RemoteListenerServerManager;
import org.jabref.model.database.BibDatabaseMode;

import com.airhacks.afterburner.injection.Injector;

public class GeneralTab extends AbstractFormTabView<GeneralTabViewModel> {

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
        getChildren().add(form()
                .title(Localization.lang("General"))

                .section(Localization.lang("Appearance"))
                .searchableCombo(Localization.lang("Language"),
                        viewModel.languagesListProperty(), viewModel.selectedLanguageProperty(), Language::getDisplayName)
                .combo(Localization.lang("Visual theme"),
                        viewModel.themesListProperty(), viewModel.selectedThemeProperty(), ThemeTypes::getDisplayName,
                        theme -> theme.disableWhen(viewModel.themeSyncOsProperty())
                                      .validate(viewModel.themeValidationStatus()))
                .checkbox(Localization.lang("Use System Preference"), viewModel.themeSyncOsProperty())
                .browseField(null,
                        viewModel.customPathToThemeProperty(), viewModel::importCSSFile,
                        path -> path.disableWhen(viewModel.selectedThemeProperty().isNotEqualTo(ThemeTypes.CUSTOM))
                                    .validate(viewModel.customPathToThemeValidationStatus()))
                .checkbox(Localization.lang("Override default font settings"), viewModel.fontOverrideProperty())
                .field(Localization.lang("Size"), buildFontSizeSpinner(),
                        size -> size.validate(viewModel.fontSizeValidationStatus()))
                .hyperlink(Localization.lang("Get more themes..."), viewModel::openBrowser)

                .section(Localization.lang("User interface"))
                .checkbox(Localization.lang("Open last edited libraries on startup"), viewModel.openLastStartupProperty())
                .checkbox(Localization.lang("Show advanced hints (i.e. helpful tooltips, suggestions and explanation)"), viewModel.showAdvancedHintsProperty())
                .checkbox(Localization.lang("Show confirmation dialog when deleting entries"), viewModel.confirmDeleteProperty())
                .checkbox(Localization.lang("Ask whether to include cross-references when copying to another library"), viewModel.shouldAskForIncludingCrossReferences())
                .checkbox(Localization.lang("Hide tab bar when single library is present"), viewModel.confirmHideTabBarProperty())
                .checkbox(Localization.lang("Do not show donation prompt again"), viewModel.donationNeverShowProperty())
                .checkbox(Localization.lang("Experimental search (Postgres)"), viewModel.usePostgresSearchProperty())

                .section(Localization.lang("Single instance"))
                .checkWithField(Localization.lang("Enforce single JabRef instance (and allow remote operations) using port"),
                        viewModel.remoteServerProperty(), viewModel.remotePortProperty(),
                        port -> port.validate(viewModel.remotePortValidationStatus())
                                    .help(HelpFile.REMOTE))

                .section(Localization.lang("HTTP Server"))
                .checkWithField(Localization.lang("Enable HTTP Server (e.g., for JabMap) on port"),
                        viewModel.enableHttpServerProperty(), viewModel.httpPortProperty(),
                        port -> port.validate(viewModel.httpPortValidationStatus()))
                .checkbox(Localization.lang("Skip import dialog for entries received from browser extensions"), viewModel.directHttpImportProperty())

                .section(Localization.lang("LSP Server"))
                .checkWithField(Localization.lang("Enable LSP Server on port"),
                        viewModel.enableLanguageServerProperty(), viewModel.languageServerPortProperty(),
                        port -> port.validate(viewModel.languageServerPortValidationStatus()))

                .section(Localization.lang("Libraries"))
                .combo(Localization.lang("Default library mode"),
                        viewModel.biblatexModeListProperty(), viewModel.selectedBiblatexModeProperty(), BibDatabaseMode::getFormattedName)

                .section(Localization.lang("Saving"))
                .checkbox(Localization.lang("Always reformat library on save and export"), viewModel.alwaysReformatBibProperty())
                .checkbox(Localization.lang("Autosave local libraries"), viewModel.autosaveLocalLibrariesProperty(),
                        autosave -> autosave.help(HelpFile.AUTOSAVE))
                .checkbox(Localization.lang("Create backup"), viewModel.createBackupProperty())
                .browseField(null, viewModel.backupDirectoryProperty(), viewModel::backupFileDirBrowse,
                        directory -> directory.disableWhen(viewModel.createBackupProperty().not()))

                .build());
    }

    private Spinner<Integer> buildFontSizeSpinner() {
        Spinner<Integer> fontSize = new Spinner<>();
        fontSize.setValueFactory(GeneralTabViewModel.fontSizeValueFactory);
        fontSize.getStyleClass().add("fontsizeSpinner");
        fontSize.setEditable(true);
        fontSize.setMaxWidth(100.0);
        fontSize.getEditor().setAlignment(Pos.CENTER_RIGHT);
        fontSize.getEditor().textProperty().bindBidirectional(viewModel.fontSizeProperty());
        fontSize.getEditor().setTextFormatter(fontSizeFormatter);
        fontSize.disableProperty().bind(viewModel.fontOverrideProperty().not());
        return fontSize;
    }
}
