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
package net.sf.jabref.exporter;

import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileBasedLock;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.Globals;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class used to handle safe storage to disk.
 * <p>
 * Usage: create a SaveSession giving the file to save to, the encoding, and whether to make a backup. The SaveSession
 * will provide a Writer to store to, which actually goes to a temporary file. The Writer keeps track of whether all
 * characters could be saved, and if not, which characters were not encodable.
 * <p>
 * After saving is finished, the client should close the Writer. If the save should be put into effect, call commit(),
 * otherwise call cancel(). When cancelling, the temporary file is simply deleted and the target file remains unchanged.
 * When committing, the temporary file is copied to the target file after making a backup if requested and if the target
 * file already existed, and finally the temporary file is deleted.
 * <p>
 * If committing fails, the temporary file will not be deleted.
 */
public class SaveSession {
    private static final Log LOGGER = LogFactory.getLog(SaveSession.class);

    public static final String LOCKFILE_SUFFIX = ".lock";

    // The age in ms of a lockfile before JabRef will offer to "steal" the locked file:
    public static final long LOCKFILE_CRITICAL_AGE = 60000;
    private static final String TEMP_PREFIX = "jabref";

    private static final String TEMP_SUFFIX = "save.bib";
    private final File tmp;
    private final Charset encoding;
    private boolean backup;
    private final boolean useLockFile;

    private final VerifyingWriter writer;

    private final List<FieldChange> undoableFieldChanges = new ArrayList<>();


    public SaveSession(Charset encoding, boolean backup) throws IOException {
        tmp = File.createTempFile(SaveSession.TEMP_PREFIX, SaveSession.TEMP_SUFFIX);
        useLockFile = Globals.prefs.getBoolean(JabRefPreferences.USE_LOCK_FILES);
        this.backup = backup;
        this.encoding = encoding;
    /* Using
	   try (FileOutputStream fos = new FileOutputStream(tmp)) {
	       writer = new VerifyingWriter(fos, encoding);
	   }
	   doesn't work since fos is closed after assigning write,
	   leading to that fos may never be closed at all
	 */
        writer = new VerifyingWriter(new FileOutputStream(tmp), encoding);
    }

    public VerifyingWriter getWriter() {
        return writer;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public void setUseBackup(boolean useBackup) {
        this.backup = useBackup;
    }

    public void commit(File file) throws SaveException {
        if (file == null) {
            return;
        }
        if (file.exists() && backup) {
            String name = file.getName();
            String path = file.getParent();
            File backupFile = new File(path, name + GUIGlobals.BACKUP_EXTENSION);
            try {
                FileUtil.copyFile(file, backupFile, true);
            } catch (IOException ex) {
                LOGGER.error("Problem copying file", ex);
                throw SaveException.BACKUP_CREATION;
            }
        }
        try {
            if (useLockFile) {
                try {
                    if (createLockFile(file)) {
                        // Oops, the lock file already existed. Try to wait it out:
                        if (!FileBasedLock.waitForFileLock(file, 10)) {
                            throw SaveException.FILE_LOCKED;
                        }

                    }
                } catch (IOException ex) {
                    LOGGER.error("Error when creating lock file.", ex);
                }
            }

            FileUtil.copyFile(tmp, file, true);
        } catch (IOException ex2) {
            // If something happens here, what can we do to correct the problem? The file is corrupted, but we still
            // have a clean copy in tmp. However, we just failed to copy tmp to file, so it's not likely that
            // repeating the action will have a different result.
            // On the other hand, our temporary file should still be clean, and won't be deleted.
            throw new SaveException("Save failed while committing changes: " + ex2.getMessage(),
                    Localization.lang("Save failed while committing changes: %0", ex2.getMessage()));
        } finally {
            if (useLockFile) {
                deleteLockFile(file);
            }
        }
        if (!tmp.delete()) {
            LOGGER.info("Cannot delete temporary file");
        }
    }

    public void cancel() {
        if (!tmp.delete()) {
            LOGGER.info("Cannot delete temporary file");
        }
    }

    /**
     * Check if a lock file exists, and create it if it doesn't.
     *
     * @return true if the lock file already existed
     * @throws IOException if something happens during creation.
     */
    private boolean createLockFile(File file) throws IOException {
        File lock = new File(file.getPath() + SaveSession.LOCKFILE_SUFFIX);
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

    /**
     * Check if a lock file exists, and delete it if it does.
     *
     * @return true if the lock file existed, false otherwise.
     * @throws IOException if something goes wrong.
     */
    private boolean deleteLockFile(File file) {
        File lock = new File(file.getPath() + SaveSession.LOCKFILE_SUFFIX);
        if (!lock.exists()) {
            return false;
        }
        if (!lock.delete()) {
            LOGGER.info("Cannot delete lock file");
        }
        return true;
    }

    public File getTemporaryFile() {
        return tmp;
    }

    public List<FieldChange> getFieldChanges() {
        return undoableFieldChanges;
    }

    public void addFieldChanges(List<FieldChange> newUndoableFieldChanges) {
        this.undoableFieldChanges.addAll(newUndoableFieldChanges);
    }
}
