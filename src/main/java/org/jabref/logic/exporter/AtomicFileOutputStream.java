package org.jabref.logic.exporter;

import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

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
 * <li>Create a backup (with .bak suffix) of the original file (if it exists) in the <a href="https://github.com/harawata/appdirs#supported-directories">UserDataDir</a>.</li>
 * <li>Write to a temporary file (with .tmp suffix) in the system-wide temporary directory.</li>
 * <li>Copy the content of the temporary file to the .bib file, overwriting any content that already exists in that file.</li>
 * <li>Delete the temporary file.</li>
 * <li>Rename the .bak to .old.</li>
 * </ol>
 * If all goes well, no temporary or backup files will remain on disk after closing the stream.
 * <p>
 * Errors are handled as follows:
 * <ol>
 * <li>If anything goes wrong while writing to the temporary file, the temporary file will be deleted (leaving the
 * original file untouched).</li>
 * <li>If anything goes wrong while copying the temporary file to the target file, the backup of the original file is written into the original file.</li>
 * <li>If that rescue goes wrong, JabRef knows at the start that there is a .bak file and will prompt the user.</li>
 * </ol>
 */
public class AtomicFileOutputStream extends FilterOutputStream {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtomicFileOutputStream.class);

    private static final String TEMPORARY_EXTENSION = ".tmp";
    static final String BACKUP_EXTENSION = ".bak";
    private static final String OLD_EXTENSION = ".old";

    /**
     * The file we want to create/replace.
     */
    private final Path targetFile;

    /**
     * The file to which writes are redirected to.
     */
    private final Path temporaryFile;

    // The lock can be a local variable, because the locking is done by the operating system (and not a Java-based lock)
    private final FileLock temporaryFileLock;

    /**
     * A backup of the target file (if it exists), created when the stream is closed
     */
    private final Path backupFile;

    private boolean errorDuringWrite = false;

    /**
     * Creates a new output stream to write to or replace the file at the specified path.
     *
     * @param path       the path of the file to write to or replace
     */
    public AtomicFileOutputStream(Path path) throws IOException {
        super(Files.newOutputStream(getPathOfTemporaryFile(path)));

        this.targetFile = path;
        this.temporaryFile = getPathOfTemporaryFile(path);
        this.backupFile = FileUtil.getPathOfBackupFileAndCreateDirectory(path, BackupFileType.BACKUP);

        if (Files.exists(targetFile)) {
            // Make a backup of the original file
            Files.copy(targetFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
        }

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

    /**
     * Determines the path of the temporary file.
     */
    static Path getPathOfTemporaryFile(Path targetFile) {
        Path tempFile;
        try {
            tempFile = Files.createTempFile(targetFile.getFileName().toString(), TEMPORARY_EXTENSION);
        } catch (IOException e) {
            Path result = FileUtil.addExtension(targetFile, TEMPORARY_EXTENSION);
            LOGGER.warn("Could not create bib writing temporary file, using {} as file", result, e);
            return result;
        }
        return tempFile;
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
            LOGGER.warn("Unable to release lock on file {}", temporaryFile, exception);
        }
    }

    // perform the final operations to move the temporary file to its final destination
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

            if (!errorDuringWrite) {
                // We successfully wrote everything to the temporary file, let's copy it to the correct place
                replaceOriginalFileByWrittenFile();
            }
        } finally {
            cleanup();
        }
    }

    private void replaceOriginalFileByWrittenFile() throws IOException {
        // Move temporary file (replace original if it exists)
        // We implement the move as write into the original and delete the temporary one to keep file permissions etc.
        try (InputStream inputStream = Files.newInputStream(temporaryFile, StandardOpenOption.DELETE_ON_CLOSE);
             OutputStream outputStream = Files.newOutputStream(targetFile)) {
            inputStream.transferTo(outputStream);
        } catch (IOException e) {
            LOGGER.error("Could not write into final .bib file {}", targetFile, e);
            if (Files.size(targetFile) != Files.size(backupFile)) {
                LOGGER.debug("Size of target file and backup file differ. Trying to restore target file from backup.");
                LOGGER.info("Trying to restore backup from {} to {}", backupFile, targetFile);
                try (InputStream inputStream = Files.newInputStream(backupFile);
                     OutputStream outputStream = Files.newOutputStream(targetFile)) {
                    inputStream.transferTo(outputStream);
                } catch (IOException ex) {
                    LOGGER.error("Could not restore backup from {} to {}", backupFile, targetFile, ex);
                    throw ex;
                }
                LOGGER.info("Backup restored");
            }
            // we rethrow the original error to indicate that writing (somehow) went wrong
            throw e;
        }
        String filenameOld = FileUtil.getBaseName(backupFile) + OLD_EXTENSION;
        try {
            Files.move(backupFile, backupFile.resolveSibling(filenameOld), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            LOGGER.warn("Could not rename {} to {}", backupFile, filenameOld, e);
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
            errorDuringWrite = true;
            throw exception;
        }
    }
}

