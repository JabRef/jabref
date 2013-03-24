package net.sf.jabref.imports;

import java.io.File;
import java.io.FileFilter;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;

/**
 * {@link FileFilter} implementation, that allows only files which are not
 * linked in any of the {@link BibtexEntry}s of the specified
 * {@link BibtexDatabase}. <br>
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

    public UnlinkedPDFFileFilter(FileFilter aFileFilter, BibtexDatabase database) {
        this.fileFilter = aFileFilter;
        this.lookup = new DatabaseFileLookup(database);
    }

    public boolean accept(File pathname) {
        if (fileFilter.accept(pathname))
            return !lookup.lookupDatabase(pathname);
        return false;
    }
};
