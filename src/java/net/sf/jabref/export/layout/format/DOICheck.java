package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;

/**
 * Used to fix [ 1588028 ] export HTML table doi url.
 * 
 * Will prepend "http://dx.doi.org/" if only doi number and not a URL is given.
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 *
 */
public class DOICheck implements LayoutFormatter {

	public String format(String fieldText) {
		
		if (fieldText == null){
			return null;
		}
		
		fieldText = fieldText.trim();
		
		if (fieldText.length() == 0){
			return fieldText;
		}
		
		if (fieldText.startsWith("10")){
			return "http://dx.doi.org/" + fieldText;
		}
		
		return fieldText;
	}
}
