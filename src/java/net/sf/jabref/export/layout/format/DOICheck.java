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
package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.Globals;
import net.sf.jabref.Util;

/**
 * Used to fix [ 1588028 ] export HTML table doi url.
 * 
 * Will prepend "http://dx.doi.org/" if only doi number and not a URL is given.
 *
 * @author mark-schenk
 * @author olly98
 */
public class DOICheck implements LayoutFormatter {

	public String format(String fieldText) {
		
		if (fieldText == null){
			return null;
		}
		
		if (fieldText.trim().isEmpty()) {
		    return "";
		}
		
		if (Util.checkForDOIwithHTTPprefix(fieldText)) {
		    return fieldText;
		} else {
		    return Globals.DOI_LOOKUP_PREFIX.concat(Util.getDOI(fieldText));
		}
	}
}
