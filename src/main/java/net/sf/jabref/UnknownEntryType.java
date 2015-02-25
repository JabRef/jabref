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
package net.sf.jabref;

/**
 * This class is used to represent an unknown entry type, e.g. encountered
 * during bibtex parsing. The only known information is the type name.
 * This is useful if the bibtex file contains type definitions that are used
 * in the file - because the entries will be parsed before the type definitions
 * are found. In the meantime, the entries will be assigned an 
 * UnknownEntryType giving the name.
 */
public class UnknownEntryType extends BibtexEntryType {

    private String name;
    private String[] fields = new String[0];

    public UnknownEntryType(String name_) {
	name = name_;
    }

    public String getName() {
	return name;
    }

    public String[] getOptionalFields() {
	return fields;
    }
    public String[] getRequiredFields() {
	return fields;
    }


    public String describeRequiredFields() {
	return "unknown";
    }

    public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
	return true;
    }

}
