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

import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.io.FileUtil;

import net.harawata.appdirs.AppDirsFactory;
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
    private static final String BACKUP_EXTENSION = ".bak";

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
        super(Files.newOutputStream(getPathOfTemporaryFile(path)));

        this.targetFile = path;
        this.temporaryFile = getPathOfTemporaryFile(path);
        this.backupFile = getPathOfBackupFile(path);
        this.keepBackup = keepBackup;

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
     * Creates a new output stream to write to or replace the file at the specified path. The backup file is deleted when the write was successful.
     *
     * @param path the path of the file to write to or replace
     */
    public AtomicFileOutputStream(Path path) throws IOException {
        this(path, false);
    }

    private static Path getBibsPath() {
        // We choose the cache dir as it is a local-to-app temporary directory
        Path directory = Path.of(AppDirsFactory.getInstance().getUserCacheDir(
                                     "jabref",
                                     new BuildInfo().version.toString(),
                                     "org.jabref"))
                             .resolve("bibs");
        return directory;
    }

    private static Path getPathOfSecondFile(Path targetFile, String extension) {
        Path directory = getBibsPath();
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            Path result = FileUtil.addExtension(targetFile, extension);
            LOGGER.warn("Could not create bib writing directory {}, using {} as file", directory, result, e);
            return result;
        }
        // By using a part of the hex string, we keep some old versions, but not all
        String fileName = FileUtil.getBaseName(targetFile) + "-" + Integer.toHexString(targetFile.hashCode()).substring(0, 2) + extension + ".bib";
        return directory.resolve(fileName);
    }

    static Path getPathOfTemporaryFile(Path targetFile) {
        return getPathOfSecondFile(targetFile, TEMPORARY_EXTENSION);
    }

    static Path getPathOfBackupFile(Path targetFile) {
        return getPathOfSecondFile(targetFile, BACKUP_EXTENSION);
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

            if (!keepBackup) {
                Files.deleteIfExists(backupFile);
            }
        } finally {
            cleanup();
        }
    }

    private void replaceOriginalFileByWrittenFile() throws IOException {
        // Move temporary file (replace original if it exists)
        // We implement the move as write into the original and delete the temporary one to keep file permissions etc.
        try (InputStream inputStream = Files.newInputStream(temporaryFile);
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

