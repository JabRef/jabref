package org.jabref.gui.git;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import jakarta.inject.Inject;

public class GitShareToGitHubDialogView extends BaseDialog<Void> {
    private static final String GITHUB_NEW_REPO_URL = "https://github.com/new";

    @FXML private Label credentialsHint;
    @FXML private TextField repositoryUrl;
    @FXML private ButtonType shareButton;
    @FXML private Button checkGitHubAccessButton;
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
    private GitHandlerRegistry gitHandlerRegistry;

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
        this.viewModel = new GitShareToGitHubDialogViewModel(preferences.getGitPreferences(), stateManager, dialogService, taskExecutor, gitHandlerRegistry);

        this.setTitle(Localization.lang("Share this Library to GitHub"));
        checkGitHubAccessButton.setText(Localization.lang("Check GitHub access"));
        credentialsHint.setText(Localization.lang("The GitHub credentials configured in Preferences > Git are used."));

        // See "javafx.md"
        this.setResultConverter(button -> {
            if (button != ButtonType.CANCEL) {
                // We do not want to use "OK", but we want to use a custom text instead.
                // JavaFX does not allow to alter the text of the "OK" button.
                // Therefore, we used another button type.
                // Since we have only two buttons, we can check for non-cancel here.
                shareToGitHub();
            }
            return null;
        });

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

        repositoryUrl.textProperty().bindBidirectional(viewModel.repositoryUrlProperty());

        viewModel.setValues();

        Platform.runLater(() -> {
            visualizer.setDecoration(new IconValidationDecorator());

            visualizer.initVisualization(viewModel.repositoryUrlValidation(), repositoryUrl, true);

            // The button is null in initialize, so it has to be looked up afterwards
            Button share = (Button) this.getDialogPane().lookupButton(shareButton);
            share.disableProperty().bind(viewModel.repositoryUrlValidation().validProperty().not());
        });
    }

    @FXML
    private void shareToGitHub() {
        viewModel.shareToGitHub(this::onShared);
    }

    /// The library is inside a Git repository only after it has been shared, so the scheduler
    /// cannot have been started when the library was opened
    private void onShared() {
        stateManager.activeTabProperty().get().ifPresent(tab ->
                GitPullScheduler.start(tab.getBibDatabaseContext(),
                        dialogService,
                        preferences,
                        stateManager,
                        taskExecutor,
                        gitHandlerRegistry,
                        tab::isModified));
        close();
    }

    @FXML
    private void checkGitHubAccess() {
        viewModel.checkGitHubAccess();
    }
}
