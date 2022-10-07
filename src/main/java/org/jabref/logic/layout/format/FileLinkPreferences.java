package org.jabref.logic.layout.format;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class FileLinkPreferences {

    private final Optional<Path> mainFileDirectory;
    private final List<Path> fileDirForDatabase;

    public FileLinkPreferences(Optional<Path> mainFileDirectory, List<Path> fileDirForDatabase) {
        this.mainFileDirectory = mainFileDirectory;
        this.fileDirForDatabase = fileDirForDatabase;
    }

    public Optional<Path> getMainFileDirectory() {
        return mainFileDirectory;
    }

    /**
     * The following field is used as a global variable during the export of a database.
     * By setting this field to the path of the database's default file directory, formatters
     * that should resolve external file paths can access this field. This is an ugly hack
     * to solve the problem of formatters not having access to any context except for the
     * string to be formatted and possible formatter arguments.
     *
     * See also {@link org.jabref.preferences.JabRefPreferences#fileDirForDatabase}
     */
    public List<Path> getFileDirForDatabase() {
        return fileDirForDatabase;
    }
}
