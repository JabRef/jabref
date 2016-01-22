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
package net.sf.jabref.importer;

import java.io.File;
import java.util.*;

import net.sf.jabref.Globals;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.JabRef;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;

/**
 * Search class for files. <br>
 * <br>
 * This class provides some functionality to search in a {@link BibDatabase} for
 * files. <br>

 * @author Nosh&Dan
 */
class DatabaseFileLookup {

    private static final String KEY_FILE_FIELD = "file";

    private final Set<File> fileCache = new HashSet<>();

    private final String[] possibleFilePaths;

    /**
     * Creates an instance by passing a {@link BibDatabase} which will be used for the searches.
     *
     * @param database A {@link BibDatabase}.
     */
    public DatabaseFileLookup(BibDatabase database) {
        Objects.requireNonNull(database);
        possibleFilePaths = Optional.ofNullable(JabRef.jrf.getCurrentBasePanel().metaData().getFileDirectory(Globals.FILE_FIELD)).orElse(new String[]{});

        for (BibEntry entry : database.getEntries()) {
            fileCache.addAll(parseFileField(entry));
        }
    }

    /**
     * Returns whether the File <code>file</code> is present in the database
     * as an attached File to an {@link BibEntry}. <br>
     * <br>
     * To do this, the field specified by the key <b>file</b> will be searched
     * for the provided file for every {@link BibEntry} in the database. <br>
     * <br>
     * For the matching, the absolute file paths will be used.
     *
     * @param file
     *            A {@link File} Object.
     * @return <code>true</code>, if the file Object is stored in at least one
     *         entry in the database, otherwise <code>false</code>.
     */
    public boolean lookupDatabase(File file) {
        if (fileCache.contains(file)) {
            return true;
        } else {
            return false;
        }
    }

    private List<File> parseFileField(BibEntry entry) {
        Objects.requireNonNull(entry);

        FileListTableModel model = new FileListTableModel();

        String fileField = entry.getField(DatabaseFileLookup.KEY_FILE_FIELD);
        model.setContent(fileField);

        List<File> fileLinks = new ArrayList<>();
        for (FileListEntry e : model.parseFileField(fileField)) {
            String link = e.getLink();

            if (link == null) {
                break;
            }

            File expandedFilename = FileUtil.expandFilename(link, possibleFilePaths);
            fileLinks.add(expandedFilename);
        }

        return fileLinks;
    }
}
