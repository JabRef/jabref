/*
 * Created on 10/10/2004
 * 
 */
package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;

/**
 * Replaces and's for & (in case of two authors) and ; (in case
 * of more than two authors).
 * 
 * @author Carlos Silla
 */
public class AuthorAndsReplacer implements LayoutFormatter {

	/* (non-Javadoc)
	 * @see net.sf.jabref.export.layout.LayoutFormatter#format(java.lang.String)
	 */
	public String format(String fieldText) {

        if (fieldText == null)
            return null;
        String[] authors = fieldText.split(" and ");
		String s;
	
		switch(authors.length) {
			case 1:
				//Does nothing;
				s = authors[0];
			break;
			case 2:
				s = authors[0] + " & " + authors[1];
			break;
			default:
				int i = 0, x = authors.length;
				StringBuffer sb = new StringBuffer();
				
				for(i=0;i<x-2;i++) {
                    sb.append(authors[i]).append("; ");
				}
                sb.append(authors[i]).append(" & ").append(authors[i + 1]);
				s = new String(sb);				
			break;		
		}
		
		return s;
 
	}
}
