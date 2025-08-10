package org.jabref.gui.git;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class GitShareToGitHubDialogView extends BaseDialog<Void> {
    private static final String GITHUB_PAT_DOCS_URL =
            "https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens";

    private static final String GITHUB_NEW_REPO_URL = "https://github.com/new";

    @FXML private TextField repositoryUrl;
    @FXML private TextField username;
    @FXML private PasswordField personalAccessToken;
    @FXML private ButtonType shareButton;
    @FXML private Label patHelpIcon;
    @FXML private Tooltip patHelpTooltip;
    @FXML private CheckBox rememberSettingsCheck;
    @FXML private Label repoHelpIcon;
    @FXML private Tooltip repoHelpTooltip;

    private final GitShareToGitHubDialogViewModel viewModel;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;

    public GitShareToGitHubDialogView(StateManager stateManager, DialogService dialogService, GuiPreferences preferences) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.preferences = preferences;

        this.setTitle(Localization.lang("Share this library to GitHub"));
        this.viewModel = new GitShareToGitHubDialogViewModel(stateManager, dialogService);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
        ControlHelper.setAction(shareButton, this.getDialogPane(), event -> shareToGitHub());
    }

    @FXML
    private void initialize() {
        patHelpTooltip.setText(
                Localization.lang("Need help?") + "\n" +
                        Localization.lang("Click to open GitHub Personal Access Token documentation")
        );

        username.setPromptText(Localization.lang("Your GitHub username"));
        personalAccessToken.setPromptText(Localization.lang("PAT with repo access"));

        repoHelpTooltip.setText(
                Localization.lang("Create an empty repository on GitHub, then copy the HTTPS URL (ends with .git). Click to open GitHub.")
        );
        Tooltip.install(repoHelpIcon, repoHelpTooltip);
        repoHelpIcon.setOnMouseClicked(e ->
                NativeDesktop.openBrowserShowPopup(
                        GITHUB_NEW_REPO_URL,
                        dialogService,
                        this.preferences.getExternalApplicationsPreferences()
                )
        );

        Tooltip.install(patHelpIcon, patHelpTooltip);
        patHelpIcon.setOnMouseClicked(e ->
                NativeDesktop.openBrowserShowPopup(
                        GITHUB_PAT_DOCS_URL,
                        dialogService,
                        this.preferences.getExternalApplicationsPreferences()
                )
        );

        repositoryUrl.textProperty().bindBidirectional(viewModel.repositoryUrlProperty());
        username.textProperty().bindBidirectional(viewModel.githubUsernameProperty());
        personalAccessToken.textProperty().bindBidirectional(viewModel.githubPatProperty());
        rememberSettingsCheck.selectedProperty().bindBidirectional(viewModel.rememberSettingsProperty());
    }

    @FXML
    private void shareToGitHub() {
        boolean success = viewModel.shareToGitHub();
        if (success) {
            this.close();
        }
    }
}
