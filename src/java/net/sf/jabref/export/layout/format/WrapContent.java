package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.AbstractParamLayoutFormatter;

/**
 * This formatter outputs the input value after adding a prefix and a postfix,
 * as long as the input value is non-empty. If the input value is empty, an
 * empty string is output (the prefix and postfix are not output in this case).
 *
 * The formatter requires an argument containing the prefix and postix separated
 * by a comma. To include a the comma character in either, use an escape sequence
 * (\,).
 */
public class WrapContent extends AbstractParamLayoutFormatter {

    
    private String before = null, after = null;


    public void setArgument(String arg) {
	String[] parts = parseArgument(arg);
        if (parts.length < 2)
	   return;
        before = parts[0];
        after = parts[1];
    }

    public String format(String fieldText) {
	if (before == null)
	    return "";
    	if (fieldText.length() == 0)
	    return "";
	else
	    return new StringBuilder(before).append(fieldText).append(after).toString();    
    }
}
