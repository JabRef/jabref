package net.sf.jabref.logic.util.io;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.entry.ParsedFileField;
import net.sf.jabref.model.metadata.FileDirectoryPreferences;

/**
 * Search class for files. <br>
 * <br>
 * This class provides some functionality to search in a {@link BibDatabase} for
 * files. <br>

 * @author Nosh&Dan
 */
public class DatabaseFileLookup {

    private final Set<File> fileCache = new HashSet<>();

    private final List<String> possibleFilePaths;

    /**
     * Creates an instance by passing a {@link BibDatabase} which will be used for the searches.
     *
     * @param database A {@link BibDatabase}.
     */
    public DatabaseFileLookup(BibDatabaseContext databaseContext, FileDirectoryPreferences fileDirectoryPreferences) {
        Objects.requireNonNull(databaseContext);
        possibleFilePaths = Optional.ofNullable(databaseContext.getFileDirectory(fileDirectoryPreferences))
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
     * @param file
     *            A {@link File} Object.
     * @return <code>true</code>, if the file Object is stored in at least one
     *         entry in the database, otherwise <code>false</code>.
     */
    public boolean lookupDatabase(File file) {
        return fileCache.contains(file);
    }

    private List<File> parseFileField(BibEntry entry) {
        Objects.requireNonNull(entry);

        List<ParsedFileField> entries = FileField.parse(entry.getField(FieldName.FILE).orElse(null));

        List<File> fileLinks = new ArrayList<>();
        for (ParsedFileField field : entries) {
            String link = field.getLink();

            // Do not query external file links (huge performance leak)
            if(link.contains("//")) {
                continue;
            }

            FileUtil.expandFilename(link, possibleFilePaths).ifPresent(fileLinks::add);
        }

        return fileLinks;
    }
}
