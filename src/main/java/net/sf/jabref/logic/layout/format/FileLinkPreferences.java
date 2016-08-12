package net.sf.jabref.logic.layout.format;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.preferences.JabRefPreferences;

public class FileLinkPreferences {

    private final List<String> generatedDirForDatabase;
    private final List<String> fileDirForDatabase;
    public static final String DIR_SUFFIX = "Directory";


    public FileLinkPreferences(List<String> generatedDirForDatabase, List<String> fileDirForDatabase) {
        this.generatedDirForDatabase = generatedDirForDatabase;
        this.fileDirForDatabase = fileDirForDatabase;
    }

    public static FileLinkPreferences fromPreferences(JabRefPreferences prefs) {
        return new FileLinkPreferences(Collections.singletonList(prefs.get(FieldName.FILE + FileLinkPreferences.DIR_SUFFIX)),
                prefs.fileDirForDatabase);
    }

    public List<String> getGeneratedDirForDatabase() {
        return generatedDirForDatabase;
    }

    public List<String> getFileDirForDatabase() {
        return fileDirForDatabase;
    }
}
