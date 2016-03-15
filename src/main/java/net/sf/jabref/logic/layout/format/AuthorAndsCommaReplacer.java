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
 * Replaces and's for & (in case of two authors) and , (in case
 * of more than two authors).
 *
 * @author Carlos Silla
 */
public class AuthorAndsCommaReplacer implements LayoutFormatter {

    /* (non-Javadoc)
     * @see net.sf.jabref.export.layout.LayoutFormatter#format(java.lang.String)
     */
    @Override
    public String format(String fieldText) {

        String[] authors = fieldText.split(" and ");
        String s;

        switch (authors.length) {
        case 1:
            //Does nothing;
            s = authors[0];
            break;
        case 2:
            s = authors[0] + " & " + authors[1];
            break;
        default:
            int i;
            int x = authors.length;
            StringBuilder sb = new StringBuilder();

            for (i = 0; i < (x - 2); i++) {
                sb.append(authors[i]).append(", ");
            }
            sb.append(authors[i]).append(" & ").append(authors[i + 1]);
            s = sb.toString();
            break;
        }

        return s;

    }
}
