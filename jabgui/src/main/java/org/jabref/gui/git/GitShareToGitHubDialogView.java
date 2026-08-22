package org.jabref.gui.git;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class GitShareToGitHubDialogView extends BaseDialog<Void> {

    @FXML private Label description;

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

    public GitShareToGitHubDialogView() {
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    @FXML
    private void initialize() {
        this.viewModel = new GitShareToGitHubDialogViewModel(preferences.getGitPreferences(), stateManager, dialogService, taskExecutor, gitHandlerRegistry);

        this.setTitle(Localization.lang("Share this Library to GitHub"));
        description.setText(Localization.lang("This will commit the library and push it to %0.", viewModel.getRepositoryUrl()));

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
    }

    @FXML
    private void shareToGitHub() {
        viewModel.shareToGitHub(this::close);
    }
}
