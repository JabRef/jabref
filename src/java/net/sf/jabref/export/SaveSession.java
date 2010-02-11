package net.sf.jabref.export;

import net.sf.jabref.Globals;
import net.sf.jabref.Util;
import net.sf.jabref.GUIGlobals;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Class used to handle safe storage to disk. 
 * 
 * Usage: create a SaveSession giving the file to save to, the
 * encoding, and whether to make a backup. The SaveSession will provide a Writer to store to, which actually
 * goes to a temporary file. The Writer keeps track of whether all characters could be saved, and if not,
 * which characters were not encodable.
 * 
 * After saving is finished, the client should close the Writer. If the save should be put into effect, call
 * commit(), otherwise call cancel(). When cancelling, the temporary file is simply deleted and the target
 * file remains unchanged. When committing, the temporary file is copied to the target file after making
 * a backup if requested and if the target file already existed, and finally the temporary file is deleted.
 * 
 * If committing fails, the temporary file will not be deleted.
 */
public class SaveSession {

    public static final String LOCKFILE_SUFFIX = ".lock";
    // The age in ms of a lockfile before JabRef will offer to "steal" the locked file:
    public static final long LOCKFILE_CRITICAL_AGE = 60000;

    private static final String TEMP_PREFIX = "jabref";
    private static final String TEMP_SUFFIX = "save.bib";

    File file, tmp, backupFile;
    String encoding;
    boolean backup, useLockFile;
    VerifyingWriter writer;

    public SaveSession(File file, String encoding, boolean backup) throws IOException,
        UnsupportedCharsetException {
        this.file = file;
        tmp = File.createTempFile(TEMP_PREFIX, TEMP_SUFFIX);
        useLockFile = Globals.prefs.getBoolean("useLockFiles");
        this.backup = backup;
        this.encoding = encoding;
        writer = new VerifyingWriter(new FileOutputStream(tmp), encoding);
    }

    public VerifyingWriter getWriter() {
        return writer;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setUseBackup(boolean useBackup) {
        this.backup = useBackup;
    }

    public void commit() throws SaveException {
        if (file == null)
            return;
        if (file.exists() && backup) {
            String name = file.getName();
            String path = file.getParent();
            File backupFile = new File(path, name + GUIGlobals.backupExt);
            try {
                Util.copyFile(file, backupFile, true);
            } catch (IOException ex) {
                ex.printStackTrace();
                throw SaveException.BACKUP_CREATION;
                //throw new SaveException(Globals.lang("Save failed during backup creation")+": "+ex.getMessage());
            }
        }
        try {
            if (useLockFile) {
                try {
                    if (createLockFile()) {
                        // Oops, the lock file already existed. Try to wait it out:
                        if (!Util.waitForFileLock(file, 10))
                            throw SaveException.FILE_LOCKED;

                    }
                } catch (IOException ex) {
                    System.err.println("Error when creating lock file");
                    ex.printStackTrace();
                }
            }

            Util.copyFile(tmp, file, true);
        } catch (IOException ex2) {
            // If something happens here, what can we do to correct the problem? The file is corrupted, but we still
            // have a clean copy in tmp. However, we just failed to copy tmp to file, so it's not likely that
            // repeating the action will have a different result.
            // On the other hand, our temporary file should still be clean, and won't be deleted.
            throw new SaveException(Globals.lang("Save failed while committing changes")+": "+ex2.getMessage());
        } finally {
            if (useLockFile) {
                try {
                    deleteLockFile();
                } catch (IOException ex) {
                    System.err.println("Error when deleting lock file");
                    ex.printStackTrace();
                }
            }
        }

        tmp.delete();
    }

    public void cancel() throws IOException {
        tmp.delete();
    }


    /**
     * Check if a lock file exists, and create it if it doesn't.
     * @return true if the lock file already existed
     * @throws IOException if something happens during creation.
     */
    private boolean createLockFile() throws IOException {
        File lock = new File(file.getPath()+LOCKFILE_SUFFIX);
        if (lock.exists()) {
            return true;
        }
        FileOutputStream out = new FileOutputStream(lock);
        out.write(0);
        try {
            out.close();
        } catch (IOException ex) {
            System.err.println("Error when creating lock file");
            ex.printStackTrace();
        }
        lock.deleteOnExit();
        return false;
    }

    /**
     * Check if a lock file exists, and delete it if it does.
     * @return true if the lock file existed, false otherwise.
     * @throws IOException if something goes wrong.
     */
    private boolean deleteLockFile() throws IOException {
        File lock = new File(file.getPath()+LOCKFILE_SUFFIX);
        if (!lock.exists()) {
            return false;
        }
        lock.delete();
        return true;
    }

    public File getTemporaryFile() {
        return tmp;
    }
}
