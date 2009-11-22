package net.sf.jabref.autocompleter;

import net.sf.jabref.BibtexEntry;

/**
 * Stores the full original value of one field of the given BibtexEntries.
 * 
 * @author kahlert, cordes
 * 
 */
public class EntireFieldAutoCompleter extends AbstractAutoCompleter {

	public String _fieldName;

	/**
	 * @see AutoCompleterFactory
	 */
	protected EntireFieldAutoCompleter(String fieldName) {
		_fieldName = fieldName;
	}

	public boolean isSingleUnitField() {
		return true;
	}

	public String[] complete(String s) {
		return super.complete(s);
	}

	@Override
	public void addBibtexEntry(BibtexEntry entry) {
		if (entry != null) {
			String fieldValue = entry.getField(_fieldName);
			if (fieldValue != null) {
				addWordToIndex(fieldValue.toString().trim());
			}
		}
	}
}
