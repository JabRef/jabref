package net.sf.jabref.autocompleter;

import net.sf.jabref.AuthorList;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;

/**
 * Interpretes the given values as names and stores them in different
 * permutations so we can complete by beginning with last name or first name.
 * 
 * @author kahlert, cordes
 * 
 */
public class NameFieldAutoCompleter extends AbstractAutoCompleter {

	private String[] fieldNames;
    private boolean lastNameOnly;
    private String prefix = "";
    private boolean autoCompFF, autoCompLF;
    private boolean caseSensitive = true;

	/**
	 * @see AutoCompleterFactory
	 */
    protected NameFieldAutoCompleter(String fieldName) {
        this(new String[] {fieldName}, false);

    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

	public NameFieldAutoCompleter(String[] fieldNames, boolean lastNameOnly) {
		this.fieldNames = fieldNames;
        this.lastNameOnly = lastNameOnly;
        if (Globals.prefs.getBoolean("autoCompFF")) {
            autoCompFF = true;
            autoCompLF = false;
        }
        else if (Globals.prefs.getBoolean("autoCompLF")) {
            autoCompFF = false;
            autoCompLF = true;
        }
        else {
            autoCompFF = true;
            autoCompLF = true;
        }

	}

	public boolean isSingleUnitField() {
		return true;
	}

	public void addBibtexEntry(String fieldValue, BibtexEntry entry) {
		addBibtexEntry(entry);
	}

	public void addBibtexEntry(BibtexEntry entry) {
        if (entry != null) {
            for (int i=0; i<fieldNames.length; i++) {
                String fieldValue = entry.getField(fieldNames[i]);
                if (fieldValue != null) {
                    AuthorList authorList = AuthorList.getAuthorList(fieldValue);
                    for (int j = 0; j < authorList.size(); j++) {
                        AuthorList.Author author = authorList.getAuthor(j);
                        if (lastNameOnly) {
                            addWordToIndex(author.getLastOnly());
                        } else {
                            if (autoCompLF) {
                                addWordToIndex(author.getLastFirst(true));
                                addWordToIndex(author.getLastFirst(false));
                            }
                            if (autoCompFF) {
                                addWordToIndex(author.getFirstLast(true));
                                addWordToIndex(author.getFirstLast(false));
                            }
                        }
                    }
                }
            }
		}
	}

	public String[] complete(String str) {
        str = str.toLowerCase();
        int index = str.lastIndexOf(" and ");
        if (index >= 0) {
            prefix = str.substring(0, index+5);
            str = str.substring(index+5);
        }
        else prefix = "";

        String[] res = super.complete(str);
        return res;
	}

	public String getFieldName() {
		return fieldNames[0];
	}

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void addWordToIndex(String word) {
        super.addWordToIndex(word.toLowerCase());
    }
}
