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
