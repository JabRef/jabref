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
package net.sf.jabref.external;

import net.sf.jabref.gui.IconTheme;

/**
 * This subclass of ExternalFileType is used to mark types that are unknown.
 * This can be the case when a database is loaded which contains links to files
 * of a type that has not been defined on this JabRef instance.
 */
public class UnknownExternalFileType extends ExternalFileType {

    public UnknownExternalFileType(String name) {
        super(name, "", "", "", "unknown", IconTheme.JabRefIcon.FILE.getSmallIcon());
    }

}
