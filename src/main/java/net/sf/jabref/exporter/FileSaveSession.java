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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileBasedLock;
import net.sf.jabref.logic.util.io.FileUtil;

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
 * otherwise call cancel(). When canceling, the temporary file is simply deleted and the target file remains unchanged.
 * When committing, the temporary file is copied to the target file after making a backup if requested and if the target
 * file already existed, and finally the temporary file is deleted.
 * <p>
 * If committing fails, the temporary file will not be deleted.
 */
public class FileSaveSession extends SaveSession {

    private static final Log LOGGER = LogFactory.getLog(FileSaveSession.class);

    // Filenames.
    private static final String BACKUP_EXTENSION = ".bak";
    private static final String TEMP_PREFIX = "jabref";
    private static final String TEMP_SUFFIX = "save.bib";
    private final boolean useLockFile;
    private Path temporaryFile;

    public FileSaveSession(Charset encoding, boolean backup) throws SaveException {
        this(encoding, backup, createTemporaryFile());
    }

    public FileSaveSession(Charset encoding, boolean backup, Path temporaryFile) throws SaveException {
        super(encoding, backup, getWriterForFile(encoding, temporaryFile));
        this.temporaryFile = temporaryFile;
        this.useLockFile = Globals.prefs.getBoolean(JabRefPreferences.USE_LOCK_FILES);
    }

    private static VerifyingWriter getWriterForFile(Charset encoding, Path file) throws SaveException {
        try {
            return new VerifyingWriter(new FileOutputStream(file.toFile()), encoding);
        } catch (FileNotFoundException e) {
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
    public void commit(File file) throws SaveException {
        if (file == null) {
            return;
        }
        if (file.exists() && backup) {
            String name = file.getName();
            String path = file.getParent();
            File backupFile = new File(path, name + BACKUP_EXTENSION);
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
                    if (FileBasedLock.createLockFile(file)) {
                        // Oops, the lock file already existed. Try to wait it out:
                        if (!FileBasedLock.waitForFileLock(file, 10)) {
                            throw SaveException.FILE_LOCKED;
                        }
                    }
                } catch (IOException ex) {
                    LOGGER.error("Error when creating lock file.", ex);
                }
            }

            FileUtil.copyFile(temporaryFile.toFile(), file, true);
        } catch (IOException ex2) {
            // If something happens here, what can we do to correct the problem? The file is corrupted, but we still
            // have a clean copy in tmp. However, we just failed to copy tmp to file, so it's not likely that
            // repeating the action will have a different result.
            // On the other hand, our temporary file should still be clean, and won't be deleted.
            throw new SaveException("Save failed while committing changes: " + ex2.getMessage(),
                    Localization.lang("Save failed while committing changes: %0", ex2.getMessage()));
        } finally {
            if (useLockFile) {
                FileBasedLock.deleteLockFile(file);
            }
        }
        try {
            Files.delete(temporaryFile);
        } catch (IOException e) {
            LOGGER.warn("Cannot delete temporary file", e);
        }
    }

    @Override
    public void cancel() {
        try {
            Files.delete(temporaryFile);
        } catch (IOException e) {
            LOGGER.warn("Cannot delete temporary file", e);
        }
    }
}
