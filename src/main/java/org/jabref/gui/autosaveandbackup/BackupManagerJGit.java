package org.jabref.gui.autosaveandbackup;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jabref.gui.LibraryTab;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.CoarseChangeFilter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

public class BackupManagerJGit {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupManager.class);

    private static final int DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS = 19;

    private static Set<BackupManagerJGit> runningInstances = new HashSet<BackupManagerJGit>();

    private final BibDatabaseContext bibDatabaseContext;
    private final CliPreferences preferences;
    private final ScheduledThreadPoolExecutor executor;
    private final CoarseChangeFilter changeFilter;
    private final BibEntryTypesManager entryTypesManager;
    private final LibraryTab libraryTab;
    private final Git git;

    private boolean needsBackup = false;

    BackupManagerJGit(LibraryTab libraryTab, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager, CliPreferences preferences) throws IOException, GitAPIException {
        this.bibDatabaseContext = bibDatabaseContext;
        this.entryTypesManager = entryTypesManager;
        this.preferences = preferences;
        this.executor = new ScheduledThreadPoolExecutor(2);
        this.libraryTab = libraryTab;

        changeFilter = new CoarseChangeFilter(bibDatabaseContext);
        changeFilter.registerListener(this);

        // Initialize Git repository
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        git = new Git(builder.setGitDir(new File(preferences.getFilePreferences().getBackupDirectory().toFile(), ".git"))
                             .readEnvironment()
                             .findGitDir()
                             .build());
        if (git.getRepository().getObjectDatabase().exists()) {
            LOGGER.info("Git repository already exists");
        } else {
            git.init().call();
            LOGGER.info("Initialized new Git repository");
        }
    }

    public static BackupManagerJGit startJGit(LibraryTab libraryTab, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager, CliPreferences preferences) throws IOException, GitAPIException {
        BackupManagerJGit backupManagerJGit = new BackupManagerJGit(libraryTab, bibDatabaseContext, entryTypesManager, preferences);
        backupManagerJGit.startBackupTaskJGit(preferences.getFilePreferences().getBackupDirectory());
        runningInstances.add(backupManagerJGit);
        return backupManagerJGit;
    }

    public static void shutdownJGit(BibDatabaseContext bibDatabaseContext, Path backupDir, boolean createBackup) {
        runningInstances.stream().filter(instance -> instance.bibDatabaseContext == bibDatabaseContext).forEach(backupManager -> backupManager.shutdownJGit(backupDir, createBackup));
        runningInstances.removeIf(instance -> instance.bibDatabaseContext == bibDatabaseContext);
    }

    private void startBackupTaskJGit(Path backupDir) {
        executor.scheduleAtFixedRate(
                () -> {
                    try {
                        performBackup(backupDir);
                    } catch (IOException | GitAPIException e) {
                        LOGGER.error("Error during backup", e);
                    }
                },
                DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS,
                DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS,
                TimeUnit.SECONDS);
    }

    private void performBackup(Path backupDir) throws IOException, GitAPIException {
        if (!needsBackup) {
            return;
        }

        // Add and commit changes
        git.add().addFilepattern(".").call();
        RevCommit commit = git.commit().setMessage("Backup at " + System.currentTimeMillis()).call();
        LOGGER.info("Committed backup: {}", commit.getId());

        // Reset the backup flag
        this.needsBackup = false;
    }

    public static void restoreBackup(Path originalPath, Path backupDir) {
        try {
            Git git = Git.open(backupDir.toFile());
            git.checkout().setName("HEAD").call();
            LOGGER.info("Restored backup from Git repository");
        } catch (IOException | GitAPIException e) {
            LOGGER.error("Error while restoring the backup", e);
        }
    }

    private void shutdownJGit(Path backupDir, boolean createBackup) {
        changeFilter.unregisterListener(this);
        changeFilter.shutdown();
        executor.shutdown();

        if (createBackup) {
            try {
                performBackup(backupDir);
            } catch (IOException | GitAPIException e) {
                LOGGER.error("Error during shutdown backup", e);
            }
        }
    }
}
