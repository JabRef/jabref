package org.jabref.gui.git;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import jakarta.inject.Inject;

public class GitCommitDialogView extends BaseDialog<Void> {

    @FXML private TextArea commitMessage;
    @FXML private ButtonType commitButton;
    @FXML private Button showDiffButton;

    private GitCommitDialogViewModel viewModel;

    @Inject
    private GuiPreferences preferences;

    private ImportFormatPreferences importFormatPreferences;

    @Inject
    private StateManager stateManager;

    @Inject
    private DialogService dialogService;

    @Inject
    private TaskExecutor taskExecutor;
    @Inject
    private GitHandlerRegistry gitHandlerRegistry;
    @Inject
    private FileUpdateMonitor fileUpdateMonitor;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public GitCommitDialogView() {
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    @FXML
    private void initialize() {
        setTitle(Localization.lang("Git Commit"));
        this.viewModel = new GitCommitDialogViewModel(stateManager, dialogService, taskExecutor, gitHandlerRegistry, preferences, importFormatPreferences, fileUpdateMonitor);
        commitMessage.textProperty().bindBidirectional(viewModel.commitMessageProperty());
        commitMessage.setPromptText(Localization.lang("Enter commit message here"));

        showDiffButton.setOnAction(_ -> showDiff());

        this.setResultConverter(button -> {
            if (button != ButtonType.CANCEL) {
                viewModel.commit(() -> this.close());
            }
            return null;
        });

        Platform.runLater(() -> {
            visualizer.setDecoration(new IconValidationDecorator());
            visualizer.initVisualization(viewModel.commitMessageValidation(), commitMessage, true);
        });
    }

    @FXML
    private void showDiff() {
        viewModel.diffTask()
                 .onSuccess(diffs -> dialogService.showCustomDialogAndWait(new GitDiffDialogView(diffs)))
                 .onFailure(ex -> dialogService.showErrorDialogAndWait(
                         Localization.lang("Git Diff Failed"),
                         ex.getMessage(),
                         ex
                 ))
                 .executeWith(taskExecutor);
    }
}
