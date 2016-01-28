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

import java.util.Objects;

/**
 * This class represents a file link for a Bibtex entry.
 */
public class FileListEntry {

    public String description;
    public String link;
    public ExternalFileType type;

    public FileListEntry(String description, String link) {
        this(description, link, null);
    }

    public FileListEntry(String description, String link, ExternalFileType type) {
        this.description = Objects.requireNonNull(description);
        this.link = Objects.requireNonNull(link);
        this.type = type; // may be null
    }

    public String[] getStringArrayRepresentation() {
        return new String[] {description, link, getTypeName()};
    }

    private String getTypeName() {
        return this.type == null ? "" : this.type.getName();
    }

    @Override
    public String toString() {
        return description + " : " + link + " : " + type;
    }
}
