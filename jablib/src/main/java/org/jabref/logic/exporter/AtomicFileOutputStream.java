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
import java.util.Set;

import org.jabref.logic.os.OS;
import org.jabref.logic.util.BackupFileType;
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
/// overwrite is also used when the file system does not support atomic moves.
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
///
/// Implementation inspired by code from [Marty Lamb](https://github.com/martylamb/atomicfileoutputstream/blob/master/src/main/java/com/martiansoftware/io/AtomicFileOutputStream.java) and [Apache](https://github.com/apache/zookeeper/blob/master/src/java/main/org/apache/zookeeper/common/AtomicFileOutputStream.java).
@NullMarked
public class AtomicFileOutputStream extends FilterOutputStream {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtomicFileOutputStream.class);

    private static final String TEMPORARY_EXTENSION = ".tmp";
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
        this(path, createTemporaryFile(path), keepBackup, AtomicFileOutputStream::moveAtomically, AtomicFileOutputStream::copyReplacingExisting);
    }

    /// The temporary file is opened as a [FileChannel], because the channel is needed for [FileChannel#force(boolean)].
    /// `Files.newOutputStream(...)` returns a `sun.nio.ch.ChannelOutputStream`, which does not offer it.
    private AtomicFileOutputStream(Path path, Path pathOfTemporaryFile, boolean keepBackup, FileMoveOperation fileMoveOperation, FileCopyOperation backupFileCopyOperation) throws IOException {
        this(path,
                pathOfTemporaryFile,
                FileChannel.open(pathOfTemporaryFile, StandardOpenOption.WRITE),
                keepBackup,
                fileMoveOperation,
                backupFileCopyOperation);
    }

    private AtomicFileOutputStream(Path path, Path pathOfTemporaryFile, FileChannel temporaryFileChannel, boolean keepBackup, FileMoveOperation fileMoveOperation, FileCopyOperation backupFileCopyOperation) throws IOException {
        this(path,
                pathOfTemporaryFile,
                Channels.newOutputStream(temporaryFileChannel),
                temporaryFileChannel,
                keepBackup,
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
    AtomicFileOutputStream(Path path, Path pathOfTemporaryFile, OutputStream temporaryFileOutputStream, boolean keepBackup) throws IOException {
        this(path, pathOfTemporaryFile, temporaryFileOutputStream, keepBackup, AtomicFileOutputStream::moveAtomically, AtomicFileOutputStream::copyReplacingExisting);
    }

    /// Required for proper testing
    AtomicFileOutputStream(Path path, Path pathOfTemporaryFile, OutputStream temporaryFileOutputStream, boolean keepBackup, FileMoveOperation fileMoveOperation) throws IOException {
        this(path, pathOfTemporaryFile, temporaryFileOutputStream, keepBackup, fileMoveOperation, AtomicFileOutputStream::copyReplacingExisting);
    }

    /// Required for proper testing
    AtomicFileOutputStream(Path path, Path pathOfTemporaryFile, OutputStream temporaryFileOutputStream, boolean keepBackup, FileMoveOperation fileMoveOperation, FileCopyOperation backupFileCopyOperation) throws IOException {
        this(path, pathOfTemporaryFile, temporaryFileOutputStream, null, keepBackup, fileMoveOperation, backupFileCopyOperation);
    }

    private AtomicFileOutputStream(Path path,
                                   Path pathOfTemporaryFile,
                                   OutputStream temporaryFileOutputStream,
                                   @Nullable FileChannel temporaryFileChannel,
                                   boolean keepBackup,
                                   FileMoveOperation fileMoveOperation,
                                   FileCopyOperation backupFileCopyOperation) throws IOException {
        super(temporaryFileOutputStream);
        this.targetFile = path;
        this.temporaryFile = pathOfTemporaryFile;
        this.backupFile = getPathOfSaveBackupFile(path);
        this.keepBackup = keepBackup;
        this.temporaryFileChannel = temporaryFileChannel;
        this.fileMoveOperation = fileMoveOperation;
        this.backupFileCopyOperation = backupFileCopyOperation;
    }

    private static Path createTemporaryFile(Path targetFile) throws IOException {
        Path parentDirectory = targetFile.toAbsolutePath().getParent();
        String prefix = "jabref-" + targetFile.getFileName() + "-";
        return Files.createTempFile(parentDirectory, prefix, TEMPORARY_EXTENSION);
    }

    private static Path getPathOfSaveBackupFile(Path targetFile) {
        return FileUtil.addExtension(targetFile, SAVE_EXTENSION);
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

    private boolean createBackup() {
        if (!Files.exists(targetFile)) {
            return false;
        }

        try {
            backupFileCopyOperation.copy(targetFile, backupFile);
            return true;
        } catch (IOException exception) {
            LOGGER.warn("Could not create backup file {} (backup created: false)", backupFile, exception);
            return false;
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
