package org.jabref.gui.autosaveandbackup;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
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
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupManagerGit {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupManagerGit.class);

    private static final int DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS = 19;

    private static Set<BackupManagerGit> runningInstances = new HashSet<BackupManagerGit>();

    private final BibDatabaseContext bibDatabaseContext;
    private final CliPreferences preferences;
    private final ScheduledThreadPoolExecutor executor;
    private final CoarseChangeFilter changeFilter;
    private final BibEntryTypesManager entryTypesManager;
    private final LibraryTab libraryTab;
    private final Git git;

    private boolean needsBackup = false;

    BackupManagerGit(LibraryTab libraryTab, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager, CliPreferences preferences) throws IOException, GitAPIException {
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
            Git.init().call();
            LOGGER.info("Initialized new Git repository");
        }
    }

    /**
     * Starts a new BackupManagerGit instance and begins the backup task.
     *
     * @param libraryTab the library tab
     * @param bibDatabaseContext the BibDatabaseContext to be backed up
     * @param entryTypesManager the BibEntryTypesManager
     * @param preferences the CLI preferences
     * @param originalPath the original path of the file to be backed up
     * @return the started BackupManagerGit instance
     * @throws IOException if an I/O error occurs
     * @throws GitAPIException if a Git API error occurs
     */

    public BackupManagerGit start(LibraryTab libraryTab, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager, CliPreferences preferences, Path originalPath) throws IOException, GitAPIException {
        BackupManagerGit BackupManager = new BackupManagerGit(libraryTab, bibDatabaseContext, entryTypesManager, preferences);
        this.startBackupTask(preferences.getFilePreferences().getBackupDirectory(), originalPath);
        runningInstances.add(BackupManager);
        return BackupManager;
    }

    /**
     * Shuts down the BackupManagerGit instances associated with the given BibDatabaseContext.
     *
     * @param bibDatabaseContext the BibDatabaseContext
     * @param backupDir the backup directory
     * @param createBackup whether to create a backup before shutting down
     * @param originalPath the original path of the file to be backed up
     */

    @SuppressWarnings({"checkstyle:NoWhitespaceBefore", "checkstyle:WhitespaceAfter"})
    public static void shutdown(BibDatabaseContext bibDatabaseContext, Path backupDir, boolean createBackup, Path originalPath) {
        runningInstances.stream().filter(instance -> instance.bibDatabaseContext == bibDatabaseContext).forEach(backupManager -> backupManager.shutdownJGit(backupDir, createBackup, originalPath));
        runningInstances.removeIf(instance -> instance.bibDatabaseContext == bibDatabaseContext);
    }

    /**
     * Starts the backup task that periodically checks for changes and commits them to the Git repository.
     *
     * @param backupDir the backup directory
     * @param originalPath the original path of the file to be backed up
     */

    @SuppressWarnings({"checkstyle:WhitespaceAfter", "checkstyle:LeftCurly"})
    private void startBackupTask(Path backupDir, Path originalPath) {
        executor.scheduleAtFixedRate(
                () -> {
                    try {
                        performBackup(backupDir, originalPath);
                    } catch (
                            IOException |
                            GitAPIException e) {
                        LOGGER.error("Error during backup", e);
                    }
                },
                DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS,
                DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS,
                TimeUnit.SECONDS);
    }

    /**
     * Performs the backup by checking for changes and committing them to the Git repository.
     *
     * @param backupDir the backup directory
     * @param originalPath the original path of the file to be backed up
     * @throws IOException if an I/O error occurs
     * @throws GitAPIException if a Git API error occurs
     */

    protected void performBackup(Path backupDir, Path originalPath) throws IOException, GitAPIException {
        /*
        needsBackup must be initialized
         */
        needsBackup = BackupManagerGit.backupGitDiffers(backupDir, originalPath);
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

    /**
     * Restores the backup from the specified commit.
     *
     * @param originalPath the original path of the file to be restored
     * @param backupDir the backup directory
     * @param objectId the commit ID to restore from
     */

    public static void restoreBackup(Path originalPath, Path backupDir, ObjectId objectId) {
        try {
            Git git = Git.open(backupDir.toFile());

            git.checkout().setStartPoint(objectId.getName()).setAllPaths(true).call();
            // Add commits to staging Area
            git.add().addFilepattern(".").call();

            // Commit with a message
            git.commit().setMessage("Restored content from commit: " + objectId.getName()).call();

            LOGGER.info("Restored backup from Git repository and committed the changes");
        } catch (
                IOException |
                GitAPIException e) {
            LOGGER.error("Error while restoring the backup", e);
        }
    }

    /**
     * Checks if there are differences between the original file and the backup.
     *
     * @param originalPath the original path of the file
     * @param backupDir the backup directory
     * @return true if there are differences, false otherwise
     * @throws IOException if an I/O error occurs
     * @throws GitAPIException if a Git API error occurs
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
                return false;
            }
            git.add().addFilepattern(originalPath.getFileName().toString()).call();
            String relativePath = backupDir.relativize(originalPath).toString();
            List<DiffEntry> diffs = git.diff()
                                       .setPathFilter(PathFilter.create(relativePath)) // Utiliser PathFilter ici
                                       .call();
            return !diffs.isEmpty();
        }
    }

    /**
     * Shows the differences between the specified commit and the latest commit.
     *
     * @param originalPath the original path of the file
     * @param backupDir the backup directory
     * @param commitId the commit ID to compare with the latest commit
     * @return a list of DiffEntry objects representing the differences
     * @throws IOException if an I/O error occurs
     * @throws GitAPIException if a Git API error occurs
     */

    public List<DiffEntry> showDiffers(Path originalPath, Path backupDir, String commitId) throws IOException, GitAPIException {

        File repoDir = backupDir.toFile();
        Repository repository = new FileRepositoryBuilder()
                .setGitDir(new File(repoDir, ".git"))
                .build();
        /*
        need a class to show the last ten backups indicating: date/ size/ number of entries
         */

        ObjectId oldCommit = repository.resolve(commitId);
        ObjectId newCommit = repository.resolve("HEAD");

        FileOutputStream fos = new FileOutputStream(FileDescriptor.out);
        DiffFormatter diffFr = new DiffFormatter(fos);
        diffFr.setRepository(repository);
        return diffFr.scan(oldCommit, newCommit);
    }

    // n is a counter incrementing by 1 when the user asks to see older versions (packs of 10)
// and decrements by 1 when the user asks to see the pack of the 10 earlier versions
// the scroll down: n->n+1 ; the scroll up: n->n-1
    public List<RevCommit> retreiveCommits(Path backupDir, int n) throws IOException, GitAPIException {
        List<RevCommit> retrievedCommits = new ArrayList<>();
        // Open Git depository
        try (Repository repository = Git.open(backupDir.toFile()).getRepository()) {
            //  Use RevWalk to go through all commits
            try (RevWalk revWalk = new RevWalk(repository)) {
                // Start from HEAD
                RevCommit startCommit = revWalk.parseCommit(repository.resolve("HEAD"));
                revWalk.markStart(startCommit);

                int count = 0;
                int startIndex = n * 10;
                int endIndex = startIndex + 9;

                for (RevCommit commit : revWalk) {
                    // Ignore commits before starting index
                    if (count < startIndex) {
                        count++;
                        continue;
                    }
                    if (count >= endIndex) {
                        break;
                    }
                    // Add commits to the main list
                    retrievedCommits.add(commit);
                    count++;
                }
            }
        }

        return retrievedCommits;
    }


    /**
     * Retrieves detailed information about the specified commits.
     *
     * @param commits the list of commits to retrieve details for
     * @param backupDir the backup directory
     * @return a list of lists, each containing details about a commit
     * @throws IOException if an I/O error occurs
     * @throws GitAPIException if a Git API error occurs
     */

    @SuppressWarnings("checkstyle:WhitespaceAround")
    public List<List<String>> retrieveCommitDetails(List<RevCommit> commits, Path backupDir) throws IOException, GitAPIException {
        List<List<String>> commitDetails;
        try (Repository repository = Git.open(backupDir.toFile()).getRepository()) {
            commitDetails = new ArrayList<>();

            // Browse the list of commits given as a parameter
            for (RevCommit commit : commits) {
                // A list to stock details about the commit
                List<String> commitInfo = new ArrayList<>();
                commitInfo.add(commit.getName()); // ID of commit

                // Get the size of files changes by the commit
                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(commit.getTree());
                    treeWalk.setRecursive(true);
                    long totalSize = 0;

                    while (treeWalk.next()) {
                        ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
                        totalSize += loader.getSize(); // size in bytes
                    }

                    // Convert the size to Kb or Mb
                    String sizeFormatted = (totalSize > 1024 * 1024)
                            ? String.format("%.2f Mo", totalSize / (1024.0 * 1024.0))
                            : String.format("%.2f Ko", totalSize / 1024.0);

                    commitInfo.add(sizeFormatted); // Add Formatted size
                }

                // adding date detail
                Date date = commit.getAuthorIdent().getWhen();
                commitInfo.add(date.toString());
                // Add list of details to the main list
                commitDetails.add(commitInfo);
            }
        }

        return commitDetails;
    }

    /**
     * Shuts down the JGit components and optionally creates a backup.
     *
     * @param backupDir the backup directory
     * @param createBackup whether to create a backup before shutting down
     * @param originalPath the original path of the file to be backed up
     */

    private void shutdownJGit(Path backupDir, boolean createBackup, Path originalPath) {
        changeFilter.unregisterListener(this);
        changeFilter.shutdown();
        executor.shutdown();

        if (createBackup) {
            try {
                performBackup(backupDir, originalPath);
            } catch (IOException | GitAPIException e) {
                LOGGER.error("Error during shutdown backup", e);
            }
        }
    }
}






