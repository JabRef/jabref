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
package net.sf.jabref.gui.openoffice;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: 16-Dec-2007
 * Time: 10:37:23
 * To change this template use File | Settings | File Templates.
 */
class BibEntryNotFoundException extends Exception {

    private final String bibtexKey;


    public BibEntryNotFoundException(String bibtexKey, String message) {
        super(message);

        this.bibtexKey = bibtexKey;
    }

    public String getBibtexKey() {
        return bibtexKey;
    }
}
