/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.collab;

import net.sf.jabref.logic.util.io.FileUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.IOException;

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
        entries.put(key, new Entry(ul, file));
        return key;
    }

    /**
     * Forces a check on the file, and returns the result. Does not
     * force a report to all listeners before the next routine check.
     */
    public boolean hasBeenModified(String handle) {
        Object o = entries.get(handle);
        if (o == null) {
            return false;
        }
        try {
            return ((Entry) o).hasBeenUpdated();
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
        Object o = entries.get(handle);
        if (o == null) {
            return;
        }
        ((Entry) o).decreaseTimeStamp();
    }

    /**
     * Removes a listener from the monitor.
     * @param handle String The handle for the listener to remove.
     */
    public void removeUpdateListener(String handle) {
        entries.remove(handle);
    }

    public void updateTimeStamp(String key) {
        Object o = entries.get(key);
        if (o != null) {
            Entry entry = (Entry) o;
            entry.updateTimeStamp();
        }
    }

    /**
     * Method for getting the temporary file used for this database. The tempfile
     * is used for comparison with the changed on-disk version.
     * @param key String The handle for this monitor.
     * @throws IllegalArgumentException If the handle doesn't correspond to an entry.
     * @return File The temporary file.
     */
    public File getTempFile(String key) throws IllegalArgumentException {
        Object o = entries.get(key);
        if (o == null) {
            throw new IllegalArgumentException("Entry not found");
        }
        return ((Entry) o).getTmpFile();
    }


    /**
     * A class containing the File, the FileUpdateListener and the current time stamp for one file.
     */
    static class Entry {

        private final FileUpdateListener listener;
        private final File file;
        private final File tmpFile;
        private long timeStamp;
        private long fileSize;


        public Entry(FileUpdateListener ul, File f) {
            listener = ul;
            file = f;
            timeStamp = file.lastModified();
            fileSize = file.length();
            tmpFile = FileUpdateMonitor.getTempFile();
            if (tmpFile != null) {
                tmpFile.deleteOnExit();
                copy();
            }
        }

        /**
         * Check if time stamp or the file size has changed.
         * @throws IOException if the file does no longer exist.
         * @return boolean true if the file has changed.
         */
        public boolean hasBeenUpdated() throws IOException {
            long modified = file.lastModified();
            if (modified == 0L) {
                throw new IOException("File deleted");
            }
            long fileSizeNow = file.length();
            return (timeStamp != modified) || (fileSize != fileSizeNow);
        }

        public void updateTimeStamp() {
            timeStamp = file.lastModified();
            if (timeStamp == 0L) {
                notifyFileRemoved();
            }
            fileSize = file.length();

            copy();
        }

        public boolean copy() {

            boolean res = false;
            try {
                res = FileUtil.copyFile(file, tmpFile, true);
            } catch (IOException ex) {
                LOGGER.info("Cannot copy to temporary file '" + tmpFile.getPath() + '\'', ex);
            }
            return res;
        }

        /**
         * Call the listener method to signal that the file has changed.
         */
        public void notifyListener() {
            // Update time stamp.
            timeStamp = file.lastModified();
            fileSize = file.length();
            listener.fileUpdated();
        }

        /**
         * Call the listener method to signal that the file has been removed.
         */
        public void notifyFileRemoved() {
            listener.fileRemoved();
        }

        public File getTmpFile() {
            return tmpFile;
        }

        public void decreaseTimeStamp() {
            timeStamp--;
        }
    }


    private static synchronized File getTempFile() {
        File f = null;
        try {
            f = File.createTempFile("jabref", null);
            f.deleteOnExit();
        } catch (IOException ex) {
            LOGGER.warn("Could not create temporary file.", ex);
        }
        return f;
    }
}
