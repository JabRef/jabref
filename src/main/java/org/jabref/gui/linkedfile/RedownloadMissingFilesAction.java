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

    private boolean shouldContinue = true;

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
        init();
        redownloadMissing();
    }

    public void init() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        databaseContext = stateManager.getActiveDatabase().get();
        boolean confirm = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Redownload missing files"),
                Localization.lang("Redownload missing files for current library?"));
        if (!confirm) {
            shouldContinue = false;
            return;
        }
    }

    private void redownloadMissing() {
        if (!shouldContinue || stateManager.getActiveDatabase().isEmpty()) {
            return;
        }
        LOGGER.info("Redownloading missing files");
        stateManager.getActiveDatabase().get().getEntries().forEach(entry -> {
            entry.getFiles().forEach(linkedFile -> {
                if (linkedFile.isOnlineLink() || linkedFile.getSourceUrl().isEmpty()) {
                    return;
                }

                Optional<Path> path = FileUtil.find(databaseContext, linkedFile.getLink(), filePreferences);
                if (path.isPresent() && Files.exists(path.get())) {
                    return;
                }

                DownloadLinkedFileAction downloadAction = new DownloadLinkedFileAction(databaseContext, entry,
                        linkedFile, linkedFile.getSourceUrl(), dialogService, filePreferences, taskExecutor);
                downloadAction.execute();
            });
        });
    }
}
