package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

import org.jabref.logic.util.io.FileBasedLock;
import org.jabref.logic.util.io.FileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to handle safe storage to disk.
 * <p>
 * Usage: create a SaveSession giving the file to save to, the encoding, and whether to make a backup. The SaveSession
 * will provide a Writer to store to, which actually goes to a temporary file. The Writer keeps track of whether all
 * characters could be saved, and if not, which characters were not encodable.
 * <p>
 * After saving is finished, the client should close the Writer. If the save should be put into effect, call commit(),
 * otherwise call cancel(). When canceling, the temporary file is simply deleted and the target file remains unchanged.
 * When committing, the temporary file is copied to the target file after making a backup if requested and if the target
 * file already existed, and finally the temporary file is deleted.
 * <p>
 * If committing fails, the temporary file will not be deleted.
 */
public class FileSaveSession extends SaveSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSaveSession.class);

    // Filenames.
    private static final String BACKUP_EXTENSION = ".bak";
    private static final String TEMP_PREFIX = "jabref";
    private static final String TEMP_SUFFIX = "save.bib";
    private final Path temporaryFile;

    public FileSaveSession(Charset encoding, boolean backup) throws SaveException {
        this(encoding, backup, createTemporaryFile());
    }

    public FileSaveSession(Charset encoding, boolean backup, Path temporaryFile) throws SaveException {
        super(encoding, backup, getWriterForFile(encoding, temporaryFile));
        this.temporaryFile = temporaryFile;
    }

    private static VerifyingWriter getWriterForFile(Charset encoding, Path file) throws SaveException {
        try {
            return new VerifyingWriter(Files.newOutputStream(file), encoding);
        } catch (IOException e) {
            throw new SaveException(e);
        }
    }

    private static Path createTemporaryFile() throws SaveException {
        try {
            return Files.createTempFile(FileSaveSession.TEMP_PREFIX, FileSaveSession.TEMP_SUFFIX);
        } catch (IOException e) {
            throw new SaveException(e);
        }
    }

    @Override
    public void commit(Path file) throws SaveException {
        if (file == null) {
            return;
        }
        if (backup && Files.exists(file)) {
            Path backupFile = FileUtil.addExtension(file, BACKUP_EXTENSION);
            FileUtil.copyFile(file, backupFile, true);
        }
        try {
            // Always use a lock file
            try {
                if (FileBasedLock.createLockFile(file)) {
                    // Oops, the lock file already existed. Try to wait it out:
                    if (!FileBasedLock.waitForFileLock(file)) {
                        throw SaveException.FILE_LOCKED;
                    }
                }
            } catch (IOException ex) {
                LOGGER.error("Error when creating lock file.", ex);
            }

            // Try to save file permissions to restore them later (by default: 664)
            Set<PosixFilePermission> oldFilePermissions = EnumSet.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_WRITE,
                    PosixFilePermission.OTHERS_READ);
            if (FileUtil.IS_POSIX_COMPILANT && Files.exists(file)) {
                try {
                    oldFilePermissions = Files.getPosixFilePermissions(file);
                } catch (IOException exception) {
                    LOGGER.warn("Error getting file permissions.", exception);
                }
            }

            FileUtil.copyFile(temporaryFile, file, true);

            // Restore file permissions
            if (FileUtil.IS_POSIX_COMPILANT) {
                try {
                    Files.setPosixFilePermissions(file, oldFilePermissions);
                } catch (IOException exception) {
                    throw new SaveException(exception);
                }
            }
        } finally {
            FileBasedLock.deleteLockFile(file);
        }
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException e) {
            LOGGER.warn("Cannot delete temporary file", e);
        }
    }

    @Override
    public void cancel() {
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException e) {
            LOGGER.warn("Cannot delete temporary file", e);
        }
    }
}
