package org.jabref.logic.exporter;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jabref.logic.os.OS;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.io.FileSnapshot;
import org.jabref.logic.util.io.FileUtil;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A file output stream that is similar to the standard [java.io.FileOutputStream], except that all writes are first
/// redirected to a temporary file. When the stream is closed, the temporary file (atomically) replaces the target file.
///
///
/// In detail, the strategy is to:
///
/// 1. Write to a temporary file (with .tmp suffix) in the same directory as the destination file.
/// 2. Create a backup (with .sav suffix) of the original file (if it exists) in the same directory.
/// 3. Atomically move the temporary file to the correct place, overwriting any file that already exists at that
/// location. On Linux and macOS, files with hard links are overwritten in place to preserve their inode. An in-place
/// overwrite is also used when the file system does not support atomic moves. Afterwards, the group, DOS attributes,
/// ACL and user-defined extended attributes of the original file are applied to the new file on a best-effort basis
/// (the owner cannot be restored without elevated privileges).
/// 4. Delete the backup file (if configured to do so).
///
/// If all goes well, no temporary or backup files will remain on disk after closing the stream.
///
/// Errors are handled as follows:
///
/// 1. If anything goes wrong while writing to the temporary file, the temporary file will be deleted (leaving the
/// original file untouched).
/// 2. If anything goes wrong while copying the temporary file to the target file, the backup of the original file is
/// kept.
/// 3. If the target file was modified by another process between opening this stream and committing it (e.g. a second
/// JabRef instance saving the same library), the commit is aborted with a [FileChangedException], leaving the other
/// process's version of the file untouched.
///
/// Implementation inspired by code from [Marty Lamb](https://github.com/martylamb/atomicfileoutputstream/blob/master/src/main/java/com/martiansoftware/io/AtomicFileOutputStream.java) and [Apache](https://github.com/apache/zookeeper/blob/master/src/java/main/org/apache/zookeeper/common/AtomicFileOutputStream.java).
@NullMarked
public class AtomicFileOutputStream extends FilterOutputStream {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtomicFileOutputStream.class);

    private static final String TEMPORARY_EXTENSION = ".tmp";
    private static final String TEMPORARY_FILE_PREFIX = "jabref-";
    private static final String SAVE_EXTENSION = "." + BackupFileType.SAVE.getExtensions().getFirst();

    /// Number of attempts to move the temporary file onto the target file. See [#moveTemporaryFileToTargetFile()].
    private static final int MOVE_ATTEMPTS = 5;

    /// Delay before the second move attempt; doubled after each further failed attempt.
    private static final long MOVE_RETRY_INITIAL_DELAY_MILLIS = 20;

    /// The file we want to create/replace.
    private final Path targetFile;

    /// The uniquely named file to which writes are redirected to.
    private final Path temporaryFile;

    /// Null if the stream was constructed from an injected [OutputStream] (tests), because syncing is not possible
    /// then.
    @Nullable private final FileChannel temporaryFileChannel;

    /// A backup of the target file (if it exists), created when the stream is closed
    private final Path backupFile;

    private final boolean keepBackup;

    private final FileMoveOperation fileMoveOperation;

    private final FileCopyOperation backupFileCopyOperation;

    /// The state the target file must still be in when this stream commits: an inherited baseline (see the
    /// [#AtomicFileOutputStream(Path,boolean,FileSnapshot)] constructor), or the state at stream creation. `null` if
    /// the attributes could not be read, in which case concurrent-change detection is disabled for this write.
    @Nullable private final FileSnapshot expectedTargetFileState;

    /// State of the target file right after a successful commit; `null` before the commit, after an aborted or failed
    /// one, and when the attributes could not be read.
    @Nullable private FileSnapshot committedTargetFileState;

    private boolean errorDuringWrite = false;

    @FunctionalInterface
    interface FileMoveOperation {
        void move(Path source, Path target) throws IOException;
    }

    @FunctionalInterface
    interface FileCopyOperation {
        void copy(Path source, Path target) throws IOException;
    }

    /// Creates a new output stream to write to or replace the file at the specified path.
    ///
    /// @param path       the path of the file to write to or replace
    /// @param keepBackup whether to keep the backup file (.sav) after a successful write process
    public AtomicFileOutputStream(Path path, boolean keepBackup) throws IOException {
        this(path, keepBackup, null);
    }

    /// Creates a new output stream to write to or replace the file at the specified path, verifying against an
    /// inherited baseline instead of the file's state at stream creation.
    ///
    /// @param path          the path of the file to write to or replace
    /// @param keepBackup    whether to keep the backup file (.sav) after a successful write process
    /// @param expectedState the state the target file is expected to (still) be in when this stream commits — a snapshot from an earlier point of the same logical operation, so that a concurrent write landing before this stream was even opened is still detected; `null` to verify against the state at stream creation
    public AtomicFileOutputStream(Path path, boolean keepBackup, @Nullable FileSnapshot expectedState) throws IOException {
        this(path, createTemporaryFile(path), keepBackup, expectedState, AtomicFileOutputStream::moveAtomically, AtomicFileOutputStream::copyReplacingExisting);
    }

    /// The temporary file is opened as a [FileChannel], because the channel is needed for [FileChannel#force(boolean)].
    /// `Files.newOutputStream(...)` returns a `sun.nio.ch.ChannelOutputStream`, which does not offer it.
    private AtomicFileOutputStream(Path path, Path pathOfTemporaryFile, boolean keepBackup, @Nullable FileSnapshot expectedState, FileMoveOperation fileMoveOperation, FileCopyOperation backupFileCopyOperation) throws IOException {
        this(path,
                pathOfTemporaryFile,
                FileChannel.open(pathOfTemporaryFile, StandardOpenOption.WRITE),
                keepBackup,
                expectedState,
                fileMoveOperation,
                backupFileCopyOperation);
    }

    private AtomicFileOutputStream(Path path, Path pathOfTemporaryFile, FileChannel temporaryFileChannel, boolean keepBackup, @Nullable FileSnapshot expectedState, FileMoveOperation fileMoveOperation, FileCopyOperation backupFileCopyOperation) {
        this(path,
                pathOfTemporaryFile,
                Channels.newOutputStream(temporaryFileChannel),
                temporaryFileChannel,
                keepBackup,
                expectedState,
                fileMoveOperation,
                backupFileCopyOperation);
    }

    /// Creates a new output stream to write to or replace the file at the specified path.
    /// The backup file (.sav) is deleted when write was successful.
    ///
    /// @param path the path of the file to write to or replace
    public AtomicFileOutputStream(Path path) throws IOException {
        this(path, false);
    }

    /// Required for proper testing
    AtomicFileOutputStream(Path path, Path pathOfTemporaryFile, OutputStream temporaryFileOutputStream, boolean keepBackup) {
        this(path, pathOfTemporaryFile, temporaryFileOutputStream, keepBackup, AtomicFileOutputStream::moveAtomically, AtomicFileOutputStream::copyReplacingExisting);
    }

    /// Required for proper testing
    AtomicFileOutputStream(Path path, Path pathOfTemporaryFile, OutputStream temporaryFileOutputStream, boolean keepBackup, FileMoveOperation fileMoveOperation) {
        this(path, pathOfTemporaryFile, temporaryFileOutputStream, keepBackup, fileMoveOperation, AtomicFileOutputStream::copyReplacingExisting);
    }

    /// Required for proper testing
    AtomicFileOutputStream(Path path, Path pathOfTemporaryFile, OutputStream temporaryFileOutputStream, boolean keepBackup, FileMoveOperation fileMoveOperation, FileCopyOperation backupFileCopyOperation) {
        this(path, pathOfTemporaryFile, temporaryFileOutputStream, null, keepBackup, null, fileMoveOperation, backupFileCopyOperation);
    }

    private AtomicFileOutputStream(Path path,
                                   Path pathOfTemporaryFile,
                                   OutputStream temporaryFileOutputStream,
                                   @Nullable FileChannel temporaryFileChannel,
                                   boolean keepBackup,
                                   @Nullable FileSnapshot expectedState,
                                   FileMoveOperation fileMoveOperation,
                                   FileCopyOperation backupFileCopyOperation) {
        super(temporaryFileOutputStream);
        this.targetFile = path;
        this.temporaryFile = pathOfTemporaryFile;
        this.backupFile = getPathOfSaveBackupFile(path);
        this.keepBackup = keepBackup;
        this.temporaryFileChannel = temporaryFileChannel;
        this.fileMoveOperation = fileMoveOperation;
        this.backupFileCopyOperation = backupFileCopyOperation;
        this.expectedTargetFileState = expectedState != null ? expectedState : FileSnapshot.read(path);
    }

    /// Best-effort lost-update guard: detects whether another process modified the target file after the expected
    /// baseline state was captured. See [FileSnapshot] for the limits of the comparison; additionally, there is an
    /// unavoidable race between this check and the subsequent commit, so the check is repeated as late as possible.
    /// An unreadable current state does not abort the write (the commit itself will surface real I/O problems).
    // [impl->req~logic.exporter.concurrent-save-detection~1]
    private void ensureTargetFileUnchanged() throws FileChangedException {
        if (expectedTargetFileState == null) {
            return;
        }
        FileSnapshot currentState = FileSnapshot.read(targetFile);
        if (currentState != null && !expectedTargetFileState.equals(currentState)) {
            throw new FileChangedException(targetFile);
        }
    }

    /// Returns the state of the target file as written by this stream, captured immediately after the successful
    /// commit. Callers spanning a longer logical operation (e.g. a save that may be retried with a different encoding
    /// after a user dialog) can pass it as the expected state of a follow-up stream, so that the whole operation is
    /// guarded against concurrent writes — including the time between the two streams.
    ///
    /// @return the committed state, or `null` when the stream did not commit (yet) or the attributes could not be read
    @Nullable
    public FileSnapshot getCommittedTargetFileState() {
        return committedTargetFileState;
    }

    private static Path createTemporaryFile(Path targetFile) throws IOException {
        Path parentDirectory = targetFile.toAbsolutePath().getParent();
        return Files.createTempFile(parentDirectory, TEMPORARY_FILE_PREFIX, TEMPORARY_EXTENSION);
    }

    private static Path getPathOfSaveBackupFile(Path targetFile) {
        Path backupFile = FileUtil.addExtension(targetFile, SAVE_EXTENSION);
        return backupFile.resolveSibling(FileUtil.getValidFileName(backupFile.getFileName().toString()));
    }

    /// Returns the path of the backup copy of the original file (may not exist)
    public Path getBackup() {
        return backupFile;
    }

    /// Overridden because of cleanup actions in case of an error
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            out.write(b, off, len);
        } catch (IOException exception) {
            markWriteAsFailed();
            throw exception;
        }
    }

    /// Closes the write process to the temporary file but does not commit to the target file.
    public void abort() {
        markWriteAsFailed();
        try {
            super.close();
        } catch (IOException exception) {
            LOGGER.debug("Unable to abort writing to file {}", temporaryFile, exception);
        } finally {
            cleanup();
        }
    }

    private void markWriteAsFailed() {
        errorDuringWrite = true;
        cleanup();
    }

    private void cleanup() {
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException exception) {
            LOGGER.debug("Unable to delete file {}", temporaryFile, exception);
        }
    }

    /// perform the final operations to move the temporary file to its final destination
    @Override
    public void close() throws IOException {
        try {
            if (errorDuringWrite) {
                super.close();
                return;
            }

            try {
                // Make sure we have written everything to the temporary file
                flush();
                if (temporaryFileChannel != null) {
                    temporaryFileChannel.force(true);
                }
            } catch (IOException exception) {
                // Try to close nonetheless
                super.close();
                throw exception;
            }
            super.close();

            if (errorDuringWrite) {
                // in case there was an error during write, we do not replace the original file
                return;
            }

            // Check before creating the backup, so that no backup of a concurrently written file is left behind
            ensureTargetFileUnchanged();

            boolean mustOverwriteTargetInPlace = targetHasHardLinks() || Files.isSymbolicLink(targetFile);

            // We successfully wrote everything to the temporary file, lets copy it to the correct place
            // First, make backup of original file and try to save file permissions to restore them later (by default: 664)
            Set<PosixFilePermission> oldFilePermissions = EnumSet.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_WRITE,
                    PosixFilePermission.OTHERS_READ);
            boolean backupCreated = createBackup();
            if (Files.exists(targetFile)) {
                if (FileUtil.IS_POSIX_COMPLIANT) {
                    try {
                        oldFilePermissions = Files.getPosixFilePermissions(targetFile);
                    } catch (IOException exception) {
                        LOGGER.warn("Error getting file permissions for file {}.", targetFile, exception);
                    }
                }
            }

            // Read before the final change check, because ACL and extended-attribute I/O take time on network shares.
            // Applied to the target only after the commit: the temporary file must stay readable by this process for
            // the in-place fallbacks, which a restrictive ACL of the target could prevent.
            Map<String, Object> preservedAttributes = Files.exists(targetFile) ? readPreservableAttributes(targetFile, temporaryFile) : Map.of();

            // Re-check right before the commit: creating the backup of a large file can take a while, so the first
            // check may be long in the past by now
            try {
                ensureTargetFileUnchanged();
            } catch (FileChangedException exception) {
                // The target is untouched, so the backup written by this aborted attempt has no recovery value (unlike
                // on commit failures, where the backup is deliberately kept). With keepBackup, a backup file is
                // expected to persist across saves, so the (overwritten) one is left in place.
                if (backupCreated && !keepBackup) {
                    try {
                        Files.deleteIfExists(backupFile);
                    } catch (IOException deleteException) {
                        exception.addSuppressed(deleteException);
                    }
                }
                throw exception;
            }

            if (mustOverwriteTargetInPlace) {
                if (!backupCreated) {
                    LOGGER.warn("Could not create a backup for linked file {} (backup created: {}). Replacing the file without preserving its links.", targetFile, backupCreated);
                    moveTemporaryFileToTargetFile(backupCreated);
                } else {
                    overwriteTargetFile(backupCreated);
                }
            } else {
                // Move temporary file (replace original if it exists)
                moveTemporaryFileToTargetFile(backupCreated);
            }

            // Captured directly after the commit, so the window in which a concurrent write could be mistaken for our
            // own is as small as possible
            committedTargetFileState = FileSnapshot.read(targetFile);

            // Before the permission restore: a read-only target refuses attribute writes on some platforms
            applyAttributes(targetFile, preservedAttributes);

            // Restore file permissions
            if (FileUtil.IS_POSIX_COMPLIANT) {
                try {
                    Files.setPosixFilePermissions(targetFile, oldFilePermissions);
                } catch (IOException exception) {
                    LOGGER.warn("Error writing file permissions to file {}.", targetFile, exception);
                }
            }

            if (!keepBackup) {
                // Remove backup file for saving
                Files.deleteIfExists(backupFile);
            }
        } finally {
            // Remove temporary file (but not the backup!)
            cleanup();
        }
    }

    /// Reads everything a move cannot preserve (group, DOS attributes, ACL, user-defined extended attributes) from
    /// `file`, keyed by attribute name for [Files#setAttribute], in the order they have to be applied: the ACL
    /// after the ordinary attributes because it may revoke the access to write them, and read-only last of all.
    /// Each part is independent and best-effort: a mounted file system may lack support the default file system
    /// advertises. Ownership is not included, because restoring it needs elevated privileges. POSIX permissions are
    /// restored separately, so that they also apply to a freshly created target.
    ///
    /// The string-based attribute API is used instead of the typed views: it never yields `null` and transfers each
    /// extended attribute completely or throws, so a partial transfer cannot go unnoticed.
    ///
    /// @param replacement the file that is going to replace `file`; only DOS flags differing from its flags are included, because writing an unchanged `false` on Linux adds a DOSATTRIB xattr to every saved file
    // [impl->req~logic.exporter.preserve-file-attributes~1]
    private static Map<String, Object> readPreservableAttributes(Path file, Path replacement) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        Set<String> views = file.getFileSystem().supportedFileAttributeViews();

        if (views.contains("posix")) {
            try {
                attributes.put("posix:group", Files.getAttribute(file, "posix:group"));
            } catch (IOException | UnsupportedOperationException | SecurityException exception) {
                LOGGER.debug("Could not read group of {}", file, exception);
            }
        }

        if (views.contains("user")) {
            try {
                Files.readAttributes(file, "user:*").forEach((name, value) -> attributes.put("user:" + name, value));
            } catch (IOException | UnsupportedOperationException | SecurityException exception) {
                LOGGER.debug("Could not read extended attributes of {}", file, exception);
            }
        }

        Map<String, Object> dosFlags = Map.of();
        if (views.contains("dos")) {
            try {
                String dosFlagNames = "dos:hidden,system,archive,readonly";
                Map<String, Object> replacementFlags = Files.readAttributes(replacement, dosFlagNames);
                dosFlags = new LinkedHashMap<>(Files.readAttributes(file, dosFlagNames));
                dosFlags.entrySet().removeIf(flag -> flag.getValue().equals(replacementFlags.get(flag.getKey())));
                for (String flag : List.of("hidden", "system", "archive")) {
                    if (dosFlags.containsKey(flag)) {
                        attributes.put("dos:" + flag, dosFlags.get(flag));
                    }
                }
            } catch (IOException | UnsupportedOperationException | SecurityException exception) {
                LOGGER.debug("Could not read DOS attributes of {}", file, exception);
            }
        }

        if (views.contains("acl")) {
            try {
                attributes.put("acl:acl", Files.getAttribute(file, "acl:acl"));
            } catch (IOException | UnsupportedOperationException | SecurityException exception) {
                LOGGER.debug("Could not read ACL of {}", file, exception);
            }
        }

        if (dosFlags.containsKey("readonly")) {
            attributes.put("dos:readonly", dosFlags.get("readonly"));
        }

        return attributes;
    }

    /// Applies the attributes read by [#readPreservableAttributes(Path,Path)] in their order, skipping (and logging)
    /// each one the OS refuses, e.g. a group the user is not a member of.
    private static void applyAttributes(Path file, Map<String, Object> attributes) {
        attributes.forEach((name, value) -> {
            try {
                Files.setAttribute(file, name, value);
            } catch (IOException | UnsupportedOperationException | IllegalArgumentException | SecurityException exception) {
                LOGGER.debug("Could not set attribute {} on {}", name, file, exception);
            }
        });
    }

    private boolean createBackup() {
        // [impl->req~jabgui.autosaveandbackup.complete-backup~1]
        if (!Files.exists(targetFile)) {
            return false;
        }

        Path temporaryBackupFile = null;
        try {
            temporaryBackupFile = createTemporaryFile(backupFile);
            backupFileCopyOperation.copy(targetFile, temporaryBackupFile);
            forceFileToDisk(temporaryBackupFile);
            moveBackupFileIntoPlace(temporaryBackupFile);
            return true;
        } catch (IOException exception) {
            LOGGER.warn("Could not create backup file {} (backup created: false)", backupFile, exception);
            return false;
        } finally {
            if (temporaryBackupFile != null) {
                try {
                    Files.deleteIfExists(temporaryBackupFile);
                } catch (IOException exception) {
                    LOGGER.debug("Unable to delete temporary backup file {}", temporaryBackupFile, exception);
                }
            }
        }
    }

    private static void forceFileToDisk(Path file) throws IOException {
        try (FileChannel fileChannel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            fileChannel.force(true);
        }
    }

    private void moveBackupFileIntoPlace(Path temporaryBackupFile) throws IOException {
        try {
            Files.move(temporaryBackupFile, backupFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            LOGGER.debug("Atomic move is not supported for backup file {}. Falling back to a non-atomic move.", backupFile, exception);
            Files.move(temporaryBackupFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean targetHasHardLinks() {
        if ((!OS.LINUX && !OS.OS_X) || !Files.exists(targetFile)) {
            return false;
        }

        try {
            return ((Number) Files.getAttribute(targetFile, "unix:nlink")).longValue() > 1;
        } catch (IllegalArgumentException | UnsupportedOperationException exception) {
            LOGGER.debug("Could not determine hard-link count for {}", targetFile, exception);
            return false;
        } catch (IOException exception) {
            LOGGER.warn("Could not determine hard-link count for {}", targetFile, exception);
            return false;
        }
    }

    private void overwriteTargetFile(boolean backupCreated) throws IOException {
        try {
            copyFileToTarget(temporaryFile);
        } catch (IOException writeException) {
            if (backupCreated) {
                try {
                    copyFileToTarget(backupFile);
                } catch (IOException restoreException) {
                    writeException.addSuppressed(restoreException);
                    LOGGER.error("Could not restore file {} from backup {}", targetFile, backupFile, restoreException);
                }
            }
            throw writeException;
        }
    }

    private void copyFileToTarget(Path source) throws IOException {
        try (InputStream inputStream = Files.newInputStream(source);
             FileChannel targetFileChannel = Files.exists(targetFile)
                                             ? FileChannel.open(targetFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
                                             : FileChannel.open(targetFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            inputStream.transferTo(Channels.newOutputStream(targetFileChannel));
            targetFileChannel.force(true);
        }
    }

    /// Moves the temporary file onto the target file, replacing it.
    ///
    /// On Windows, replacing a file requires `DELETE` access to it. That fails with a sharing violation
    /// (`ERROR_SHARING_VIOLATION`) while any other process holds a handle that was opened without
    /// `FILE_SHARE_DELETE`. Qt-based editors (for example TeXstudio, which re-reads `.bib` files while the user
    /// types), anti-virus scanners and the search indexer all open files that way, typically only for a few
    /// milliseconds. Retrying briefly therefore clears virtually all of these collisions.
    ///
    /// See <[#11916](https://github.com/JabRef/jabref/issues/11916)>.
    private void moveTemporaryFileToTargetFile(boolean backupCreated) throws IOException {
        for (int attempt = 1; attempt <= MOVE_ATTEMPTS; attempt++) {
            try {
                fileMoveOperation.move(temporaryFile, targetFile);
                return;
            } catch (AtomicMoveNotSupportedException exception) {
                if (backupCreated) {
                    LOGGER.debug("Atomic move is not supported for {} (backup created: {}). Falling back to an in-place save.", targetFile, backupCreated, exception);
                    fallBackToInPlaceSave(exception);
                } else {
                    LOGGER.debug("Atomic move is not supported for {} (backup created: {}). Falling back to a non-atomic move.", targetFile, backupCreated, exception);
                    moveTemporaryFileWithoutAtomicity(exception);
                }
                return;
            } catch (FileSystemException exception) {
                if (attempt == MOVE_ATTEMPTS) {
                    if (backupCreated) {
                        LOGGER.debug("Could not move temporary file (backup created: {}). Falling back to an in-place save.", backupCreated, exception);
                        fallBackToInPlaceSave(exception);
                    } else {
                        LOGGER.debug("Could not move temporary file (backup created: {}). Falling back to a non-atomic move.", backupCreated, exception);
                        moveTemporaryFileWithoutAtomicity(exception);
                    }
                    return;
                }
                LOGGER.debug("Attempt {} of {} to move {} onto {} failed", attempt, MOVE_ATTEMPTS, temporaryFile, targetFile, exception);
                try {
                    Thread.sleep(MOVE_RETRY_INITIAL_DELAY_MILLIS << (attempt - 1));
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    InterruptedIOException interruptedIOException = new InterruptedIOException("Interrupted while moving temporary file " + temporaryFile + " onto " + targetFile);
                    interruptedIOException.initCause(interruptedException);
                    interruptedIOException.addSuppressed(exception);
                    LOGGER.warn("Interrupted while moving temporary file {} onto {}", temporaryFile, targetFile, interruptedIOException);
                    throw interruptedIOException;
                }
            }
        }
    }

    private void fallBackToInPlaceSave(IOException moveException) throws IOException {
        try {
            overwriteTargetFile(true);
        } catch (IOException fallbackException) {
            fallbackException.addSuppressed(moveException);
            throw fallbackException;
        }
    }

    private void moveTemporaryFileWithoutAtomicity(IOException atomicMoveException) throws IOException {
        try {
            Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException moveException) {
            moveException.addSuppressed(atomicMoveException);
            throw moveException;
        }
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void copyReplacingExisting(Path source, Path target) throws IOException {
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void flush() throws IOException {
        try {
            super.flush();
        } catch (IOException exception) {
            markWriteAsFailed();
            throw exception;
        }
    }

    @Override
    public void write(int b) throws IOException {
        try {
            super.write(b);
        } catch (IOException exception) {
            markWriteAsFailed();
            throw exception;
        }
    }
}
