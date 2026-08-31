package org.jabref.gui.git;

import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangeList;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.validation.ValidationVisualizer;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class GitCommitDialogView extends BaseDialog<Void> {

    @FXML private TextArea commitMessage;
    @FXML private ButtonType commitButton;
    @FXML private ButtonType commitAndPushButton;

    private GitCommitDialogViewModel viewModel;

    @Inject private StateManager stateManager;
    @Inject private DialogService dialogService;
    @Inject private TaskExecutor taskExecutor;
    @Inject private GitHandlerRegistry gitHandlerRegistry;
    @Inject private GuiPreferences preferences;
    @Inject private FileUpdateMonitor fileUpdateMonitor;

    public GitCommitDialogView() {
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    @FXML
    private void initialize() {
        setTitle(Localization.lang("Git commit"));
        this.viewModel = new GitCommitDialogViewModel(
                stateManager,
                dialogService,
                taskExecutor,
                gitHandlerRegistry,
                preferences.getImportFormatPreferences(),
                fileUpdateMonitor);

        commitMessage.textProperty().bindBidirectional(viewModel.commitMessageProperty());
        commitMessage.setPromptText(Localization.lang("Enter commit message here"));

        this.setResultConverter(button -> {
            if (button != ButtonType.CANCEL) {
                if (button == commitAndPushButton) {
                    viewModel.commitAndPush(this::close);
                } else {
                    viewModel.commit(this::close);
                }
            }
            return null;
        });

        new ValidationVisualizer().initVisualization(viewModel.commitMessageProperty(), commitMessage);

        Platform.runLater(() -> {
            commitMessage.requestFocus();
            // [impl->req~textinput.clipboard.autofocus~1]
            final String clipboardText = ClipBoardManager.getContents().trim();
            if (!StringUtil.isBlank(clipboardText)) {
                commitMessage.setText(clipboardText);
                commitMessage.selectAll();
            }
        });
    }

    // [impl->req~ux.git-commit.preview-current-library~1]
    @FXML
    private void showDiff() {
        viewModel.diffTask()
                 .onSuccess(this::openDiffDialog)
                 .onFailure(ex -> dialogService.showErrorDialogAndWait(
                         Localization.lang("Git diff failed"),
                         ex.getMessage(),
                         ex
                 ))
                 .executeWith(taskExecutor);
    }

    private void openDiffDialog(GitCommitDialogViewModel.DiffDatabases diffDatabases) {
        List<DatabaseChange> changes = DatabaseChangeList.compareAndGetChanges(
                diffDatabases.headDatabase(),
                diffDatabases.workingTreeDatabase(),
                null);

        dialogService.showCustomDialogAndWait(
                new GitDiffDialogView(changes, diffDatabases.headDatabase(), diffDatabases.workingTreeDatabase()));
    }
}
