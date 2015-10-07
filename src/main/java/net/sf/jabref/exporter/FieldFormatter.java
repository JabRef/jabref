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
package net.sf.jabref.exporter;

public interface FieldFormatter {

    /**
     * Formats the content of a field.
     * Currently only one implementation: net.sf.jabref.exporter.LatexFieldFormatter
     * 
     * Reason for this interface: unknown
     * 
     * @param s the content of the field
     * @param fieldName the name of the field - used to trigger different serializations, e.g., turning off resolution for some strings
     * @return a formatted string suitable for output
     * @throws IllegalArgumentException if s is not a correct bibtex string, e.g., because of improperly balanced braces or using # not paired
     */
    String format(String s, String fieldName) throws IllegalArgumentException;
}
