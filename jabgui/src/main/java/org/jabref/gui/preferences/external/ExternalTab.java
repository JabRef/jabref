package org.jabref.gui.preferences.external;

import javafx.beans.property.StringProperty;
import javafx.scene.control.TextArea;

import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.push.GuiPushToApplication;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;

import org.controlsfx.control.SearchableComboBox;

public class ExternalTab extends AbstractPreferenceTabView<ExternalTabViewModel> {

    // Kept as fields so validation can be attached after the grids are assembled.

    public ExternalTab() {
        this.viewModel = new ExternalTabViewModel(
                dialogService,
                preferences.getExternalApplicationsPreferences(),
                preferences.getPushToApplicationPreferences(),
                preferences.getFilePreferences());
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("External programs");
    }

    private void buildView() {
        String commandHint = Localization.lang("Enter the application on the first line and one argument per following line. Use %DIR% for the library directory.");

        setContent(form()

                .section(Localization.lang("Sending of emails"), emails -> emails
                        .stringField(Localization.lang("Subject for sending an email with references"), viewModel.eMailReferenceSubjectProperty())
                        .stringField(Localization.lang("Email address for sending to Kindle"), viewModel.kindleEmailProperty())
                        .checkbox(Localization.lang("Automatically open folders of attached files"), viewModel.autoOpenAttachedFoldersProperty()))

                .section(Localization.lang("Push applications"), push -> push
                        .field(Localization.lang("Application to push entries to"), buildPushToApplicationCombo(),
                                application -> application.attach(ControlHelper.narrowIconButton(IconTheme.JabRefIcons.PREFERENCES,
                                        Localization.lang("Application settings"), viewModel::pushToApplicationSettings)))
                        .stringField(Localization.lang("Cite command"), viewModel.citeCommandProperty(),
                                cite -> cite.validate(viewModel.citeCommandValidationStatus())
                                            .help(StandardActions.HELP_PUSH_TO_APPLICATION, HelpFile.PUSH_TO_APPLICATION)
                                            .attach(ControlHelper.narrowIconButton(IconTheme.JabRefIcons.REFRESH,
                                                    Localization.lang("Reset to default"), viewModel::resetCiteCommandToDefault))))

                .section(Localization.lang("Custom applications"), applications -> applications
                        .checkbox(Localization.lang("Use custom terminal emulator"), viewModel.useCustomTerminalProperty(),
                                terminal -> terminal.enabledWhenOn(command -> command
                                        .field(Localization.lang("Application"), commandArea(viewModel.customTerminalCommandProperty()),
                                                field -> field.description(commandHint)
                                                              .validate(viewModel.terminalCommandValidationStatus())
                                                              .grow()
                                                              .browse(viewModel::customTerminalBrowse))))
                        .checkbox(Localization.lang("Use custom file browser"), viewModel.useCustomFileBrowserProperty(),
                                fileBrowser -> fileBrowser.enabledWhenOn(command -> command
                                        .field(Localization.lang("Application"), commandArea(viewModel.customFileBrowserCommandProperty()),
                                                field -> field.description(commandHint)
                                                              .validate(viewModel.fileBrowserCommandValidationStatus())
                                                              .grow()
                                                              .browse(viewModel::customFileBrowserBrowse)))))

                .build());
    }

    private SearchableComboBox<GuiPushToApplication> buildPushToApplicationCombo() {
        SearchableComboBox<GuiPushToApplication> pushToApplicationCombo = new SearchableComboBox<>();
        new ViewModelListCellFactory<GuiPushToApplication>()
                .withText(GuiPushToApplication::getDisplayName)
                .withIcon(GuiPushToApplication::getApplicationIcon)
                .install(pushToApplicationCombo);
        pushToApplicationCombo.itemsProperty().bind(viewModel.pushToApplicationsListProperty());
        pushToApplicationCombo.valueProperty().bindBidirectional(viewModel.selectedPushToApplication());
        return pushToApplicationCombo;
    }

    /// A command with its arguments, one per line, as the custom application settings expect it.
    private static TextArea commandArea(StringProperty command) {
        TextArea area = new TextArea();
        area.setPrefRowCount(3);
        area.textProperty().bindBidirectional(command);
        return area;
    }
}
