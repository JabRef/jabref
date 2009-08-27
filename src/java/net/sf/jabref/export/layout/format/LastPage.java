package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;

/**
 * Formatter that returns the last page from the "pages" field, if set.
 *
 * For instance, if the pages field is set to "345-360" or "345--360",
 * this formatter will return "360".
 */
public class LastPage implements LayoutFormatter {

    public String format(String s) {
        if (s == null)
			return "";
		String[] pageParts = s.split("[\\-]+");
		if (pageParts.length == 2)
            return pageParts[1];
        // If we didn't get two parts, it may be that only the number of pages is given.
        // Return the first part:
        else return pageParts[0];
        //else return "";

    }
}