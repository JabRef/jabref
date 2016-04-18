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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.TypedBibEntry;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.ParsedFileField;

public class MoveFilesCleanup implements CleanupJob {

    private final BibDatabaseContext databaseContext;

    public MoveFilesCleanup(BibDatabaseContext databaseContext) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        if(!databaseContext.getMetaData().getDefaultFileDirectory().isPresent()) {
            return new ArrayList<>();
        }

        List<String> paths = databaseContext.getFileDirectory();
        String defaultFileDirectory = databaseContext.getMetaData().getDefaultFileDirectory().get();
        Optional<File> targetDirectory = FileUtil.expandFilename(defaultFileDirectory, paths);
        if(!targetDirectory.isPresent()) {
            return new ArrayList<>();
        }

        TypedBibEntry typedEntry = new TypedBibEntry(entry, databaseContext);
        List<ParsedFileField> fileList = typedEntry.getFiles();
        List<ParsedFileField> newFileList = new ArrayList<>();
        boolean changed = false;
        for (ParsedFileField fileEntry : fileList) {
            String oldFileName = fileEntry.getLink();

            Optional<File> oldFile = FileUtil.expandFilename(oldFileName, paths);
            if(!oldFile.isPresent() || !oldFile.get().exists()) {
                continue;
            }

            File targetFile = new File(targetDirectory.get(), oldFile.get().getName());
            if(targetFile.exists()) {
                // We do not overwrite already existing files
                continue;
            }

            oldFile.get().renameTo(targetFile);
            String newFileName = targetFile.getName();

            ParsedFileField newFileEntry = fileEntry;
            if (!oldFileName.equals(newFileName)) {
                newFileEntry = new ParsedFileField(fileEntry.getDescription(), newFileName, fileEntry.getFileType());
                changed = true;
            }
            newFileList.add(newFileEntry);
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
