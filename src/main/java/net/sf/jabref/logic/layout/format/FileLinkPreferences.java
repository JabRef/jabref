package net.sf.jabref.logic.layout.format;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.preferences.JabRefPreferences;

public class FileLinkPreferences {

    private final List<String> generatedDirForDatabase;
    private final List<String> fileDirForDatabase;


    public FileLinkPreferences(List<String> generatedDirForDatabase, List<String> fileDirForDatabase) {
        this.generatedDirForDatabase = generatedDirForDatabase;
        this.fileDirForDatabase = fileDirForDatabase;
    }

    public static FileLinkPreferences fromPreferences(JabRefPreferences prefs) {
        return new FileLinkPreferences(Collections.singletonList(prefs.get(Globals.FILE_FIELD + Globals.DIR_SUFFIX)),
                prefs.fileDirForDatabase);
    }

    public List<String> getGeneratedDirForDatabase() {
        return generatedDirForDatabase;
    }

    public List<String> getFileDirForDatabase() {
        return fileDirForDatabase;
    }
}
