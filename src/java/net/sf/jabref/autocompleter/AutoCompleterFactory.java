package net.sf.jabref.autocompleter;

/**
 * Returns an autocompleter to a given fieldname.
 * 
 * @author kahlert, cordes
 */
public class AutoCompleterFactory {

	public static AbstractAutoCompleter getFor(String fieldName) {
		AbstractAutoCompleter result;
		if (fieldName.equals("author") || fieldName.equals("editor")) {
			result = new NameFieldAutoCompleter(fieldName);
		} else if (fieldName.equals("crossref")) {
			result = new CrossrefAutoCompleter(fieldName);
		} else if (fieldName.equals("journal") || fieldName.equals("publisher")) {
			result = new EntireFieldAutoCompleter(fieldName);
		} else {
			result = new DefaultAutoCompleter(fieldName);
		}
		return result;
	}

}
