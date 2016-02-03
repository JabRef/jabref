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

import net.sf.jabref.Globals;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RenamePdfCleanup implements CleanupJob {

    private final List<String> paths;
    private final BibDatabase database;
    private final Boolean onlyRelativePaths;
    private int unsuccessfulRenames;

    public RenamePdfCleanup(List<String> paths, Boolean onlyRelativePaths, BibDatabase database) {
        this.paths = paths;
        this.database = database;
        this.onlyRelativePaths = onlyRelativePaths;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        //Extract the path
        Optional<String> oldValue = entry.getFieldOptional(Globals.FILE_FIELD);
        if (!oldValue.isPresent()) {
            return new ArrayList<>();
        }

        List<FileField.ParsedFileField> fileList = FileField.parse(oldValue.get());
        List<FileField.ParsedFileField> newFileList = new ArrayList<>();
        boolean changed = false;
        for (FileField.ParsedFileField flEntry : fileList) {
            String realOldFilename = flEntry.link;

            if (onlyRelativePaths && (new File(realOldFilename).isAbsolute())) {
                continue;
            }

            StringBuilder newFilename = new StringBuilder(Util.getLinkedFileName(database, entry));
            //String oldFilename = bes.getField(GUIGlobals.FILE_FIELD); // would have to be stored for undoing purposes

            //Add extension to newFilename
            newFilename.append(".");
            newFilename.append(FileUtil.getFileExtension(realOldFilename).orElse("pdf"));

            //get new Filename with path
            //Create new Path based on old Path and new filename
            File expandedOldFile = FileUtil.expandFilename(realOldFilename, paths);
            if ((expandedOldFile == null) || (expandedOldFile.getParent() == null)) {
                // something went wrong. Just skip this entry
                continue;
            }
            String newPath = expandedOldFile.getParent().concat(System.getProperty("file.separator")).concat(
                    newFilename.toString());

            if (new File(newPath).exists()) {
                // we do not overwrite files
                // TODO: we could check here if the newPath file is linked with the current entry. And if not, we could add a link
                continue;
            }

            //do rename
            boolean renameSuccessful = FileUtil.renameFile(expandedOldFile.toString(), newPath);

            if (renameSuccessful) {
                changed = true;

                //Change the path for this entry
                String description = flEntry.description;
                String type = flEntry.fileType;

                // we cannot use "newPath" to generate a FileListEntry as newPath is absolute, but we want to keep relative paths whenever possible
                File parent = (new File(realOldFilename)).getParentFile();
                String newFileEntryFileName;
                if (parent == null || paths.contains(parent.getAbsolutePath())) {
                    newFileEntryFileName = newFilename.toString();
                } else {
                    newFileEntryFileName = parent.toString().concat(System.getProperty("file.separator")).concat(
                            newFilename.toString());
                }
                newFileList.add(new FileField.ParsedFileField(description, newFileEntryFileName, type));
            } else {
                unsuccessfulRenames++;
            }
        }

        if (changed) {
            String newValue = FileField.getStringRepresentation(newFileList);
            assert (!oldValue.get().equals(newValue));
            entry.setField(Globals.FILE_FIELD, newValue);
            //we put an undo of the field content here
            //the file is not being renamed back, which leads to inconsistencies
            //if we put a null undo object here, the change by "doMakePathsRelative" would overwrite the field value nevertheless.
            FieldChange change = new FieldChange(entry, Globals.FILE_FIELD, oldValue.get(), newValue);
            return Collections.singletonList(change);
        }

        return new ArrayList<>();
    }

    public int getUnsuccessfulRenames() {
        return unsuccessfulRenames;
    }
}
