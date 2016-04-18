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
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.Author;

/**
 * Will return the Authors to match the OrgSci format:
 * 
 * <ul>
 * <li>That is the first author is LastFirst, but all others are FirstLast.</li>
 * <li>First names are abbreviated</li>
 * <li>Spaces between abbreviated first names are NOT removed. Use
 * NoSpaceBetweenAbbreviations to achieve this.</li>
 * </ul>
 * <p>
 * See the testcase for examples.
 * </p>
 * <p>
 * Idea from: http://stuermer.ch/blog/bibliography-reference-management-with-jabref.html
 * </p>
 * 
 */
public class AuthorOrgSci implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        AuthorList a = AuthorList.parse(fieldText);
        if (a.isEmpty()) {
            return fieldText;
        }
        Author first = a.getAuthor(0);
        StringBuilder sb = new StringBuilder();
        sb.append(first.getLastFirst(true));
        for (int i = 1; i < a.getNumberOfAuthors(); i++) {
            sb.append(", ").append(a.getAuthor(i).getFirstLast(true));
        }
        return sb.toString();
    }
}
