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

import java.util.List;

/**
 * This class represents a simplified version of the file link for a Bibtex entry.
 */
public class SimpleFileListEntry {

    protected String description;
    protected String link;


    public SimpleFileListEntry(String description, String link) {
        this.description = description;
        this.link = link;
    }

    public SimpleFileListEntry(List<String> entry) {
        if (entry.isEmpty()) {
            this.description = "";
            this.link = "";
            return;
        }

        // A single string, probably the link
        if (entry.size() == 1) {
            this.description = "";
            this.link = entry.get(0);
            return;
        }

        this.description = entry.get(0);
        this.link = entry.get(1);
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

    @Override
    public String toString() {
        return description + " : " + link;
    }

}
