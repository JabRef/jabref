package net.sf.jabref.autocompleter;

import net.sf.jabref.AuthorList;
import net.sf.jabref.BibtexEntry;

/**
 * Interpretes the given values as names and stores them in different
 * permutations so we can complete by beginning with last name or first name.
 * 
 * @author kahlert, cordes
 * 
 */
public class NameFieldAutoCompleter extends AbstractAutoCompleter {

	private String _fieldName;

	/**
	 * @see AutoCompleterFactory
	 */
	protected NameFieldAutoCompleter(String fieldName) {
		_fieldName = fieldName;
	}

	public boolean isSingleUnitField() {
		return false;
	}

	public void addBibtexEntry(String fieldValue, BibtexEntry entry) {
		addBibtexEntry(entry);
	}

	public void addBibtexEntry(BibtexEntry entry) {
		if (entry != null) {
			String fieldValue = entry.getField(_fieldName);
			if (fieldValue != null) {
				AuthorList authorList = AuthorList.getAuthorList(fieldValue);
				for (int i = 0; i < authorList.size(); i++) {
					AuthorList.Author author = authorList.getAuthor(i);
					addWordToIndex(author.getLastFirst(true));
					addWordToIndex(author.getLastFirst(false));
					addWordToIndex(author.getFirstLast(true));
					addWordToIndex(author.getFirstLast(false));
				}
			}
		}
	}

	public String[] complete(String str) {
		return super.complete(str);
	}

	public String getFieldName() {
		return _fieldName;
	}
}
