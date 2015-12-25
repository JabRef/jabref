/*  Copyright (C) 2015 JabRef contributors.
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
package net.sf.jabref.logic.util.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.logic.util.io.SimpleFileListEntry;
import net.sf.jabref.logic.util.strings.StringUtil;

/**
 * Data structure to contain a list of file links, parseable from a coded string.
 * To be used instead of FileListTableModel when no GUI parts are required.
 */
public class SimpleFileList {

    private final List<SimpleFileListEntry> list = new ArrayList<>();

    public SimpleFileListEntry getEntry(int index) {
            return list.get(index);
    }

    public int size() {
        return list.size();
    }

    public Boolean isEmpty() {
        return list.isEmpty();
    }

    public void removeEntry(int index) {
            list.remove(index);
    }

    public void addEntry(final SimpleFileListEntry entry) {
            list.add(entry);
    }

    /**
     * Set up the list contents based on the flat string representation of the file list
     * @param value The string representation
     */
    public void setContent(String value) {
        if (value == null) {
            value = "";
        }
        List<SimpleFileListEntry> fileEntryList = FileUtil.decodeFileField(value);
        List<SimpleFileListEntry> newList = new ArrayList<>();
        for (SimpleFileListEntry thisEntry : fileEntryList) {
            newList.add(decodeEntry(thisEntry));
        }

        list.clear();
        list.addAll(newList);
    }


    private SimpleFileListEntry decodeEntry(SimpleFileListEntry entry) {
        Boolean validName = Globals.prefs.isExternalFileTypeName(entry.getTypeName());
        String typeName = entry.getTypeName();

        if (!validName) {
            // No file type was recognized. Try to find a usable file type based
            // on mime type:
            typeName = Globals.prefs.getExternalFileTypeNameByMimeType(entry.getTypeName());
            if (typeName == null) {
                // No type could be found from mime type on the extension:
                //System.out.println("Not found by mime: '"+getElementIfAvailable(contents, 2));
                String typeGuess = null;
                Optional<String> extension = FileUtil.getFileExtension(entry.getLink());
                if (extension.isPresent()) {
                    typeGuess = Globals.prefs.getExternalFileTypeNameByExt(extension.get());
                }
                if (typeGuess != null) {
                    typeName = typeGuess;
                }
            }
        }

        return new SimpleFileListEntry(entry.getDescription(), entry.getLink(), typeName);
    }

    /**
     * Transform the file list into a flat string representable
     * as a BibTeX field:
     * @return String representation.
     */
    public String getStringRepresentation() {
        String[][] array = new String[list.size()][];
        int i = 0;
        for (SimpleFileListEntry entry : list) {
            array[i] = entry.getStringArrayRepresentation();
            i++;
        }
        return StringUtil.encodeStringArray(array);
    }

}
