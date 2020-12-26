package org.jabref.gui.externalfiles;

import java.io.File;
import java.io.FileFilter;

import org.jabref.logic.util.io.DatabaseFileLookup;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.FilePreferences;

/**
 * {@link FileFilter} implementation, that allows only files which are not
 * linked in any of the {@link BibEntry}s of the specified
 * {@link BibDatabase}. <br>
 * <br>
 * This {@link FileFilter} sits on top of another {@link FileFilter}
 * -implementation, which it first consults. Only if this major filefilter
 * has accepted a file, this implementation will verify on that file.
 */
public class UnlinkedPDFFileFilter implements FileFilter {

    private final DatabaseFileLookup lookup;
    private final FileFilter fileFilter;

    public UnlinkedPDFFileFilter(FileFilter fileFilter, BibDatabaseContext databaseContext, FilePreferences filePreferences) {
        this.fileFilter = fileFilter;
        this.lookup = new DatabaseFileLookup(databaseContext, filePreferences);
    }

    @Override
    public boolean accept(File pathname) {
        return fileFilter.accept(pathname) && !lookup.lookupDatabase(pathname);
    }
}
