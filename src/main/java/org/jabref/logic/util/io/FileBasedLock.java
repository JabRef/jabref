package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileBasedLock {
    /**
     * The age in ms of a lockfile before JabRef will offer to "steal" the locked file.
     */
    public static final long LOCKFILE_CRITICAL_AGE = 60000;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileBasedLock.class);
    private static final String LOCKFILE_SUFFIX = ".lock";

    // default retry count for aquiring file lock
    private static final int AQUIRE_LOCK_RETRY = 10;

    private FileBasedLock() {
    }

    /**
     * This method checks whether there is a lock file for the given file. If
     * there is, it waits for 500 ms. This is repeated until the lock is gone
     * or we have waited the maximum number of times.
     *
     * @param file The file to check the lock for.
     * @param maxWaitCount The maximum number of times to wait.
     * @return true if the lock file is gone, false if it is still there.
     */
    private static boolean waitForFileLock(Path file, int maxWaitCount) {
        // Check if the file is locked by another JabRef user:
        int lockCheckCount = 0;
        while (hasLockFile(file)) {

            if (lockCheckCount++ == maxWaitCount) {
                return false;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
                // Ignored
            }
        }
        return true;
    }

    public static boolean waitForFileLock(Path file) {
        return waitForFileLock(file, AQUIRE_LOCK_RETRY);
    }

    /**
     * Check whether a lock file exists for this file.
     * @param file The file to check.
     * @return true if a lock file exists, false otherwise.
     */
    public static boolean hasLockFile(Path file) {
        Path lockFile = getLockFilePath(file);
        return Files.exists(lockFile);
    }

    /**
     * Find the lock file's last modified time, if it has a lock file.
     * @param file The file to check.
     * @return the last modified time if lock file exists, empty optional otherwise.
     */
    public static Optional<FileTime> getLockFileTimeStamp(Path file) {
        Path lockFile = getLockFilePath(file);
        try {
            return Files.exists(lockFile) ?
                    Optional.of(Files.readAttributes(lockFile, BasicFileAttributes.class).lastModifiedTime()) :
                    Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Check if a lock file exists, and delete it if it does.
     *
     * @return true if the lock file existed, false otherwise.
     */
    public static boolean deleteLockFile(Path file) {
        Path lockFile = getLockFilePath(file);
        if (!Files.exists(lockFile)) {
            return false;
        }
        try {
            Files.delete(lockFile);
        } catch (IOException e) {
            LOGGER.warn("Cannot delete lock file", e);
        }
        return true;
    }

    /**
     * Check if a lock file exists, and create it if it doesn't.
     *
     * @return true if the lock file already existed
     * @throws IOException if something happens during creation.
     */
    public static boolean createLockFile(Path file) throws IOException {
        Path lockFile = getLockFilePath(file);
        if (Files.exists(lockFile)) {
            return true;
        }

        try {
            Files.write(lockFile, "0".getBytes());
        } catch (IOException ex) {
            LOGGER.error("Error when creating lock file.", ex);
        }
        lockFile.toFile().deleteOnExit();
        return false;
    }

    private static Path getLockFilePath(Path file) {
        return file.resolveSibling(file.getFileName() + LOCKFILE_SUFFIX);
    }
}
