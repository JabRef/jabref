/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.logic.cleanup;

import java.io.File;
import java.util.*;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.ParsedFileField;
import net.sf.jabref.logic.TypedBibEntry;

public class RelativePathsCleanup implements CleanupJob {

    private final BibDatabaseContext databaseContext;

    public RelativePathsCleanup(BibDatabaseContext databaseContext) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        TypedBibEntry typedEntry = new TypedBibEntry(entry, databaseContext);
        List<ParsedFileField> fileList = typedEntry.getFiles();
        List<ParsedFileField> newFileList = new ArrayList<>();
        boolean changed = false;
        for (ParsedFileField flEntry : fileList) {
            String oldFileName = flEntry.link;
            List<String> paths = databaseContext.getMetaData().getFileDirectory();
            String newFileName = FileUtil.shortenFileName(new File(oldFileName), paths).toString();

            ParsedFileField newFlEntry = flEntry;
            if (!oldFileName.equals(newFileName)) {
                newFlEntry = new ParsedFileField(flEntry.description, newFileName, flEntry.fileType);
                changed = true;
            }
            newFileList.add(newFlEntry);
        }
        if (changed) {
            Optional<FieldChange> change = typedEntry.setFiles(newFileList);
            if(change.isPresent()) {
                return Collections.singletonList(change.get());
            } else {
                return Collections.emptyList();
            }
        }
        return new ArrayList<>();
    }

}
