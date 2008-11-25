package net.sf.jabref.export.layout.format;

import net.sf.jabref.*;
import net.sf.jabref.export.layout.*;

public class RisKeywords implements LayoutFormatter {

	public String format(String s) {
		if (s == null)
			return "";
		StringBuilder sb = new StringBuilder();
		String[] keywords = s.split(",[ ]*");
		for (int i=0; i<keywords.length; i++) {
			sb.append("KW  - ");
			sb.append(keywords[i]);
			if (i < keywords.length-1)
				sb.append(Globals.NEWLINE);
		}
		return sb.toString();
	}
}