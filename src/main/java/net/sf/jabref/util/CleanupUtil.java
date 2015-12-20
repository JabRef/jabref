package net.sf.jabref.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

// TODO: Move to cleanup classes.
public class CleanupUtil {

    /**
     * Collect file links from the given set of fields, and add them to the list contained in the field
     * GUIGlobals.FILE_FIELD.
     *
     * @param database The database to modify.
     * @param fields The fields to find links in.
     * @return A CompoundEdit specifying the undo operation for the whole operation.
     */
    public static NamedCompound upgradePdfPsToFile(BibDatabase database, String[] fields) {
        NamedCompound ce = new NamedCompound(Localization.lang("Move external links to 'file' field"));

        for (BibEntry entry : database.getEntryMap().values()) {
            List<FieldChange> changes = upgradePdfPsToFile(entry, Arrays.asList(fields));

            for (FieldChange change : changes) {
                ce.addEdit(new UndoableFieldChange(change));
            }
        }

        ce.end();
        return ce;
    }

    /**
     * Collect file links from the given set of fields, and add them to the list
     * contained in the field GUIGlobals.FILE_FIELD.
     *
     * @param entries The entries to modify.
     * @param fields The fields to find links in.
     * @return
     * @return A CompoundEdit specifying the undo operation for the whole operation.
     */
    public static List<FieldChange> upgradePdfPsToFile(BibEntry entry, List<String> fields) {
        List<FieldChange> changes = new ArrayList<>();

        FileListTableModel tableModel = new FileListTableModel();
        // If there are already links in the file field, keep those on top:
        String oldFileContent = entry.getField(Globals.FILE_FIELD);
        if (oldFileContent != null) {
            tableModel.setContent(oldFileContent);
        }
        int oldRowCount = tableModel.getRowCount();
        for (String field : fields) {
            String o = entry.getField(field);
            if (o != null) {
                if (!o.trim().isEmpty()) {
                    File f = new File(o);
                    FileListEntry flEntry = new FileListEntry(f.getName(), o,
                            Globals.prefs.getExternalFileTypeByExt(field));
                    tableModel.addEntry(tableModel.getRowCount(), flEntry);

                    entry.clearField(field);
                    changes.add(new FieldChange(entry, field, o, null));
                }
            }
        }
        if (tableModel.getRowCount() != oldRowCount) {
            String newValue = tableModel.getStringRepresentation();
            entry.setField(Globals.FILE_FIELD, newValue);
            changes.add(new FieldChange(entry, Globals.FILE_FIELD, oldFileContent, newValue));
        }

        return changes;
    }

    public static List<FieldChange> fixFileEntries(BibEntry entry) {
        String oldValue = entry.getField(Globals.FILE_FIELD);
        if (oldValue == null) {
            return new ArrayList<>();
        }
        FileListTableModel flModel = new FileListTableModel();
        flModel.setContent(oldValue);
        if (flModel.getRowCount() == 0) {
            return new ArrayList<>();
        }
        boolean changed = false;
        for (int i = 0; i < flModel.getRowCount(); i++) {
            FileListEntry flEntry = flModel.getEntry(i);
            String link = flEntry.getLink();
            String description = flEntry.getDescription();
            if ("".equals(link) && (!"".equals(description))) {
                // link and description seem to be switched, quickly fix that
                flEntry.setLink(flEntry.getDescription());
                flEntry.setDescription("");
                changed = true;
            }
        }
        if (changed) {
            String newValue = flModel.getStringRepresentation();
            assert(!oldValue.equals(newValue));
            entry.setField(Globals.FILE_FIELD, newValue);
            FieldChange change = new FieldChange(entry, Globals.FILE_FIELD, oldValue, newValue);
            return Collections.singletonList(change);
        }
        return new ArrayList<>();
    }

    public static List<FieldChange> makePathsRelative(BibEntry entry, String[] paths) {
        String oldValue = entry.getField(Globals.FILE_FIELD);
        if (oldValue == null) {
            return new ArrayList<>();
        }
        FileListTableModel flModel = new FileListTableModel();
        flModel.setContent(oldValue);
        if (flModel.getRowCount() == 0) {
            return new ArrayList<>();
        }
        boolean changed = false;
        for (int i = 0; i < flModel.getRowCount(); i++) {
            FileListEntry flEntry = flModel.getEntry(i);
            String oldFileName = flEntry.getLink();
            String newFileName = FileUtil.shortenFileName(new File(oldFileName), paths).toString();
            if (!oldFileName.equals(newFileName)) {
                flEntry.setLink(newFileName);
                changed = true;
            }
        }
        if (changed) {
            String newValue = flModel.getStringRepresentation();
            assert(!oldValue.equals(newValue));
            entry.setField(Globals.FILE_FIELD, newValue);
            FieldChange change = new FieldChange(entry, Globals.FILE_FIELD, oldValue, newValue);
            return Collections.singletonList(change);
        }
        return new ArrayList<>();
    }

    // TODO: Remove dependence on database if possible
    public static List<FieldChange> renameFiles(BibEntry entry, boolean onlyRelativePaths, BibDatabase database,
            List<String> paths) {
        //Extract the path
        String oldValue = entry.getField(Globals.FILE_FIELD);
        if (oldValue == null) {
            return new ArrayList<>();
        }
        FileListTableModel flModel = new FileListTableModel();
        flModel.setContent(oldValue);
        if (flModel.getRowCount() == 0) {
            return new ArrayList<>();
        }
        boolean changed = false;

        for (int i = 0; i < flModel.getRowCount(); i++) {
            String realOldFilename = flModel.getEntry(i).getLink();

            if (onlyRelativePaths && (new File(realOldFilename).isAbsolute())) {
                continue;
            }

            String newFilename = Util.getLinkedFileName(database, entry);
            //String oldFilename = bes.getField(GUIGlobals.FILE_FIELD); // would have to be stored for undoing purposes

            //Add extension to newFilename
            newFilename = newFilename + "." + flModel.getEntry(i).getType().getExtension();

            //get new Filename with path
            //Create new Path based on old Path and new filename
            File expandedOldFile = FileUtil.expandFilename(realOldFilename, paths.toArray(new String[] {}));
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
                String description = flModel.getEntry(i).getDescription();
                ExternalFileType type = flModel.getEntry(i).getType();
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
                // TODO: Reenable this
                //unsuccessfulRenames++;
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
            return Arrays.asList(new FieldChange[] {change});
        }

        return new ArrayList<>();
    }
}
