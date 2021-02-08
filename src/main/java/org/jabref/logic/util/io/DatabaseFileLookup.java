package org.jabref.logic.util.io;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.FilePreferences;

/**
 * Search class for files. <br>
 * <br>
 * This class provides some functionality to search in a {@link BibDatabase} for files. <br>
 */
public class DatabaseFileLookup {

    private final Set<Path> fileCache = new HashSet<>();

    private final List<Path> possibleFilePaths;

    /**
     * Creates an instance by passing a {@link BibDatabase} which will be used for the searches.
     */
    public DatabaseFileLookup(BibDatabaseContext databaseContext, FilePreferences filePreferences) {
        Objects.requireNonNull(databaseContext);
        possibleFilePaths = Optional.ofNullable(databaseContext.getFileDirectories(filePreferences))
                                    .orElse(new ArrayList<>());

        for (BibEntry entry : databaseContext.getDatabase().getEntries()) {
            fileCache.addAll(parseFileField(entry));
        }
    }

    /**
     * Returns whether the File <code>file</code> is present in the database
     * as an attached File to an {@link BibEntry}. <br>
     * <br>
     * To do this, the field specified by the key <b>file</b> will be searched
     * for the provided file for every {@link BibEntry} in the database. <br>
     * <br>
     * For the matching, the absolute file paths will be used.
     *
     * @param pathname A {@link File} Object.
     * @return <code>true</code>, if the file Object is stored in at least one
     * entry in the database, otherwise <code>false</code>.
     */
    public boolean lookupDatabase(Path pathname) {
        return fileCache.contains(pathname);
    }

    private List<Path> parseFileField(BibEntry entry) {
        Objects.requireNonNull(entry);

        return entry.getFiles().stream()
                    .filter(file -> !file.isOnlineLink()) // Do not query external file links (huge performance leak)
                    .map(file -> file.findIn(possibleFilePaths))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
    }
}
