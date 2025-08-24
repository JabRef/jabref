package org.jabref.gui.git;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import jakarta.inject.Inject;

public class GitCommitDialogView extends BaseDialog<Void> {

    @FXML private TextArea commitMessage;
    @FXML private ButtonType commitButton;

    private GitCommitDialogViewModel viewModel;

    @Inject
    private StateManager stateManager;

    @Inject
    private DialogService dialogService;

    @Inject
    private GuiPreferences preferences;
    @Inject
    private TaskExecutor taskExecutor;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public GitCommitDialogView() {
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    @FXML
    private void initialize() {
        setTitle(Localization.lang("Git Commit"));
        this.viewModel = new GitCommitDialogViewModel(stateManager, dialogService, preferences, taskExecutor);

        commitMessage.textProperty().bindBidirectional(viewModel.commitMessageProperty());
        commitMessage.setPromptText(Localization.lang("Enter commit message here"));

        this.setResultConverter(button -> {
            if (button != ButtonType.CANCEL) {
                viewModel.commit(() -> this.close());
            }
            return null;
        });
    }
}
