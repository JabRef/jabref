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

public class RisMonth implements LayoutFormatter {
	
	public String format(String s) {
		if (s == null)
			return "";
		s = s.toLowerCase();
		if (Globals.MONTH_STRINGS.get(s) != null)
		    s = Globals.MONTH_STRINGS.get(s).toLowerCase();

		if (s.equals("january"))
		    return "01";
		else if (s.equals("february"))
		    return "02";
		else if (s.equals("march"))
		    return "03";
		else if (s.equals("april"))
		    return "04";
		else if (s.equals("may"))
		    return "05";
		else if (s.equals("june"))
		    return "06";
		else if (s.equals("july"))
		    return "07";
		else if (s.equals("august"))
		    return "08";
		else if (s.equals("august"))
		    return "09";
		else if (s.equals("september"))
		    return "10";
		else if (s.equals("december"))
		    return "12";
		else
		    return s;
	}
	
}
