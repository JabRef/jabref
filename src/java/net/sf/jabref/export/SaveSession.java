package net.sf.jabref.export;

import net.sf.jabref.Globals;
import net.sf.jabref.Util;
import net.sf.jabref.GUIGlobals;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;

/**
 * Class used to handle safe storage to disk. Usage: create a SaveSession giving the file to save to, the
 * encoding, and whether to make a backup. The SaveSession will provide a Writer to store to, which actually
 * goes to a temporary file. The Writer keeps track of whether all characters could be saved, and if not,
 * which characters were not encodable.
 * After saving is finished, the client should close the Writer. If the save should be put into effect, call
 * commit(), otherwise call cancel(). When cancelling, the temporary file is simply deleted and the target
 * file remains unchanged. When committing, the temporary file is copied to the target file after making
 * a backup if requested and if the target file already existed, and finally the temporary file is deleted.
 * If committing fails, the temporary file will not be deleted.
 */
public class SaveSession {

    private static final String TEMP_PREFIX = "jabref";
    private static final String TEMP_SUFFIX = "save.bib";
    File file, tmp, backupFile;
    String encoding;
    boolean backup;
    VerifyingWriter writer;

    public SaveSession(File file, String encoding, boolean backup) throws IOException {
        this.file = file;
        tmp = File.createTempFile(TEMP_PREFIX, TEMP_SUFFIX);
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

    public void commit() throws SaveException {
        if (file.exists() && backup) {
            String name = file.getName();
            String path = file.getParent();
            File backupFile = new File(path, name + GUIGlobals.backupExt);
            try {
                Util.copyFile(file, backupFile, true);
            } catch (IOException ex) {
                throw new SaveException(Globals.lang("Save failed during backup creation")+": "+ex.getMessage());
            }
        }
        try {
            Util.copyFile(tmp, file, true);
        } catch (IOException ex2) {
            // If something happens here, what can we do to correct the problem? The file is corrupted, but we still
            // have a clean copy in tmp. However, we just failed to copy tmp to file, so it's not likely that
            // repeating the action will have a different result.
            // On the other hand, our temporary file should still be clean, and won't be deleted.
            throw new SaveException(Globals.lang("Save failed while committing changes")+": "+ex2.getMessage());
        }

        tmp.delete();
    }

    public void cancel() throws IOException {
        tmp.delete();
    }

    public File getTemporaryFile() {
        return tmp;
    }
}
