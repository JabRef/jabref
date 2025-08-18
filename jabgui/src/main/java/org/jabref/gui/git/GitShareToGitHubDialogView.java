package org.jabref.gui.git;

import javafx.application.Platform;
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
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

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

    private final StateManager stateManager;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public GitShareToGitHubDialogView(StateManager stateManager, DialogService dialogService, TaskExecutor taskExecutor, ExternalApplicationsPreferences externalApplicationsPreferences, GitPreferences gitPreferences) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.externalApplicationsPreferences = externalApplicationsPreferences;

        this.setTitle(Localization.lang("Share this library to GitHub"));
        this.viewModel = new GitShareToGitHubDialogViewModel(gitPreferences, stateManager);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
        ControlHelper.setAction(shareButton, this.getDialogPane(), _ -> shareToGitHub());
    }

    @FXML
    private void initialize() {
        patHelpTooltip.setText(
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
                        externalApplicationsPreferences
                )
        );

        Tooltip.install(patHelpIcon, patHelpTooltip);
        patHelpIcon.setOnMouseClicked(e ->
                NativeDesktop.openBrowserShowPopup(
                        GITHUB_PAT_DOCS_URL,
                        dialogService,
                        externalApplicationsPreferences
                )
        );

        repositoryUrl.textProperty().bindBidirectional(viewModel.repositoryUrlProperty());
        username.textProperty().bindBidirectional(viewModel.usernameProperty());
        personalAccessToken.textProperty().bindBidirectional(viewModel.patProperty());
        rememberSettingsCheck.selectedProperty().bindBidirectional(viewModel.rememberPatProperty());

        viewModel.setValues();

        Platform.runLater(() -> {
            visualizer.setDecoration(new IconValidationDecorator());

            visualizer.initVisualization(viewModel.repositoryUrlValidation(), repositoryUrl, true);
            visualizer.initVisualization(viewModel.githubUsernameValidation(), username, true);
            visualizer.initVisualization(viewModel.githubPatValidation(), personalAccessToken, true);
        });
    }

    @FXML
    private void shareToGitHub() {
        BackgroundTask.wrap(() -> {
            viewModel.shareToGitHub();
            return true;
        })
        .onSuccess(result -> {
            dialogService.showInformationDialogAndWait(
                    Localization.lang("GitHub Share"),
                    Localization.lang("Successfully pushed to GitHub.")
            );
            this.close();
        })
      .onFailure(e -> dialogService.showErrorDialogAndWait(Localization.lang("GitHub share failed"), e.getMessage(), e))
      .executeWith(taskExecutor);
    }
}
