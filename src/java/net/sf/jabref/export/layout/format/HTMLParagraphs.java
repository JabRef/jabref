package net.sf.jabref.export.layout.format;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.export.layout.LayoutFormatter;

/**
 * Will interpret two consecutive newlines as the start of a new paragraph and thus
 * wrap the paragraph in HTML-p-tags.
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 * 
 */
public class HTMLParagraphs implements LayoutFormatter {

	static Pattern beforeNewLines;

	public String format(String fieldText) {

		fieldText = fieldText.trim();
		
		if (fieldText.length() == 0){
			return fieldText;
		}
		
		if (beforeNewLines == null) {
			beforeNewLines = Pattern.compile("(.*?)\\n\\s*\\n");
		}

		Matcher m = beforeNewLines.matcher(fieldText);
		StringBuffer s = new StringBuffer();
		while (m.find()) {
			String middle = m.group(1).trim();
			if (middle.length() > 0){
				s.append("<p>\n");
				m.appendReplacement(s, m.group(1));
				s.append("\n</p>\n");
			}
		}
		s.append("<p>\n");
		m.appendTail(s);
		s.append("\n</p>");
	
		return s.toString();
	}
}
