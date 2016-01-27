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
import java.util.Optional;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FileField;

public class RelativePathsCleanup implements CleanupJob {

    private final List<String> paths;

    public RelativePathsCleanup(List<String> paths) {
        this.paths = paths;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        Optional<String> oldValue = entry.getFieldOptional(Globals.FILE_FIELD);
        if (!oldValue.isPresent()) {
            return new ArrayList<>();
        }

        List<FileField.ParsedFileField> fileList = FileField.parse(oldValue.get());
        List<FileField.ParsedFileField> newFileList = new ArrayList<>();
        boolean changed = false;
        for (FileField.ParsedFileField flEntry : fileList) {
            String oldFileName = flEntry.link;
            String newFileName = FileUtil.shortenFileName(new File(oldFileName), paths).toString();

            FileField.ParsedFileField newFlEntry = flEntry;
            if (!oldFileName.equals(newFileName)) {
                newFlEntry = new FileField.ParsedFileField(flEntry.description, newFileName, flEntry.fileType);
                changed = true;
            }
            newFileList.add(newFlEntry);
        }
        if (changed) {
            String newValue = FileField.getStringRepresentation(newFileList);
            assert (!oldValue.get().equals(newValue));
            entry.setField(Globals.FILE_FIELD, newValue);
            FieldChange change = new FieldChange(entry, Globals.FILE_FIELD, oldValue.get(), newValue);
            return Collections.singletonList(change);
        }
        return new ArrayList<>();
    }

}
