package net.sf.jabref.collab;

import net.sf.jabref.Globals;
import net.sf.jabref.Util;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * This thread monitors a set of files, each associated with a FileUpdateListener, for changes
* in the file's last modification time stamp. The
 */
public class FileUpdateMonitor extends Thread {

  final int WAIT = 4000;
  static int tmpNum = 0;
  int no = 0;
  HashMap<String, Entry> entries = new HashMap<String, Entry>();
  boolean running;

  public FileUpdateMonitor() {
    setPriority(MIN_PRIORITY);
  }

  public void run() {
    running = true;

    // The running variable is used to make the thread stop when needed.
    while (running) {
      //System.out.println("Polling...");
      Iterator<String> i = entries.keySet().iterator();
      for (;i.hasNext();) {
        Entry e = entries.get(i.next());
        try {
          if (e.hasBeenUpdated())
            e.notifyListener();

          //else
          //System.out.println("File '"+e.file.getPath()+"' not modified.");
        } catch (IOException ex) {
          e.notifyFileRemoved();
        }
      }

      // Sleep for a while before starting a new polling round.
      try {
        sleep(WAIT);
      } catch (InterruptedException ex) {
      }
    }
  }

  /**
   * Cause the thread to stop monitoring. It will finish the current round before stopping.
   */
  public void stopMonitoring() {
    running = false;
  }

  /**
   * Add a new file to monitor. Returns a handle for accessing the entry.
   * @param ul FileUpdateListener The listener to notify when the file changes.
   * @param file File The file to monitor.
   * @throws IOException if the file does not exist.
   */
  public String addUpdateListener(FileUpdateListener ul, File file) throws IOException {
     // System.out.println(file.getPath());
    if (!file.exists())
      throw new IOException("File not found");
    no++;
    String key = ""+no;
    entries.put(key, new Entry(ul, file));
    return key;
  }

    /**
     * Forces a check on the file, and returns the result. Does not
     * force a report to all listeners before the next routine check.
     */
    public boolean hasBeenModified(String handle) throws IllegalArgumentException {
	Object o = entries.get(handle);
	if (o == null)
            return false;
        //	    throw new IllegalArgumentException("Entry not found");
	try {
	    return ((Entry)o).hasBeenUpdated();
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
        if (o == null)
            return;
        ((Entry)o).timeStamp--;
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
    if (o == null)
      throw new IllegalArgumentException("Entry not found");
    Entry entry = (Entry)o;
    entry.updateTimeStamp();

  }

  public void changeFile(String key, File file) throws IOException, IllegalArgumentException {
    if (!file.exists())
      throw new IOException("File not found");
    Object o = entries.get(key);
    if (o == null)
      throw new IllegalArgumentException("Entry not found");
    ((Entry)o).file = file;
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
    if (o == null)
      throw new IllegalArgumentException("Entry not found");
    return ((Entry)o).tmpFile;
  }

  /**
   * A class containing the File, the FileUpdateListener and the current time stamp for one file.
   */
  class Entry {
    FileUpdateListener listener;
    File file;
    File tmpFile;
    long timeStamp, fileSize;

    public Entry(FileUpdateListener ul, File f) {
      listener = ul;
      file = f;
      timeStamp = file.lastModified();
      fileSize = file.length();
      tmpFile = getTempFile();
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
      if (modified == 0L)
        throw new IOException("File deleted");
      return timeStamp != modified || fileSize != fileSizeNow;
    }

    public void updateTimeStamp() {
      timeStamp = file.lastModified();
      if (timeStamp == 0L)
        notifyFileRemoved();
      fileSize = file.length();

      copy();
    }

    public boolean copy() {
	
	//Util.pr("<copy file=\""+tmpFile.getPath()+"\">");
      boolean res = false;
      try {
        res = Util.copyFile(file, tmpFile, true);
      } catch (IOException ex) {
        Globals.logger("Cannot copy to temporary file '"+tmpFile.getPath()+"'");
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

  static synchronized File getTempFile() {
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
