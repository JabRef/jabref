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
 * Formatter that returns the last page from the "pages" field, if set.
 *
 * For instance, if the pages field is set to "345-360" or "345--360",
 * this formatter will return "360".
 */
public class LastPage implements LayoutFormatter {

    @Override
    public String format(String s) {
        if (s == null) {
            return "";
        }
        String[] pageParts = s.split("[\\-]+");
        if (pageParts.length == 2) {
            return pageParts[1];
        } else if (pageParts.length >= 1) {
            return pageParts[0];
        } else {
            return "";
        }

    }
}
