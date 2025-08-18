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
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import jakarta.inject.Inject;

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

    private GitShareToGitHubDialogViewModel viewModel;

    @Inject
    private DialogService dialogService;

    @Inject
    private StateManager stateManager;

    @Inject
    private TaskExecutor taskExecutor;

    @Inject
    private GuiPreferences preferences;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public GitShareToGitHubDialogView() {
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    @FXML
    private void initialize() {
        this.viewModel = new GitShareToGitHubDialogViewModel(preferences.getGitPreferences(), stateManager, dialogService, taskExecutor);

        this.setTitle(Localization.lang("Share this Library to GitHub"));

        ControlHelper.setAction(shareButton, this.getDialogPane(), _ -> shareToGitHub());
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
                        preferences.getExternalApplicationsPreferences()
                )
        );

        Tooltip.install(patHelpIcon, patHelpTooltip);
        patHelpIcon.setOnMouseClicked(e ->
                NativeDesktop.openBrowserShowPopup(
                        GITHUB_PAT_DOCS_URL,
                        dialogService,
                        preferences.getExternalApplicationsPreferences()
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
        viewModel.shareToGitHub(() -> this.close());
    }
}
