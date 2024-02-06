package org.jabref.gui.linkedfile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class RedownloadMissingFilesAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryTab.class);

    private final StateManager stateManager;
    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final TaskExecutor taskExecutor;

    private BibDatabaseContext databaseContext;

    public RedownloadMissingFilesAction(StateManager stateManager,
                                        DialogService dialogService,
                                        FilePreferences filePreferences,
                                        TaskExecutor taskExecutor) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.filePreferences = filePreferences;
        this.taskExecutor = taskExecutor;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isPresent()) {
            databaseContext = stateManager.getActiveDatabase().get();
            boolean confirm = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Redownload missing files"),
                    Localization.lang("Redownload missing files for current library?"));
            if (!confirm) {
                return;
            }
            redownloadMissing(stateManager.getActiveDatabase().get());
        }
    }

    private void redownloadMissing(BibDatabaseContext databaseContext) {
        LOGGER.info("Redownloading missing files");
        databaseContext.getEntries().forEach(entry -> {
            entry.getFiles().forEach(linkedFile -> {
                if (linkedFile.isOnlineLink() || linkedFile.getSourceUrl().isEmpty()) {
                    return;
                }

                Optional<Path> path = FileUtil.find(this.databaseContext, linkedFile.getLink(), filePreferences);
                if (path.isPresent() && Files.exists(path.get())) {
                    return;
                }
                String fileName = Path.of(linkedFile.getLink()).getFileName().toString();

                DownloadLinkedFileAction downloadAction = new DownloadLinkedFileAction(this.databaseContext, entry,
                        linkedFile, linkedFile.getSourceUrl(), dialogService, filePreferences, taskExecutor, fileName);
                downloadAction.execute();
            });
        });
    }
}
