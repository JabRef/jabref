package net.sf.jabref.autocompleter;

import java.util.StringTokenizer;

import net.sf.jabref.BibtexEntry;

/**
 * Stores all words which are separated by ' ','.',',' and '\n'. This
 * autocompleter only processes the field which is given by the fieldname.
 * 
 * @author kahlert, cordes
 * 
 */
public class DefaultAutoCompleter extends AbstractAutoCompleter {

	public String _fieldName;

	/**
	 * @see AutoCompleterFactory
	 */
	protected DefaultAutoCompleter(String fieldName) {
		_fieldName = fieldName;
	}

	public boolean isSingleUnitField() {
		return false;
	}

	public String[] complete(String s) {
		return super.complete(s);
	}

	@Override
	public void addBibtexEntry(BibtexEntry entry) {
		if (entry != null) {
			String fieldValue = entry.getField(_fieldName);
			if (fieldValue == null) {
				return;
			} else {
				StringTokenizer tok = new StringTokenizer(fieldValue.toString(), " .,\n");
				while (tok.hasMoreTokens()) {
					String word = tok.nextToken();
					addWordToIndex(word);
				}
			}
		}
	}
}
