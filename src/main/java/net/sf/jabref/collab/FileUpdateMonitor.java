/*  Copyright (C) 2003-2011 JabRef contributors.
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

import net.sf.jabref.util.FileUtil;
import net.sf.jabref.Globals;

import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * This thread monitors a set of files, each associated with a FileUpdateListener, for changes
 * in the file's last modification time stamp. The
 */
public class FileUpdateMonitor implements Runnable {

    private static final Logger logger = Logger.getLogger(FileUpdateMonitor.class.getName());

    private static final int WAIT = 4000;

    private int numberOfUpdateListener = 0;
    private final HashMap<String, Entry> entries = new HashMap<String, Entry>();

    @Override
    public void run() {
        // The running variable is used to make the thread stop when needed.
        while (true) {
            //System.out.println("Polling...");
            Iterator<String> i = entries.keySet().iterator();
            for (; i.hasNext();) {
                Entry e = entries.get(i.next());
                try {
                    if (e.hasBeenUpdated()) {
                        e.notifyListener();
                    }

                    //else
                    //System.out.println("File '"+e.file.getPath()+"' not modified.");
                } catch (IOException ex) {
                    e.notifyFileRemoved();
                }
            }

            // Sleep for a while before starting a new polling round.
            try {
                Thread.sleep(WAIT);
            } catch (InterruptedException ex) {
                FileUpdateMonitor.logger.finest("FileUpdateMonitor has been interrupted.");
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
        String key = "" + numberOfUpdateListener;
        entries.put(key, new Entry(ul, file));
        return key;
    }

    /**
     * Forces a check on the file, and returns the result. Does not
     * force a report to all listeners before the next routine check.
     */
    public boolean hasBeenModified(String handle) throws IllegalArgumentException {
        Object o = entries.get(handle);
        if (o == null) {
            return false;
        }
        //	    throw new IllegalArgumentException("Entry not found");
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
        ((Entry) o).timeStamp--;
    }

    /**
     * Removes a listener from the monitor.
     * @param handle String The handle for the listener to remove.
     */
    public void removeUpdateListener(String handle) {
        entries.remove(handle);
    }

    public void updateTimeStamp(String key) throws IllegalArgumentException {
        Object o = entries.get(key);
        if (o == null) {
            throw new IllegalArgumentException("Entry not found");
        }
        Entry entry = (Entry) o;
        entry.updateTimeStamp();

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
        return ((Entry) o).tmpFile;
    }


    /**
     * A class containing the File, the FileUpdateListener and the current time stamp for one file.
     */
    static class Entry {

        final FileUpdateListener listener;
        final File file;
        final File tmpFile;
        long timeStamp, fileSize;


        public Entry(FileUpdateListener ul, File f) {
            listener = ul;
            file = f;
            timeStamp = file.lastModified();
            fileSize = file.length();
            tmpFile = FileUpdateMonitor.getTempFile();
            tmpFile.deleteOnExit();
            copy();
        }

        /**
         * Check if time stamp or the file size has changed.
         * @throws IOException if the file does no longer exist.
         * @return boolean true if the file has changed.
         */
        public boolean hasBeenUpdated() throws IOException {
            long modified = file.lastModified();
            long fileSizeNow = file.length();
            if (modified == 0L) {
                throw new IOException("File deleted");
            }
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

            //Util.pr("<copy file=\""+tmpFile.getPath()+"\">");
            boolean res = false;
            try {
                res = FileUtil.copyFile(file, tmpFile, true);
            } catch (IOException ex) {
                Globals.logger("Cannot copy to temporary file '" + tmpFile.getPath() + '\'');
            }
            //Util.pr("</copy>");
            return res;

            //return true;
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

        /*public void finalize() {
          try {
            tmpFile.delete();
          } catch (Throwable e) {
            Globals.logger("Cannot delete temporary file '"+tmpFile.getPath()+"'");
          }
        }*/
    }


    private static synchronized File getTempFile() {
        File f = null;
        // Globals.prefs.get("tempDir")
        //while ((f = File.createTempFile("jabref"+(tmpNum++), null)).exists());
        try {
            f = File.createTempFile("jabref", null);
            f.deleteOnExit();
            //System.out.println(f.getPath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return f;
    }
}
