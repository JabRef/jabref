package org.jabref.gui.autosaveandbackup;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.backup.BackupEntry;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.CoarseChangeFilter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.BibDatabaseContextChangedEvent;
import org.jabref.model.entry.BibEntryTypesManager;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupManagerGit {

    static Set<BackupManagerGit> runningInstances = new HashSet<BackupManagerGit>();

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupManagerGit.class);

    private static final int DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS = 19;

    private static Git git;

    private final BibDatabaseContext bibDatabaseContext;
    private final CliPreferences preferences;
    private final ScheduledThreadPoolExecutor executor;
    private final CoarseChangeFilter changeFilter;
    private final BibEntryTypesManager entryTypesManager;
    private final LibraryTab libraryTab;

    private boolean needsBackup = false;

    BackupManagerGit(LibraryTab libraryTab, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager, CliPreferences preferences) throws IOException, GitAPIException {
        Path dbFile = bibDatabaseContext.getDatabasePath().orElseThrow(() -> new IllegalArgumentException("Database path is not provided."));
        if (!Files.exists(dbFile)) {
            LOGGER.error("Database file does not exist: {}", dbFile);
            throw new IOException("Database file not found: " + dbFile);
        }

        this.bibDatabaseContext = bibDatabaseContext;
        LOGGER.info("Backup manager initialized for file: {}", bibDatabaseContext.getDatabasePath().orElseThrow());
        this.entryTypesManager = entryTypesManager;
        this.preferences = preferences;
        this.executor = new ScheduledThreadPoolExecutor(2);
        this.libraryTab = libraryTab;

        changeFilter = new CoarseChangeFilter(bibDatabaseContext);
        changeFilter.registerListener(this);

        Path backupDirPath = preferences.getFilePreferences().getBackupDirectory();
        LOGGER.info("Backup directory path: {}", backupDirPath);

        ensureGitInitialized(backupDirPath);

        File backupDir = backupDirPath.toFile();
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            LOGGER.error("Failed to create backup directory: {}", backupDir);
            throw new IOException("Unable to create backup directory: " + backupDir);
        }

        copyDatabaseFileToBackupDir(dbFile, backupDirPath);
    }

    /**
     * Appends a UUID to a file name, keeping the original extension.
     *
     * @param originalFileName The original file name (e.g., library.bib).
     * @param uuid             The UUID to append.
     * @return The modified file name with the UUID (e.g., library_123e4567-e89b-12d3-a456-426614174000.bib).
     */
    private String appendUuidToFileName(String originalFileName, String uuid) {
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex == -1) {
            // If there's no extension, just append the UUID
            return originalFileName + "_" + uuid;
        }

        // Insert the UUID before the extension
        String baseName = originalFileName.substring(0, dotIndex);
        String extension = originalFileName.substring(dotIndex);
        return baseName + "_" + uuid + extension;
    }

    /**
     * Retrieves or generates a persistent unique identifier (UUID) for the given file.
     * The UUID is stored in an extended attribute or a metadata file alongside the original file.
     *
     * @param filePath The path to the file.
     * @return The UUID associated with the file.
     * @throws IOException If an error occurs while accessing or creating the UUID.
     */
    private String getOrGenerateFileUuid(Path filePath) throws IOException {
        // Define a hidden metadata file to store the UUID
        Path metadataFile = filePath.resolveSibling("." + filePath.getFileName().toString() + ".uuid");

        // If the UUID metadata file exists, read it
        if (Files.exists(metadataFile)) {
            return Files.readString(metadataFile).trim();
        }

        // Otherwise, generate a new UUID and save it
        String uuid = UUID.randomUUID().toString();
        Files.writeString(metadataFile, uuid);
        LOGGER.info("Generated new UUID for file {}: {}", filePath, uuid);
        return uuid;
    }

    // Helper method to normalize BibTeX content
    private static String normalizeBibTeX(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        // Diviser les lignes et traiter chaque ligne
        Stream<String> lines = input.lines();

        // Normalisation des lignes
        String normalized = lines
                .map(String::trim) // Supprimer les espaces en début et fin de ligne
                .filter(line -> !line.isBlank()) // Supprimer les lignes vides
                .collect(Collectors.joining("\n")); // Réassembler avec des sauts de ligne

        return normalized;
    }

    // Helper method to ensure the Git repository is initialized
    static void ensureGitInitialized(Path backupDir) throws IOException, GitAPIException {

        // This method was created because the initialization of the Git object, when written in the constructor, was causing a NullPointerException
        // because the first method called when loading the database is BackupGitdiffers

        // Convert Path to File
        File gitDir = new File(backupDir.toFile(), ".git");

        // Check if the `.git` directory exists
        if (!gitDir.exists() || !gitDir.isDirectory()) {
            LOGGER.info(".git directory not found in {}, initializing new Git repository.", backupDir);

            // Initialize a new Git repository
            Git.init().setDirectory(backupDir.toFile()).call();
            LOGGER.info("Git repository successfully initialized in {}", backupDir);
        } else {
            LOGGER.info("Existing Git repository found in {}", backupDir);
        }

        // Build the Git object
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(gitDir)
                                       .readEnvironment()
                                       .findGitDir()
                                       .build();
        git = new Git(repository);
    }

    // Helper method to copy the database file to the backup directory
    private void copyDatabaseFileToBackupDir(Path dbFile, Path backupDirPath) throws IOException {
        String fileUuid = getOrGenerateFileUuid(dbFile);
        String uniqueFileName = appendUuidToFileName(dbFile.getFileName().toString(), fileUuid);
        Path backupFilePath = backupDirPath.resolve(uniqueFileName);
        if (!Files.exists(backupFilePath) || Files.mismatch(dbFile, backupFilePath) != -1) {
            Files.copy(dbFile, backupFilePath, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Database file uniquely copied to backup directory: {}", backupFilePath);
        } else {
            LOGGER.info("No changes detected; skipping backup for file: {}", uniqueFileName);
        }
    }

    /**
     * Starts a new BackupManagerGit instance and begins the backup task.
     *
     * @param libraryTab         the library tab
     * @param bibDatabaseContext the BibDatabaseContext to be backed up
     * @param entryTypesManager  the BibEntryTypesManager
     * @param preferences        the CLI preferences
     * @return the started BackupManagerGit instance
     * @throws IOException     if an I/O error occurs
     * @throws GitAPIException if a Git API error occurs
     */

    public static BackupManagerGit start(LibraryTab libraryTab, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager, CliPreferences preferences) throws IOException, GitAPIException {
        LOGGER.info("In methode Start");
        BackupManagerGit backupManagerGit = new BackupManagerGit(libraryTab, bibDatabaseContext, entryTypesManager, preferences);
        backupManagerGit.startBackupTask(preferences.getFilePreferences().getBackupDirectory(), bibDatabaseContext);
        runningInstances.add(backupManagerGit);
        return backupManagerGit;
    }

    /**
     * Shuts down the BackupManagerGit instances associated with the given BibDatabaseContext.
     *
     * @param bibDatabaseContext the BibDatabaseContext
     * @param createBackup whether to create a backup before shutting down
     */
    public static void shutdown(BibDatabaseContext bibDatabaseContext, boolean createBackup) {
        runningInstances.stream()
                        .filter(instance -> instance.bibDatabaseContext == bibDatabaseContext)
                        .forEach(backupManager -> backupManager.shutdownGit(bibDatabaseContext.getDatabasePath().orElseThrow().getParent().resolve("backup"), createBackup));

        // Remove the instances associated with the BibDatabaseContext after shutdown
        runningInstances.removeIf(instance -> instance.bibDatabaseContext == bibDatabaseContext);
        LOGGER.info("Shut down backup manager for file: {}");
    }

    /**
     * Starts the backup task that periodically checks for changes and commits them to the Git repository.
     *
     * @param backupDir the backup directory
     */

    void startBackupTask(Path backupDir, BibDatabaseContext bibDatabaseContext) {
        LOGGER.info("Initializing backup task for directory: {} and file: {}", backupDir, bibDatabaseContext.getDatabasePath().orElseThrow());
        executor.scheduleAtFixedRate(
                () -> {
                    try {
                        Path dbFile = bibDatabaseContext.getDatabasePath().orElseThrow(() -> new IllegalArgumentException("Database path is not provided."));
                        copyDatabaseFileToBackupDir(dbFile, backupDir);
                        performBackup(backupDir);
                    } catch (IOException | GitAPIException e) {
                        LOGGER.error("Error during backup", e);
                    }
                },
                DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS,
                DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS,
                TimeUnit.SECONDS);
        LOGGER.info("Backup task scheduled with a delay of {} seconds", DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS);
    }

    /**
     * Performs the backup by checking for changes and committing them to the Git repository.
     *
     * @param backupDir the backup directory
     * @throws IOException     if an I/O error occurs
     * @throws GitAPIException if a Git API error occurs
     */

    protected void performBackup(Path backupDir) throws IOException, GitAPIException {

        boolean needsCommit = backupGitDiffers(backupDir);

        if (!needsBackup && !needsCommit) {
            LOGGER.info("No changes detected, beacuse needsBackup is :" + needsBackup + " and needsCommit is :" + needsCommit);
            return;
        }

        if (needsBackup) {
            LOGGER.info("Backup needed, because needsBackup is :" + needsBackup);
        } else {
        LOGGER.info("Backup needed, because needsCommit is :" + needsCommit);
    }

        // Stage the file for commit
        git.add().addFilepattern(".").call();
        LOGGER.info("Staged changes for backup in directory: {}", backupDir);

        // Commit the staged changes
        RevCommit commit = git.commit()
                              .setMessage("Backup at " + Instant.now().toString())
                              .call();
        LOGGER.info("Backup committed in :" + backupDir + " with commit ID: " + commit.getName()
        + " for the file : {}", bibDatabaseContext.getDatabasePath().orElseThrow());
    }

    public synchronized void listen(BibDatabaseContextChangedEvent event) {
        if (!event.isFilteredOut()) {
            LOGGER.info("Change detected/LISTENED in file: {}", bibDatabaseContext.getDatabasePath().orElseThrow());
            this.needsBackup = true;
        }
    }

    /**
     * Restores the backup from the specified commit.
     *
     * @param backupDir the backup directory
     * @param objectId the commit ID to restore from
     */

    public static void restoreBackup(Path backupDir, ObjectId objectId) {
        try {
            Git git = Git.open(backupDir.toFile());

            git.checkout().setStartPoint(objectId.getName()).setAllPaths(true).call();
            LOGGER.info("checkout done");

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
     * Checks if there are differences between the files in the directory and the last commit.
     *
     * @param backupDir the backup directory
     * @return true if there are differences, false otherwise
     * @throws IOException if an I/O error occurs
     * @throws GitAPIException if a Git API error occurs
     */

    public static boolean backupGitDiffers(Path backupDir) throws IOException, GitAPIException {
        // Ensure the Git repository exists
        LOGGER.info("Checking if backup differs for directory: {}", backupDir);

        // Open the Git repository located in the backup directory
        Repository repository = openGitRepository(backupDir);

        // Get the HEAD commit to compare with
        ObjectId headCommitId = repository.resolve("HEAD");
        if (headCommitId == null) {
            LOGGER.info("No commits found in the repository. Assuming the file differs.");
            return true;
        }
        LOGGER.info("HEAD commit ID: {}", headCommitId.getName());

        // Iterate over the files in the backup directory to check if they differ from the repository
        try (Stream<Path> paths = Files.walk(backupDir)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                // Ignore non-.bib files (e.g., .DS_Store)
                if (!path.toString().endsWith(".bib")) {
                    continue;  // Skip .bib files
                }

                // Skip .git directory files
                if (path.toString().contains(".git")) {
                    continue;
                }

                // Calculate the relative path in the Git repository
                Path relativePath = backupDir.relativize(path);
                LOGGER.info("Checking file: {}", relativePath);

                try {

                    // Check if the file exists in the latest commit
                    ObjectId objectId = repository.resolve("HEAD:" + relativePath.toString().replace("\\", "/"));
                    if (objectId == null) {
                        LOGGER.info("File not found in the latest commit: {}. Assuming it differs.", relativePath);
                        return true;
                    }

                    // Compare the content of the file in the Git repository with the current file
                    ObjectLoader loader = repository.open(objectId);
                    String committedContent = normalizeBibTeX(new String(loader.getBytes(), StandardCharsets.UTF_8));
                    String currentContent = normalizeBibTeX(Files.readString(path, StandardCharsets.UTF_8));
                    LOGGER.info("Committed content: {}", committedContent);
                    LOGGER.info("Current content: {}", currentContent);

                    // If the contents differ, return true
                    if (!currentContent.equals(committedContent)) {
                        LOGGER.info("Content differs for file: {}", relativePath);
                        return true;
                    }
                } catch (MissingObjectException e) {
                    // If the file is missing from the commit, assume it differs
                    LOGGER.info("File not found in the latest commit: {}. Assuming it differs.", relativePath);
                    return true;
                }
            }
        }

        LOGGER.info("No differences found in the backup.");
        return false;  // No differences found
    }

    private static Repository openGitRepository(Path backupDir) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        // Initialize Git repository from the backup directory
        return builder.setGitDir(new File(backupDir.toFile(), ".git"))
                      .readEnvironment()
                      .findGitDir()
                      .build();
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

    /**
     * Retrieves the last n commits from the Git repository.
     *
     * @param backupDir the backup directory
     * @param n the number of commits to retrieve
     * @return a list of RevCommit objects representing the commits
     * @throws IOException if an I/O error occurs
     * @throws GitAPIException if a Git API error occurs
     */

    public static List<RevCommit> retrieveCommits(Path backupDir, int n) throws IOException, GitAPIException {
        List<RevCommit> retrievedCommits = new ArrayList<>();
        // Open Git repository
        try (Repository repository = Git.open(backupDir.toFile()).getRepository()) {
            // Use RevWalk to traverse commits
            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit startCommit = revWalk.parseCommit(repository.resolve("HEAD"));
                revWalk.markStart(startCommit);

                int count = 0;
                for (RevCommit commit : revWalk) {
                    retrievedCommits.add(commit);
                    count++;
                    if (count == n) {
                        break; // Stop after collecting the required number of commits
                    }
                }
            }
        }

        // Reverse the list to have commits in the correct order
        Collections.reverse(retrievedCommits);
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

    public static List<BackupEntry> retrieveCommitDetails(List<RevCommit> commits, Path backupDir) throws IOException, GitAPIException {
        List<BackupEntry> commitDetails;
        try (Repository repository = Git.open(backupDir.toFile()).getRepository()) {
            commitDetails = new ArrayList<>();

            // Browse the list of commits given as a parameter
            for (RevCommit commit : commits) {
                // A list to stock details about the commit
                List<String> commitInfo = new ArrayList<>();
                commitInfo.add(commit.getName()); // ID of commit

                // Get the size of files changes by the commit
                String sizeFormatted;
                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(commit.getTree());
                    treeWalk.setRecursive(true);
                    long totalSize = 0;

                    while (treeWalk.next()) {
                        ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
                        totalSize += loader.getSize(); // size in bytes
                    }

                    // Convert the size to Kb or Mb
                    sizeFormatted = (totalSize > 1024 * 1024)
                            ? String.format("%.2f Mo", totalSize / (1024.0 * 1024.0))
                            : String.format("%.2f Ko", totalSize / 1024.0);

                    commitInfo.add(sizeFormatted); // Add Formatted size
                }

                // adding date detail
                Date date = commit.getAuthorIdent().getWhen();
                commitInfo.add(date.toString());
                // Add list of details to the main list
                BackupEntry backupEntry = new BackupEntry(ObjectId.fromString(commit.getName()), commitInfo.get(0), commitInfo.get(2), commitInfo.get(1), 0);
                commitDetails.add(backupEntry);
            }
        }

        return commitDetails;
    }

    /**
     * Shuts down the JGit components and optionally creates a backup.
     *
     * @param createBackup whether to create a backup before shutting down
     */

    private void shutdownGit(Path backupDir, boolean createBackup) {
        // Unregister the listener and shut down the change filter
        if (changeFilter != null) {
            changeFilter.unregisterListener(this);
            changeFilter.shutdown();
            LOGGER.info("Shut down change filter");
        }

        // Shut down the executor if it's not already shut down
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            LOGGER.info("Shut down backup task for file: {}");
        }

        // If backup is requested, ensure that we perform the Git-based backup
        if (createBackup) {
            try {
                // Ensure the backup is a recent one by performing the Git commit
                performBackup(backupDir);
                LOGGER.info("Backup created on shutdown for file: {}");
            } catch (IOException | GitAPIException e) {
                LOGGER.error("Error during Git backup on shutdown");
            }
        }
    }
}






