package net.sf.jabref.gui.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.cleanup.Cleaner;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.util.Util;

public class RenamePdfCleanup implements Cleaner {

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
        if (!entry.hasField(Globals.FILE_FIELD)) {
            return new ArrayList<>();
        }
        String oldValue = entry.getField(Globals.FILE_FIELD);
        FileListTableModel flModel = new FileListTableModel();
        flModel.setContent(oldValue);
        if (flModel.getRowCount() == 0) {
            return new ArrayList<>();
        }
        boolean changed = false;

        for (int i = 0; i < flModel.getRowCount(); i++) {
            String realOldFilename = flModel.getEntry(i).link;

            if (onlyRelativePaths && (new File(realOldFilename).isAbsolute())) {
                continue;
            }

            String newFilename = Util.getLinkedFileName(database, entry);
            //String oldFilename = bes.getField(GUIGlobals.FILE_FIELD); // would have to be stored for undoing purposes

            //Add extension to newFilename
            newFilename = newFilename + "." + flModel.getEntry(i).type.getExtension();

            //get new Filename with path
            //Create new Path based on old Path and new filename
            File expandedOldFile = FileUtil.expandFilename(realOldFilename, paths);
            if ((expandedOldFile == null) || (expandedOldFile.getParent() == null)) {
                // something went wrong. Just skip this entry
                continue;
            }
            String newPath = expandedOldFile.getParent().concat(System.getProperty("file.separator"))
                    .concat(newFilename);

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
                String description = flModel.getEntry(i).description;
                ExternalFileType type = flModel.getEntry(i).type;
                flModel.removeEntry(i);

                // we cannot use "newPath" to generate a FileListEntry as newPath is absolute, but we want to keep relative paths whenever possible
                File parent = (new File(realOldFilename)).getParentFile();
                String newFileEntryFileName;
                if (parent == null) {
                    newFileEntryFileName = newFilename;
                } else {
                    newFileEntryFileName = parent.toString().concat(System.getProperty("file.separator"))
                            .concat(newFilename);
                }
                flModel.addEntry(i, new FileListEntry(description, newFileEntryFileName, type));
            } else {
                unsuccessfulRenames++;
            }
        }

        if (changed) {
            String newValue = flModel.getStringRepresentation();
            assert(!oldValue.equals(newValue));
            entry.setField(Globals.FILE_FIELD, newValue);
            //we put an undo of the field content here
            //the file is not being renamed back, which leads to inconsistencies
            //if we put a null undo object here, the change by "doMakePathsRelative" would overwrite the field value nevertheless.
            FieldChange change = new FieldChange(entry, Globals.FILE_FIELD, oldValue, newValue);
            return Collections.singletonList(change);
        }

        return new ArrayList<>();
    }

    public int getUnsuccessfulRenames() {
        return unsuccessfulRenames;
    }
}
