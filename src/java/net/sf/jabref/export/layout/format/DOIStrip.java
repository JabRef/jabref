package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.Globals;

/**
 * Will strip any prefixes from the DOI field, in order to output only the DOI number
 * 
 * @author mark-schenk
 *
 */
public class DOIStrip implements LayoutFormatter {

	public String format(String fieldText) {
		
		if (fieldText == null){
			return null;
		}
		
		fieldText = fieldText.trim();
		if (fieldText.length() == 0){
			return fieldText;
		}

		// If starts with '10.' it's fine
		if (fieldText.startsWith("10.")) {
			return fieldText;
		}
		
		// Remove possible 'doi:'
		if (fieldText.matches("^doi:/*.*")){
			fieldText = fieldText.replaceFirst("^doi:/*", "");
			return fieldText;
		}

		// Remove possible 'http://dx.doi.org/' prefix
		if (fieldText.startsWith(Globals.DOI_LOOKUP_PREFIX)){
			fieldText = fieldText.replaceFirst(Globals.DOI_LOOKUP_PREFIX, "");
			return fieldText;
		}
		
		return fieldText;
	}
}
