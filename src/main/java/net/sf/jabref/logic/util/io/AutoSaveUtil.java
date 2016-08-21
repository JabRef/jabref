package net.sf.jabref.logic.util.io;

import java.io.File;

public class AutoSaveUtil {

    /**
     * Get a File object pointing to the autosave file corresponding to the given file.
     * @param f The database file.
     * @return its corresponding autosave file.
     */
    public static File getAutoSaveFile(File f) {
        return new File(f.getParentFile(), ".$" + f.getName() + '$');
    }

    /**
     * Check if a newer autosave exists for the given file.
     * @param f The file to check.
     * @return true if an autosave is found, and if the autosave is newer
     *   than the given file.
     */
    public static boolean newerAutoSaveExists(File f) {
        File asFile = getAutoSaveFile(f);
        return asFile.exists() && (asFile.lastModified() > f.lastModified());
    }

}
