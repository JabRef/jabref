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
package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

/**
 * <p>
 * LayoutFormatter that removes the space between abbreviated First names
 * </p>
 * <p>
 * What out that this regular expression might also remove other spaces that fit
 * the pattern.
 * </p>
 * <p>
 * Example: J. R. R. Tolkien becomes J.R.R. Tolkien.
 * </p>
 * <p>
 * See Testcase for more examples.
 * <p>
 */
public class NoSpaceBetweenAbbreviations implements LayoutFormatter {

    /*
     * Match '.' followed by spaces followed by uppercase char followed by '.'
     * but don't include the last dot into the capturing group.
     * 
     * Replace the match by removing the spaces.
     * 
     * @see net.sf.jabref.export.layout.LayoutFormatter#format(java.lang.String)
     */
    @Override
    public String format(String fieldText) {
        return fieldText.replaceAll("\\.\\s+(\\p{Lu})(?=\\.)", "\\.$1");
    }
}
