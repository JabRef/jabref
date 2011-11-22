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

import java.io.File;

import net.sf.jabref.Globals;
import net.sf.jabref.Util;
import net.sf.jabref.export.layout.LayoutFormatter;

/**
 * Will expand the relative PDF path and return a URI for the given file (which
 * must exist).
 * 
 * Users should use FileLink (even if that uses f.getCanonicalPath() instead of toURI().toString()
 * 
 * @deprecated
 * @author $Author$
 * @version $Revision$ ($Date$)
 */
public class ResolvePDF implements LayoutFormatter {

	public String format(String field) {

        // Search in the standard PDF directory:
        /* Oops, this part is not sufficient. We need access to the
          database's metadata in order to check if the database overrides
          the standard file directory */
        String dir = Globals.prefs.get("pdfDirectory");
		File f = Util.expandFilename(field, new String[] { dir, "." });
		
		/*
		 * Stumbled over this while investigating
		 * 
		 * https://sourceforge.net/tracker/index.php?func=detail&aid=1469903&group_id=92314&atid=600306
		 */
		if (f != null) {
			return f.toURI().toString();
		} else {
			return field;
		}
	}
}
