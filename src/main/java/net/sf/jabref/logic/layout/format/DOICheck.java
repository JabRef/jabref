/*  Copyright (C) 2003-2012 JabRef contributors.
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
import net.sf.jabref.logic.util.DOI;

/**
 * Used to fix [ 1588028 ] export HTML table DOI URL.
 *
 * Will prepend "http://doi.org/" if only DOI and not an URL is given.
 */
public class DOICheck implements LayoutFormatter {
    @Override
    public String format(String fieldText) {
        if (fieldText == null) {
            return null;
        }
        String result = fieldText;
        if (result.startsWith("/")) {
            result = result.substring(1);
        }
        return DOI.build(result).map(DOI::getURLAsASCIIString).orElse(result);
    }
}
