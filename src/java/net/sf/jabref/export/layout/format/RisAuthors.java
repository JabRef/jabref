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

import net.sf.jabref.*;
import net.sf.jabref.export.layout.*;

public class RisAuthors implements ParamLayoutFormatter {
	private String arg = "";

	public String format(String s) {
		if (s == null)
			return "";
		StringBuilder sb = new StringBuilder();
		String[] authors = AuthorList.fixAuthor_lastNameFirst(s).split(" and ");
		for (int i=0; i<authors.length; i++) {
			sb.append(arg);
			sb.append("  - ");
			sb.append(authors[i]);
			if (i < authors.length-1)
				sb.append(Globals.NEWLINE);
		}
		return sb.toString();
	}

	public void setArgument(String arg) {
	    this.arg = arg;
	}
}
