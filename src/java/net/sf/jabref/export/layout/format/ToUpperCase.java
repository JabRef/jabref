package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;

/**
 * Convert the contents to upper case.
 */
public class ToUpperCase implements LayoutFormatter {

    public String format(String fieldText) {
		return fieldText.toUpperCase();
	}

}
