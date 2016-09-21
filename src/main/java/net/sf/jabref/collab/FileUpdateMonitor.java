package net.sf.jabref.collab;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.sf.jabref.logic.util.io.FileUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This thread monitors a set of files, each associated with a FileUpdateListener, for changes
 * in the file's last modification time stamp. The
 */
public class FileUpdateMonitor implements Runnable {
    private static final Log LOGGER = LogFactory.getLog(FileUpdateMonitor.class);

    private static final int WAIT = 4000;

    private int numberOfUpdateListener;
    private final Map<String, Entry> entries = new HashMap<>();

    @Override
    public void run() {
        // The running variable is used to make the thread stop when needed.
        while (true) {
            for(Entry e : entries.values()) {
                try {
                    if (e.hasBeenUpdated()) {
                        e.notifyListener();
                    }
                } catch (IOException ex) {
                    e.notifyFileRemoved();
                }
            }

            // Sleep for a while before starting a new polling round.
            try {
                Thread.sleep(WAIT);
            } catch (InterruptedException ex) {
                LOGGER.debug("FileUpdateMonitor has been interrupted. Terminating...", ex);
                return;
            }
        }
    }

    /**
     * Add a new file to monitor. Returns a handle for accessing the entry.
     * @param ul FileUpdateListener The listener to notify when the file changes.
     * @param file File The file to monitor.
     * @throws IOException if the file does not exist.
     */
    public String addUpdateListener(FileUpdateListener ul, File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File not found");
        }
        numberOfUpdateListener++;
        String key = String.valueOf(numberOfUpdateListener);
        entries.put(key, new Entry(ul, file.toPath()));
        return key;
    }

    /**
     * Forces a check on the file, and returns the result. Does not
     * force a report to all listeners before the next routine check.
     */
    public boolean hasBeenModified(String handle) {
        Entry entry = entries.get(handle);
        if (entry == null) {
            return false;
        }
        try {
            return entry.hasBeenUpdated();
        } catch (IOException ex) {
            // Thrown if file has been removed. We return false.
            return false;
        }
    }

    /**
     * Change the stored timestamp for the given file. If the timestamp equals
     * the file's timestamp on disk, after this call the file will appear to
     * have been modified. Used if a file has been modified, and the change
     * scan fails, in order to ensure successive checks.
     * @param handle the handle to the correct file.
     */
    public void perturbTimestamp(String handle) {
        Entry entry = entries.get(handle);
        if (entry != null) {
            entry.decreaseTimeStamp();
        }
    }

    /**
     * Removes a listener from the monitor.
     * @param handle String The handle for the listener to remove.
     */
    public void removeUpdateListener(String handle) {
        entries.remove(handle);
    }

    public void updateTimeStamp(String key) {
        Entry entry = entries.get(key);
        if (entry != null) {
            try {
                entry.updateTimeStamp();
            } catch (IOException e) {
                LOGGER.error("Couldn't update timestamp", e);
            }
        }
    }

    /**
     * Method for getting the temporary file used for this database. The tempfile
     * is used for comparison with the changed on-disk version.
     * @param key String The handle for this monitor.
     * @throws IllegalArgumentException If the handle doesn't correspond to an entry.
     * @return File The temporary file.
     */
    public Path getTempFile(String key) throws IllegalArgumentException {
        Entry entry = entries.get(key);
        if (entry == null) {
            throw new IllegalArgumentException("Entry not found");
        }
        return entry.getTmpFile();
    }


    /**
     * A class containing the File, the FileUpdateListener and the current time stamp for one file.
     */
    static class Entry {

        private final FileUpdateListener listener;
        private final Path file;
        private final Path tmpFile;
        private long timeStamp;
        private long fileSize;


        public Entry(FileUpdateListener ul, Path f) throws IOException {
            listener = ul;
            file = f;
            timeStamp = Files.getLastModifiedTime(file).toMillis();
            fileSize = Files.size(file);
            tmpFile = FileUpdateMonitor.getTempFile();
            if (tmpFile != null) {
                tmpFile.toFile().deleteOnExit();
                copy();
            }
        }

        /**
         * Check if time stamp or the file size has changed.
         * @throws IOException if the file does no longer exist.
         * @return boolean true if the file has changed.
         */
        public boolean hasBeenUpdated() throws IOException {
            long modified = Files.getLastModifiedTime(file).toMillis();
            if (modified == 0L) {
                throw new IOException("File deleted");
            }
            long fileSizeNow = Files.size(file);
            return (timeStamp != modified) || (fileSize != fileSizeNow);
        }

        public void updateTimeStamp() throws IOException {
            timeStamp = Files.getLastModifiedTime(file).toMillis();
            if (timeStamp == 0L) {
                notifyFileRemoved();
            }
            fileSize = Files.size(file);

            copy();
        }

        public boolean copy() {

            boolean res = FileUtil.copyFile(file, tmpFile, true);
            return res;
        }

        /**
         * Call the listener method to signal that the file has changed.
         */
        public void notifyListener() throws IOException {
            // Update time stamp.
            timeStamp = Files.getLastModifiedTime(file).toMillis();
            fileSize = Files.size(file);
            listener.fileUpdated();
        }

        /**
         * Call the listener method to signal that the file has been removed.
         */
        public void notifyFileRemoved() {
            listener.fileRemoved();
        }

        public Path getTmpFile() {
            return tmpFile;
        }

        public void decreaseTimeStamp() {
            timeStamp--;
        }
    }


    private static synchronized Path getTempFile() {
        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile("jabref", null);
            temporaryFile.toFile().deleteOnExit();
        } catch (IOException ex) {
            LOGGER.warn("Could not create temporary file.", ex);
        }
        return temporaryFile;
    }
}
