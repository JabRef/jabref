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
package net.sf.jabref.gui;

import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.logic.util.io.SimpleFileListEntry;

/**
 * This class represents a file link for a Bibtex entry.
 */
public class FileListEntry extends SimpleFileListEntry {

    private ExternalFileType type;


    public FileListEntry(String description, String link, ExternalFileType type) {
        super(description, link);
        this.type = type;
    }

    public FileListEntry(SimpleFileListEntry simpleEntry, ExternalFileType type) {
        super(simpleEntry.getDescription(), simpleEntry.getLink());
        this.type = type;
    }

    public ExternalFileType getType() {
        return type;
    }

    public void setType(ExternalFileType type) {
        this.type = type;
    }

    public String[] getStringArrayRepresentation() {
        String typeString = getType() != null ? getType().getName() : "";
        return new String[] {getDescription(), getLink(), typeString};
    }

    @Override
    public String toString() {
        return description + " : " + link + " : " + type;
    }

}
