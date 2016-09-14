package net.sf.jabref.gui.importer;

import java.io.File;
import java.io.FileFilter;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.util.io.DatabaseFileLookup;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;

/**
 * {@link FileFilter} implementation, that allows only files which are not
 * linked in any of the {@link BibEntry}s of the specified
 * {@link BibDatabase}. <br>
 * <br>
 * This {@link FileFilter} sits on top of another {@link FileFilter}
 * -implementation, which it first consults. Only if this major filefilter
 * has accepted a file, this implementation will verify on that file.
 *
 * @author Nosh&Dan
 * @version 12.11.2008 | 02:00:15
 *
 */
public class UnlinkedPDFFileFilter implements FileFilter {

    private final DatabaseFileLookup lookup;
    private final FileFilter fileFilter;


    public UnlinkedPDFFileFilter(FileFilter fileFilter, BibDatabaseContext databaseContext) {
        this.fileFilter = fileFilter;
        this.lookup = new DatabaseFileLookup(databaseContext, Globals.prefs.getFileDirectoryPreferences());
    }

    @Override
    public boolean accept(File pathname) {
        return fileFilter.accept(pathname) && !lookup.lookupDatabase(pathname);
    }
}
