package org.jabref.logic.exporter;

import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.io.FileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A file output stream that is similar to the standard {@link FileOutputStream}, except that all writes are first
 * redirected to a temporary file. When the stream is closed, the temporary file (atomically) replaces the target file.
 *
 * <p>
 * In detail, the strategy is to:
 * <ol>
 * <li>Write to a temporary file (with .tmp suffix) in the same directory as the destination file.</li>
 * <li>Create a backup (with .bak suffix) of the original file (if it exists) in the same directory.</li>
 * <li>Move the temporary file to the correct place, overwriting any file that already exists at that location.</li>
 * <li>Delete the backup file (if configured to do so).</li>
 * </ol>
 * If all goes well, no temporary or backup files will remain on disk after closing the stream.
 * <p>
 * Errors are handled as follows:
 * <ol>
 * <li>If anything goes wrong while writing to the temporary file, the temporary file will be deleted (leaving the
 * original file untouched).</li>
 * <li>If anything goes wrong while copying the temporary file to the target file, the backup of the original file is
 * kept.</li>
 * </ol>
 * <p>
 * Implementation inspired by code from <a href="https://github.com/martylamb/atomicfileoutputstream/blob/master/src/main/java/com/martiansoftware/io/AtomicFileOutputStream.java">Marty
 * Lamb</a> and <a href="https://github.com/apache/zookeeper/blob/master/src/java/main/org/apache/zookeeper/common/AtomicFileOutputStream.java">Apache</a>.
 */
public class AtomicFileOutputStream extends FilterOutputStream {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtomicFileOutputStream.class);

    private static final String TEMPORARY_EXTENSION = ".tmp";
    private static final String SAVE_EXTENSION = "." + BackupFileType.SAVE.getExtensions().get(0);

    /**
     * The file we want to create/replace.
     */
    private final Path targetFile;

    /**
     * The file to which writes are redirected to.
     */
    private final Path temporaryFile;

    private final FileLock temporaryFileLock;
    /**
     * A backup of the target file (if it exists), created when the stream is closed
     */
    private final Path backupFile;

    private final boolean keepBackup;

    private boolean errorDuringWrite = false;

    /**
     * Creates a new output stream to write to or replace the file at the specified path.
     *
     * @param path       the path of the file to write to or replace
     * @param keepBackup whether to keep the backup file after a successful write process
     */
    public AtomicFileOutputStream(Path path, boolean keepBackup) throws IOException {
        // Files.newOutputStream(getPathOfTemporaryFile(path)) leads to a "sun.nio.ch.ChannelOutputStream", which does not offer "lock"
        this(path, getPathOfTemporaryFile(path), new FileOutputStream(getPathOfTemporaryFile(path).toFile()), keepBackup);
    }

    /**
     * Creates a new output stream to write to or replace the file at the specified path.
     * The backup file is deleted when write was successful.
     *
     * @param path the path of the file to write to or replace
     */
    public AtomicFileOutputStream(Path path) throws IOException {
        this(path, false);
    }

    /**
     * Required for proper testing
     */
    AtomicFileOutputStream(Path path, Path pathOfTemporaryFile, OutputStream temporaryFileOutputStream, boolean keepBackup) throws IOException {
        super(temporaryFileOutputStream);
        this.targetFile = path;
        this.temporaryFile = pathOfTemporaryFile;
        this.backupFile = getPathOfSaveBackupFile(path);
        this.keepBackup = keepBackup;

        try {
            // Lock files (so that at least not another JabRef instance writes at the same time to the same tmp file)
            if (out instanceof FileOutputStream) {
                temporaryFileLock = ((FileOutputStream) out).getChannel().lock();
            } else {
                temporaryFileLock = null;
            }
        } catch (OverlappingFileLockException exception) {
            throw new IOException("Could not obtain write access to " + temporaryFile + ". Maybe another instance of JabRef is currently writing to the same file?", exception);
        }
    }

    private static Path getPathOfTemporaryFile(Path targetFile) {
        return FileUtil.addExtension(targetFile, TEMPORARY_EXTENSION);
    }

    private static Path getPathOfSaveBackupFile(Path targetFile) {
        return FileUtil.addExtension(targetFile, SAVE_EXTENSION);
    }

    /**
     * Returns the path of the backup copy of the original file (may not exist)
     */
    public Path getBackup() {
        return backupFile;
    }

    /**
     * Overridden because of cleanup actions in case of an error
     */
    @Override
    public void write(byte b[], int off, int len) throws IOException {
        try {
            out.write(b, off, len);
        } catch (IOException exception) {
            cleanup();
            errorDuringWrite = true;
            throw exception;
        }
    }

    /**
     * Closes the write process to the temporary file but does not commit to the target file.
     */
    public void abort() {
        errorDuringWrite = true;
        try {
            super.close();
            Files.deleteIfExists(temporaryFile);
            Files.deleteIfExists(backupFile);
        } catch (IOException exception) {
            LOGGER.debug("Unable to abort writing to file {}", temporaryFile, exception);
        }
    }

    private void cleanup() {
        try {
            if (temporaryFileLock != null) {
                temporaryFileLock.release();
            }
        } catch (IOException exception) {
            // Currently, we always get the exception:
            // Unable to release lock on file C:\Users\koppor\AppData\Local\Temp\junit11976839611279549873\error-during-save.txt.tmp: java.nio.channels.ClosedChannelException
            LOGGER.debug("Unable to release lock on file {}", temporaryFile, exception);
        }
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException exception) {
            LOGGER.debug("Unable to delete file {}", temporaryFile, exception);
        }
    }

    /**
     * perform the final operations to move the temporary file to its final destination
     */
    @Override
    public void close() throws IOException {
        try {
            try {
                // Make sure we have written everything to the temporary file
                flush();
                if (out instanceof FileOutputStream) {
                    ((FileOutputStream) out).getFD().sync();
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

            // We successfully wrote everything to the temporary file, lets copy it to the correct place
            // First, make backup of original file and try to save file permissions to restore them later (by default: 664)
            Set<PosixFilePermission> oldFilePermissions = EnumSet.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_WRITE,
                    PosixFilePermission.OTHERS_READ);
            if (Files.exists(targetFile)) {
                try {
                    Files.copy(targetFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    LOGGER.warn("Could not create backup file {}", backupFile);
                }
                if (FileUtil.IS_POSIX_COMPLIANT) {
                    try {
                        oldFilePermissions = Files.getPosixFilePermissions(targetFile);
                    } catch (IOException exception) {
                        LOGGER.warn("Error getting file permissions for file {}.", targetFile, exception);
                    }
                }
            }

            try {
                // Move temporary file (replace original if it exists)
                Files.move(temporaryFile, targetFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                LOGGER.warn("Could not move temporary file", e);
                throw e;
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
                // Remove backup file
                Files.deleteIfExists(backupFile);
            }
        } finally {
            // Remove temporary file (but not the backup!)
            cleanup();
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            super.flush();
        } catch (IOException exception) {
            cleanup();
            throw exception;
        }
    }

    @Override
    public void write(int b) throws IOException {
        try {
            super.write(b);
        } catch (IOException exception) {
            cleanup();
            throw exception;
        }
    }
}

