package org.jabref.gui.autosaveandbackup;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jabref.gui.LibraryTab;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.CoarseChangeFilter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupManagerJGit {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupManagerJGit.class);

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
        /*

        il faut initialiser needsBackup
         */
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

    public static void restoreBackup(Path originalPath, Path backupDir, ObjectId objectId) {
        try {

            Git git = Git.open(backupDir.toFile());
            git.checkout().setName(objectId.getName()).call();
            LOGGER.info("Restored backup from Git repository");
        } catch (IOException | GitAPIException e) {
            LOGGER.error("Error while restoring the backup", e);
        }
    }

    /*
        compare what is in originalPath and last commit
        */

    public static boolean backupGitDiffers(Path originalPath, Path backupDir) throws IOException, GitAPIException {

        File repoDir = backupDir.toFile();
        Repository repository = new FileRepositoryBuilder()
                .setGitDir(new File(repoDir, ".git"))
                .build();
        try (Git git = new Git(repository)) {
            ObjectId headCommitId = repository.resolve("HEAD"); // to get the latest commit id
            if (headCommitId == null) {
                // No commits in the repository, so there's no previous backup
                return true;
            }
            git.add().addFilepattern(originalPath.getFileName().toString()).call();
            String relativePath = backupDir.relativize(originalPath).toString();
            List<DiffEntry> diffs = git.diff()
                                       .setPathFilter(PathFilter.create(relativePath)) // Utiliser PathFilter ici
                                       .call();
            return !diffs.isEmpty();
        }
    }

    @SuppressWarnings("checkstyle:RegexpMultiline")
    public void showDiffersJGit(Path originalPath, Path backupDir, String CommitId) throws IOException, GitAPIException {

        File repoDir = backupDir.toFile();
        Repository repository = new FileRepositoryBuilder()
                .setGitDir(new File(repoDir, ".git"))
                .build();
        /*
        il faut une classe qui affiche les dix dernier backup avec les data: date/ size / number of entries
         */

        ObjectId oldCommit = repository.resolve(CommitId);
        ObjectId newCommit = repository.resolve("HEAD");

        FileOutputStream fos = new FileOutputStream(FileDescriptor.out);
        DiffFormatter diffFr = new DiffFormatter(fos);
        diffFr.setRepository(repository);
        diffFr.scan(oldCommit, newCommit);
    }



    /*

    faire une methode qui accepte commit id et retourne les diff differences avec la version actuelle
    methode qui renvoie n derniers indice de commit
    methode ayant idcommit retourne data

     */


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
