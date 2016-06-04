package net.sf.jabref.logic.util.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileBasedLock {

    /**
     * The age in ms of a lockfile before JabRef will offer to "steal" the locked file.
     */
    public static final long LOCKFILE_CRITICAL_AGE = 60000;
    private static final String LOCKFILE_SUFFIX = ".lock";
    private static final Log LOGGER = LogFactory.getLog(FileBasedLock.class);


    /**
     * This method checks whether there is a lock file for the given file. If
     * there is, it waits for 500 ms. This is repeated until the lock is gone
     * or we have waited the maximum number of times.
     *
     * @param file The file to check the lock for.
     * @param maxWaitCount The maximum number of times to wait.
     * @return true if the lock file is gone, false if it is still there.
     */
    public static boolean waitForFileLock(File file, int maxWaitCount) {
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

    /**
     * Check whether a lock file exists for this file.
     * @param file The file to check.
     * @return true if a lock file exists, false otherwise.
     */
    public static boolean hasLockFile(File file) {
        File lock = new File(file.getPath() + LOCKFILE_SUFFIX);
        return lock.exists();
    }

    /**
     * Find the lock file's last modified time, if it has a lock file.
     * @param file The file to check.
     * @return the last modified time if lock file exists, -1 otherwise.
     */
    public static long getLockFileTimeStamp(File file) {
        File lock = new File(file.getPath() + LOCKFILE_SUFFIX);
        return lock.exists() ? lock.lastModified() : -1;
    }

    /**
     * Check if a lock file exists, and delete it if it does.
     *
     * @return true if the lock file existed, false otherwise.
     */
    public static boolean deleteLockFile(File file) {
        File lock = new File(file.getPath() + LOCKFILE_SUFFIX);
        if (!lock.exists()) {
            return false;
        }
        if (!lock.delete()) {
            LOGGER.warn("Cannot delete lock file");
        }
        return true;
    }

    /**
     * Check if a lock file exists, and create it if it doesn't.
     *
     * @return true if the lock file already existed
     * @throws IOException if something happens during creation.
     */
    public static boolean createLockFile(File file) throws IOException {
        File lock = new File(file.getPath() + LOCKFILE_SUFFIX);
        if (lock.exists()) {
            return true;
        }
        try (FileOutputStream out = new FileOutputStream(lock)) {
            out.write(0);
            out.close();
        } catch (IOException ex) {
            LOGGER.error("Error when creating lock file.", ex);
        }
        lock.deleteOnExit();
        return false;
    }
}
