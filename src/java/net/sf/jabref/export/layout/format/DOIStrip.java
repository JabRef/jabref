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
package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.Globals;

/**
 * Will strip any prefixes from the DOI field, in order to output only the DOI number
 * 
 * @author mark-schenk
 *
 */
public class DOIStrip implements LayoutFormatter {

	public String format(String fieldText) {
		
		if (fieldText == null){
			return null;
		}
		
		fieldText = fieldText.trim();
		if (fieldText.length() == 0){
			return fieldText;
		}

		// If starts with '10.' it's fine
		if (fieldText.startsWith("10.")) {
			return fieldText;
		}
		
		// Remove possible 'doi:'
		if (fieldText.matches("^doi:/*.*")){
			fieldText = fieldText.replaceFirst("^doi:/*", "");
			return fieldText;
		}

		// Remove possible 'http://dx.doi.org/' prefix
		if (fieldText.startsWith(Globals.DOI_LOOKUP_PREFIX)){
			fieldText = fieldText.replaceFirst(Globals.DOI_LOOKUP_PREFIX, "");
			return fieldText;
		}
		
		return fieldText;
	}
}
