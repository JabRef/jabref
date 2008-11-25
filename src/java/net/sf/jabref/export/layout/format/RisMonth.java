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
