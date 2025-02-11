package org.jabref.gui.autosaveandbackup;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
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

public class BackupManager {

    static Set<BackupManager> runningInstances = new HashSet<>();

    private static final String LINE_BREAK = System.lineSeparator();
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupManager.class);

    private static final int DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS = 19;

    private static Git git;

    private final BibDatabaseContext bibDatabaseContext;
    private final Path backupDirectory;
    private final ScheduledThreadPoolExecutor executor;
    private final CoarseChangeFilter changeFilter;
    private final BibEntryTypesManager entryTypesManager;
    private final LibraryTab libraryTab;

    private boolean needsBackup = false;

    BackupManager(LibraryTab libraryTab, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager, Path backupDir) throws IOException, GitAPIException {
        Path dbFile = bibDatabaseContext.getDatabasePath().orElseThrow(() -> new IllegalArgumentException("Database path is not provided."));
        if (!Files.exists(dbFile)) {
            LOGGER.error("Database file does not exist: {}", dbFile);
            throw new IOException("Database file not found: " + dbFile);
        }

        this.bibDatabaseContext = bibDatabaseContext;
        LOGGER.info("Backup manager initialized for file: {}", bibDatabaseContext.getDatabasePath().orElseThrow());
        this.entryTypesManager = entryTypesManager;
        this.backupDirectory = backupDir;
        this.executor = new ScheduledThreadPoolExecutor(2);
        this.libraryTab = libraryTab;

        changeFilter = new CoarseChangeFilter(bibDatabaseContext);
        changeFilter.registerListener(this);

        LOGGER.info("Backup directory path: {}", backupDirectory);

        ensureGitInitialized(backupDirectory);

        File backupDirFile = backupDirectory.toFile();
        if (!backupDirFile.exists() && !backupDirFile.mkdirs()) {
            LOGGER.error("Failed to create backup directory: {}", backupDirectory);
            throw new IOException("Unable to create backup directory: " + backupDirectory);
        }

        copyDatabaseFileToBackupDir(dbFile, backupDirectory);
    }

    /**
     * Appends a UUID to a file name, keeping the original extension.
     *
     * @param originalFileName The original file name (e.g., library.bib).
     * @param uuid             The UUID to append.
     * @return The modified file name with the UUID (e.g., library_123e4567-e89b-12d3-a456-426614174000.bib).
     */
    private static String appendUuidToFileName(String originalFileName, String uuid) {
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
    protected static String getOrGenerateFileUuid(Path filePath) throws IOException {
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

    /**
     * Rewrites the content of the file at the specified path with the given string.
     *
     * @param dbFile The path to the file to be rewritten.
     * @param content The string content to write into the file.
     * @throws IOException If an I/O error occurs during the write operation.
     */
    public static void rewriteFile(Path dbFile, String content) throws IOException {
        // Ensure the file exists before rewriting
        if (!Files.exists(dbFile)) {
            Locale currentLocale = Locale.getDefault();
            ResourceBundle messages = ResourceBundle.getBundle("messages", currentLocale);
            String errorMessage = MessageFormat.format(messages.getString("file.not.found"), dbFile.toString());
            throw new FileNotFoundException(errorMessage);
        }

        // Write the new content to the file (overwrite mode)
        Files.writeString(dbFile, content, StandardCharsets.UTF_8);

        LOGGER.info("Successfully rewrote the file at path: {}", dbFile);
    }

    /**
     * Normalizes the BibTeX content by trimming spaces, removing blank lines, and reassembling with line breaks.
     * This is needed to ensure consistent formatting of BibTeX entries, which helps in comparing and processing them.
     *
     * @param input The raw BibTeX content.
     * @return The normalized BibTeX content.
     */
    private static String normalizeBibTeX(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        // Split lines and process each line
        Stream<String> lines = input.lines();

        // Normalize lines
        String normalized = lines
                .map(String::trim) // Remove leading and trailing spaces
                .filter(line -> !line.isBlank()) // Remove blank lines
                .collect(Collectors.joining(LINE_BREAK)); // Reassemble with line breaks

        return normalized;
    }

    /**
     * Ensures that a Git repository is initialized in the specified backup directory.
     * If the `.git` directory does not exist, it initializes a new Git repository.
     * Then, it builds the Git object for further operations.
     *
     * @param backupDir The backup directory containing the Git repository.
     * @throws IOException If an I/O error occurs.
     * @throws GitAPIException If a Git API error occurs.
     */
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

    /**
     * Copies the database file to the backup directory with a unique file name.
     * The unique file name is generated by appending a UUID to the original file name.
     *
     * @param dbFile        The path to the database file to be copied.
     * @param backupDirPath The path to the backup directory.
     * @throws IOException If an I/O error occurs during the file copy operation.
     */
        protected static void copyDatabaseFileToBackupDir(Path dbFile, Path backupDirPath) throws IOException {
        String fileUuid = getOrGenerateFileUuid(dbFile);
        String uniqueFileName = appendUuidToFileName(dbFile.getFileName().toString(), fileUuid);
        Path backupFilePath = backupDirPath.resolve(uniqueFileName);
        Files.copy(dbFile, backupFilePath, StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("Database file uniquely copied to backup directory: {}", backupFilePath);
    }

    /**
     * Starts a new BackupManager instance and begins the backup task.
     *
     * @param libraryTab         the library tab
     * @param bibDatabaseContext the BibDatabaseContext to be backed up
     * @param entryTypesManager  the BibEntryTypesManager
     * @param preferences        the CLI preferences
     * @return the started BackupManager instance
     * @throws IOException     if an I/O error occurs
     * @throws GitAPIException if a Git API error occurs
     */
    public static BackupManager start(LibraryTab libraryTab, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager, CliPreferences preferences) throws IOException, GitAPIException {
        LOGGER.info("In methode Start");
        Path backupDir = preferences.getFilePreferences().getBackupDirectory();
        BackupManager backupManager = new BackupManager(libraryTab, bibDatabaseContext, entryTypesManager, backupDir);
        backupManager.startBackupTask(preferences.getFilePreferences().getBackupDirectory(), bibDatabaseContext);
        runningInstances.add(backupManager);
        return backupManager;
    }

    /**
     * Shuts down the BackupManager instances associated with the given BibDatabaseContext.
     *
     * @param bibDatabaseContext the BibDatabaseContext
     * @param createBackup whether to create a backup before shutting down
     */
    public static void shutdown(BibDatabaseContext bibDatabaseContext, Path backupDir, boolean createBackup) {
        runningInstances.stream()
                        .filter(instance -> instance.bibDatabaseContext == bibDatabaseContext)
                        .forEach(backupManager -> backupManager.shutdownGit(bibDatabaseContext,
                                backupDir,
                                createBackup));

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
                        // copyDatabaseFileToBackupDir(dbFile, backupDir);
                        performBackup(dbFile, backupDir);
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
     * @param dbfile the database file
     * @throws IOException     if an I/O error occurs
     * @throws GitAPIException if a Git API error occurs
     */
    protected void performBackup(Path dbfile, Path backupDir) throws IOException, GitAPIException {

        boolean needsCommit = backupGitDiffers(dbfile, backupDir);

        if (!needsBackup && !needsCommit) {
            LOGGER.info("No changes detected, beacuse needsBackup is: {} and needsCommit is: {}", needsBackup, needsCommit);
            return;
        }

        if (needsBackup) {
            LOGGER.info("Backup needed, because needsBackup is: {}", needsBackup);
        } else {
            LOGGER.info("Backup needed, because needsCommit is: {}", needsCommit);
        }

        // Stage the file for commit
        git.add().addFilepattern(".").call();
        LOGGER.info("Staged changes for backup in directory: {}", backupDir);

        // Commit the staged changes
        RevCommit commit = git.commit()
                              .setMessage("Backup at " + Instant.now().toString())
                              .call();
        LOGGER.info("Backup committed in: {} with commit ID: {} for the file: {}", backupDir, commit.getName(), bibDatabaseContext.getDatabasePath().orElseThrow());
    }

    /**
     * Listens for changes in the BibDatabaseContext and sets the needsBackup flag if a change is detected.
     *
     * @param event The BibDatabaseContextChangedEvent to listen for.
     */
    public synchronized void listen(BibDatabaseContextChangedEvent event) {
        if (!event.isFilteredOut()) {
            LOGGER.info("Change detected/LISTENED in file: {}", bibDatabaseContext.getDatabasePath().orElseThrow());
            this.needsBackup = true;
        }
    }

    /**
     * Restores the backup of the specified database file from the given commit ID.
     * This method retrieves the content of the file from the specified commit in the Git repository
     * and rewrites the original file with the retrieved content.
     *
     * @param dbFile    The path to the database file to be restored.
     * @param backupDir The backup directory containing the Git repository.
     * @param objectId  The commit ID from which to restore the file.
     */
    public static void restoreBackup(Path dbFile, Path backupDir, ObjectId objectId) {
        try (Repository repository = openGitRepository(backupDir)) {
            // Resolve the filename of dbFile in the repository
            String baseName = dbFile.getFileName().toString();
            String uuid = getOrGenerateFileUuid(dbFile); // Generate or retrieve the UUID for this file
            String relativeFilePath = baseName.replace(".bib", "") + "_" + uuid + ".bib";
            LOGGER.info("Relative file path TO RESTORE: {}", relativeFilePath);
            String gitPath = backupDir.relativize(backupDir.resolve(relativeFilePath)).toString().replace("\\", "/");

            LOGGER.info("Restoring file: {}", gitPath);

            // Load the content of the file from the specified commit
            ObjectId fileObjectId = repository.resolve(objectId.getName() + ": " + gitPath);
            if (fileObjectId == null) { // File not found in the commit
                performBackupNoCommits(dbFile, backupDir);
            }

            // Read the content of the file from the Git object
            ObjectLoader loader = repository.open(fileObjectId);
            String fileContent = new String(loader.getBytes(), StandardCharsets.UTF_8);

            // Rewrite the original file at dbFile path
            rewriteFile(dbFile, fileContent);
            LOGGER.info("Restored content to: {}", dbFile);
        } catch (IOException | IllegalArgumentException | GitAPIException e) {
            LOGGER.error("Error while restoring the backup: {}", e.getMessage(), e);
        }
    }

    /**
     * Checks if there are differences between the current database file and the last committed version in the Git repository.
     * This method ensures the database file is copied to the backup directory, initializes the Git repository if needed,
     * and compares the content of the file in the repository with the current file.
     *
     * @param dbFile    The path to the database file to be checked.
     * @param backupDir The backup directory containing the Git repository.
     * @return true if there are differences, false otherwise.
     * @throws IOException     If an I/O error occurs.
     * @throws GitAPIException If a Git API error occurs.
     */
    public static boolean backupGitDiffers(Path dbFile, Path backupDir) throws IOException, GitAPIException {

        // Ensure the specific database file is copied to the backup directory
        copyDatabaseFileToBackupDir(dbFile, backupDir);

        // Ensure the Git repository exists
        LOGGER.info("Checking if backup differs for file: {}", dbFile);

        // Open the Git repository located in the backup directory
        Repository repository = openGitRepository(backupDir);

        // Get the HEAD commit to compare with
        ObjectId headCommitId = repository.resolve("HEAD");
        if (headCommitId == null) {
            LOGGER.info("No commits found in the repository. Assuming the file differs.");
            // perform a commit
            performBackupNoCommits(dbFile, backupDir);
            return false;
        }
        LOGGER.info("HEAD commit ID: {}", headCommitId.getName());

        // Compute the repository file name using the naming convention (filename + UUID)
        String baseName = dbFile.getFileName().toString();
        String uuid = getOrGenerateFileUuid(dbFile); // Generate or retrieve the UUID for this file
        String repoFileName = baseName.replace(".bib", "") + "_" + uuid + ".bib";
        Path relativePath = Path.of(repoFileName);
        LOGGER.info("Checking repository file: {}", relativePath);

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
            String currentContent = normalizeBibTeX(Files.readString(dbFile, StandardCharsets.UTF_8));
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

        LOGGER.info("No differences found for the file: {}", dbFile);
        return false;  // No differences found
    }

    /**
     * Retrieves the backup file path for the given database file.
     * This method generates or retrieves a UUID for the file and constructs the backup file path using the UUID.
     *
     * @param dbFile    The path to the database file.
     * @param backupDir The backup directory.
     * @return The path to the backup file.
     */
    public static Path getBackupFilePath(Path dbFile, Path backupDir) {
        try {
            String baseName = dbFile.getFileName().toString();
            String uuid = getOrGenerateFileUuid(dbFile);
            String relativeFileName = baseName.replace(".bib", "") + "_" + uuid + ".bib";
            return backupDir.resolve(relativeFileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes the backup file content from the specified commit ID to the backup directory.
     * This method retrieves the content of the file from the specified commit in the Git repository
     * and rewrites the backup file with the retrieved content.
     *
     * @param dbFile    The path to the database file.
     * @param backupDir The backup directory containing the Git repository.
     * @param objectId  The commit ID from which to retrieve the file content.
     */
    public static void writeBackupFileToCommit(Path dbFile, Path backupDir, ObjectId objectId) {
        try (Repository repository = openGitRepository(backupDir)) {
            // Resolve the filename of dbFile in the repository
            String baseName = dbFile.getFileName().toString();
            String uuid = getOrGenerateFileUuid(dbFile); // Generate or retrieve the UUID for this file
            String relativeFilePath = baseName.replace(".bib", "") + "_" + uuid + ".bib";
            LOGGER.info("Relative file path TO RESTORE: {}", relativeFilePath);
            String gitPath = backupDir.relativize(backupDir.resolve(relativeFilePath)).toString().replace("\\", "/");

            LOGGER.info("Restoring file: {}", gitPath);

            // Load the content of the file from the specified commit
            ObjectId fileObjectId = repository.resolve(objectId.getName() + ": " + gitPath);
            if (fileObjectId == null) { // File not found in the commit
                performBackupNoCommits(dbFile, backupDir);
            }

            // Read the content of the file from the Git object
            ObjectLoader loader = repository.open(fileObjectId);
            String fileContent = new String(loader.getBytes(), StandardCharsets.UTF_8);

            Path backupFilePath = getBackupFilePath(dbFile, backupDir);
            // Rewrite the original file at backupFilePath path
            rewriteFile(backupFilePath, fileContent);
            LOGGER.info("Restored content to: {}", dbFile);
        } catch (IOException | IllegalArgumentException | GitAPIException e) {
            LOGGER.error("Error while restoring the backup: {}", e.getMessage(), e);
        }
    }

    /**
     * Opens the Git repository located in the specified backup directory.
     *
     * @param backupDir The backup directory containing the Git repository.
     * @return The opened Git repository.
     * @throws IOException If an I/O error occurs.
     */
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
     * @param dbFile the path of the file
     * @param backupDir the backup directory
     * @param commitId the commit ID to compare with the latest commit
     * @return a list of DiffEntry objects representing the differences
     * @throws IOException if an I/O error occurs
     * @throws GitAPIException if a Git API error occurs
     */
    public List<DiffEntry> showDiffers(Path dbFile, Path backupDir, String commitId) throws IOException, GitAPIException {

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
     * @param dbFile the database file
     * @param backupDir the backup directory
     * @param n the number of commits to retrieve
     * @return a list of RevCommit objects representing the commits
     * @throws IOException if an I/O error occurs
     * @throws GitAPIException if a Git API error occurs
     */
    public static List<RevCommit> retrieveCommits(Path dbFile, Path backupDir, int n) throws IOException, GitAPIException {
        List<RevCommit> retrievedCommits = new ArrayList<>();

        // Compute the repository file name using the naming convention (filename + UUID)
        String baseName = dbFile.getFileName().toString();
        String uuid = getOrGenerateFileUuid(dbFile); // Generate or retrieve the UUID for this file
        String repoFileName = baseName.replace(".bib", "") + "_" + uuid + ".bib";
        String dbFileRelativePath = backupDir.relativize(backupDir.resolve(repoFileName)).toString().replace("\\", "/");

        // Open Git repository
        try (Repository repository = Git.open(backupDir.toFile()).getRepository()) {
            // Use RevWalk to traverse commits
            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit startCommit = revWalk.parseCommit(repository.resolve("HEAD"));
                revWalk.markStart(startCommit);

                int count = 0;
                for (RevCommit commit : revWalk) {
                    // Check if this commit involves the dbFile
                    try (TreeWalk treeWalk = new TreeWalk(repository)) {
                        treeWalk.addTree(commit.getTree());
                        treeWalk.setRecursive(true);

                        boolean fileFound = false;
                        while (treeWalk.next()) {
                            if (treeWalk.getPathString().equals(dbFileRelativePath)) {
                                fileFound = true;
                                break;
                            }
                        }

                        if (fileFound) {
                            retrievedCommits.add(commit);
                            count++;
                            if (count == n) {
                                break; // Stop after collecting the required number of commits
                            }
                        }
                    }
                }
            }
        }

        return retrievedCommits;
    }

    /**
     * Retrieves detailed information about the specified commits, focusing on the target file.
     *
     * @param commits the list of commits to retrieve details for
     * @param dbFile the target file to retrieve details about
     * @param backupDir the backup directory
     * @return a list of BackupEntry objects containing details about each commit
     * @throws IOException if an I/O error occurs
     * @throws GitAPIException if a Git API error occurs
     */
    public static List<BackupEntry> retrieveCommitDetails(List<RevCommit> commits, Path dbFile, Path backupDir) throws IOException, GitAPIException {
        List<BackupEntry> commitDetails = new ArrayList<>();

        // Compute the repository file name using the naming convention (filename + UUID)
        String baseName = dbFile.getFileName().toString();
        String uuid = getOrGenerateFileUuid(dbFile); // Generate or retrieve the UUID for this file
        String repoFileName = baseName.replace(".bib", "") + "_" + uuid + ".bib";
        String dbFileRelativePath = backupDir.relativize(backupDir.resolve(repoFileName)).toString().replace("\\", "/");

        try (Repository repository = Git.open(backupDir.toFile()).getRepository()) {
            // Browse the list of commits given as a parameter
            for (RevCommit commit : commits) {
                // Variables to store commit-specific details
                String sizeFormatted = "0 KB";
                long fileSize = 0;
                boolean fileFound = false;

                // Use TreeWalk to find the target file in the commit
                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(commit.getTree());
                    treeWalk.setRecursive(true);

                    while (treeWalk.next()) {
                        if (treeWalk.getPathString().equals(dbFileRelativePath)) {
                            // Calculate size of the target file
                            ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
                            fileSize = loader.getSize();
                            fileFound = true;
                            break;
                        }
                    }

                    // Convert size to KB or MB
                    sizeFormatted = fileSize > 1024 * 1024
                            ? "%.2f MB".formatted(fileSize / (1024.0 * 1024.0))
                            : "%.2f KB".formatted(fileSize / 1024.0);
                }

                // Skip this commit if the file was not found
                if (!fileFound) {
                    continue;
                }

                // Add commit details
                Date date = commit.getAuthorIdent().getWhen();
                BackupEntry backupEntry = new BackupEntry(
                        ObjectId.fromString(commit.getName()), // Commit ID
                        commit.getName(),                      // Commit ID as string
                        date.toString(),                       // Commit date
                        sizeFormatted,                         // Formatted file size
                        1                                      // Number of relevant .bib files (always 1 for dbFile)
                );
                commitDetails.add(backupEntry);
            }
        }

        return commitDetails;
    }

    /**
     * Performs the initial backup by creating the first commit in the Git repository.
     * This method ensures the Git repository is initialized, stages the database file, and commits it.
     *
     * @param dbFile    The path to the database file to be backed up.
     * @param backupDir The backup directory containing the Git repository.
     * @throws IOException     If an I/O error occurs.
     * @throws GitAPIException If a Git API error occurs.
     */
    public static void performBackupNoCommits(Path dbFile, Path backupDir) throws IOException, GitAPIException {

        LOGGER.info("No commits found in the repository. We need a first commit.");
        // Ensure the specific database file is copied to the backup directory
        // no need of copying again !!
        // copyDatabaseFileToBackupDir(dbFile, backupDir);

        // Ensure the Git repository exists
        LOGGER.info("Ensuring the .git is initialized");
        ensureGitInitialized(backupDir);

        // Get the file name of the database file
        String baseName = dbFile.getFileName().toString();
        String uuid = getOrGenerateFileUuid(dbFile); // Generate or retrieve the UUID for this file
        String repoFileName = baseName.replace(".bib", "") + "_" + uuid + ".bib";

        // Stage the file for commit
        LOGGER.info("Staging the file for commit");
        git.add().addFilepattern(repoFileName).call();

        // Commit the staged changes
        LOGGER.info("Committing the file");
        RevCommit commit = git.commit()
                              .setMessage("Backup at " + Instant.now().toString())
                              .call();
    }

    /**
     * Shuts down the JGit components and optionally creates a backup.
     *
     * @param createBackup whether to create a backup before shutting down
     * @param backupDir the backup directory
     * @param bibDatabaseContext the BibDatabaseContext
     */
    private void shutdownGit(BibDatabaseContext bibDatabaseContext, Path backupDir, boolean createBackup) {
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
                // Get the file path of the database
                Path dbFile = bibDatabaseContext.getDatabasePath().orElseThrow(() -> new IllegalArgumentException("Database path is not provided."));
                // Ensure the backup is a recent one by performing the Git commit
                performBackup(dbFile, backupDir);
                LOGGER.info("Backup created on shutdown for file: {}");
            } catch (IOException | GitAPIException e) {
                LOGGER.error("Error during Git backup on shutdown");
            }
        }
    }
}






