package net.sf.jabref.importer;

import java.io.File;
import java.io.FileFilter;

import net.sf.jabref.model.database.BibDatabase;

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

    public UnlinkedPDFFileFilter(FileFilter fileFilter, BibDatabase database) {
        this.fileFilter = fileFilter;
        this.lookup = new DatabaseFileLookup(database);
    }

    @Override
    public boolean accept(File pathname) {
        return fileFilter.accept(pathname) && !lookup.lookupDatabase(pathname);
    }
}
