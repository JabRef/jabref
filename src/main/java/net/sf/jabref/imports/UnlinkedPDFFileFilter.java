/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
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

    @Override
    public boolean accept(File pathname) {
        if (fileFilter.accept(pathname)) {
            return !lookup.lookupDatabase(pathname);
        }
        return false;
    }
}
