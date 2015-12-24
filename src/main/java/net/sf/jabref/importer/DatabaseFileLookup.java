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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.Globals;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.JabRef;

/**
 * Search class for files. <br>
 * <br>
 * This class provides some functionality to search in a {@link BibDatabase} for
 * files. <br>
 * <br>
 *
 *
 * @author Nosh&Dan
 * @version 09.11.2008 | 21:21:41
 *
 */
class DatabaseFileLookup {

    private static final String KEY_FILE_FIELD = "file";

    private final Map<File, Boolean> fileToFound = new HashMap<>();

    private final Collection<BibEntry> entries;

    private final String[] possibleFilePaths;


    /**
     * Creates an instance by passing a {@link BibDatabase} which will be
     * used for the searches.
     *
     * @param aDatabase
     *            A {@link BibDatabase}.
     */
    public DatabaseFileLookup(BibDatabase aDatabase) {
        if (aDatabase == null) {
            throw new IllegalArgumentException("Passing a 'null' BibDatabase.");
        }
        entries = aDatabase.getEntries();
        possibleFilePaths = JabRef.jrf.getCurrentBasePanel().metaData().getFileDirectory(Globals.FILE_FIELD);
    }

    /**
     * Returns whether the File <code>aFile</code> is present in the database
     * as an attached File to an {@link BibEntry}. <br>
     * <br>
     * To do this, the field specified by the key <b>file</b> will be searched
     * for the provided file for every {@link BibEntry} in the database. <br>
     * <br>
     * For the matching, the absolute file paths will be used.
     *
     * @param aFile
     *            A {@link File} Object.
     * @return <code>true</code>, if the file Object is stored in at least one
     *         entry in the database, otherwise <code>false</code>.
     */
    public boolean lookupDatabase(File aFile) {
        if (fileToFound.containsKey(aFile)) {
            return fileToFound.get(aFile);
        } else {
            Boolean res = false;
            for (BibEntry entry : entries) {
                if (lookupEntry(aFile, entry)) {
                    res = true;
                    break;
                }
            }
            fileToFound.put(aFile, res);
            //System.out.println(aFile);
            return res;
        }
    }

    /**
     * Searches the specified {@link BibEntry} <code>anEntry</code> for the
     * appearance of the specified {@link File} <code>aFile</code>. <br>
     * <br>
     * Therefore the <i>file</i>-field of the bibtex-entry will be searched for
     * the absolute filepath of the searched file. <br>
     * <br>
     *
     * @param aFile
     *            A file that is searched in an bibtex-entry.
     * @param anEntry
     *            A bibtex-entry, in which the file is searched.
     * @return <code>true</code>, if the bibtex entry stores the file in its
     *         <i>file</i>-field, otherwise <code>false</code>.
     */
    private boolean lookupEntry(File aFile, BibEntry anEntry) {

        if ((aFile == null) || (anEntry == null)) {
            return false;
        }

        String fileField = anEntry.getField(Globals.FILE_FIELD);
        List<List<String>> fileList = FileUtil.decodeFileField(fileField);

        for (List<String> fileInfo : fileList) {
            String link = fileInfo.get(1);

            if ((link == null)) {
                break;
            }

            File expandedFilename = FileUtil.expandFilename(link, possibleFilePaths);
            if (Objects.equals(expandedFilename, aFile)) {
                return true;
            }
        }

        return false;
    }
}
