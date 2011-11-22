/*  Copyright (C) 2003-2011 JabRef contributors.
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

/**
 * This class represents a file link for a Bibtex entry.
*/
public class FileListEntry {
    private String link;
    private String description;
    private ExternalFileType type;

    public FileListEntry(String description, String link, ExternalFileType type) {
        this.link = link;
        this.description = description;
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public ExternalFileType getType() {
        return type;
    }

    public void setType(ExternalFileType type) {
        this.type = type;
    }

    public String toString() {
        return description+" : "+link+" : "+type;
    }

}
