package org.jabref.gui.git;

import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangeList;
import org.jabref.gui.collab.DatabaseChangeResolverFactory;
import org.jabref.gui.collab.DatabaseChangesResolverDialog;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;
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
    @Inject
    private UndoManager undoManager;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public GitCommitDialogView() {
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    @FXML
    private void initialize() {
        setTitle(Localization.lang("Git Commit"));
        this.viewModel = new GitCommitDialogViewModel(stateManager, dialogService, taskExecutor, gitHandlerRegistry, preferences, preferences.getImportFormatPreferences(), fileUpdateMonitor);
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
            commitMessage.requestFocus();
            // [impl->req~textinput.clipboard.autofocus~1]
            final String clipboardText = ClipBoardManager.getContents().trim();
            if (!StringUtil.isBlank(clipboardText)) {
                commitMessage.setText(clipboardText);
                commitMessage.selectAll();
            }
        });
    }

    @FXML
    private void showDiff() {
        viewModel.diffTask()
                 .onSuccess(this::openDiffDialog)
                 .onFailure(ex -> dialogService.showErrorDialogAndWait(
                         Localization.lang("Git Diff Failed"),
                         ex.getMessage(),
                         ex
                 ))
                 .executeWith(taskExecutor);
    }

    private void openDiffDialog(GitCommitDialogViewModel.DiffDatabases diffDatabases) {
        BibDatabaseContext savedDatabase = diffDatabases.savedDatabase();
        BibDatabaseContext headDatabase = diffDatabases.headDatabase();

        DatabaseChangeResolverFactory changeResolverFactory = new DatabaseChangeResolverFactory(dialogService, savedDatabase, preferences, stateManager);
        List<DatabaseChange> changes = DatabaseChangeList.compareAndGetChanges(savedDatabase, headDatabase, changeResolverFactory);

        DatabaseChangesResolverDialog diffDialog = new DatabaseChangesResolverDialog(changes, savedDatabase, Localization.lang("Diff View"));
        dialogService.showCustomDialogAndWait(diffDialog);

        undoManager.addEdit(Localization.lang("Merged external changes"), compoundEdit ->
                changes.stream()
                       .filter(DatabaseChange::isAccepted)
                       .forEach(change -> change.applyChange(compoundEdit)));
    }
}
