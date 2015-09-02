package net.sf.jabref.autocompleter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

public abstract class StringAbstractAutoCompleter extends AbstractAutoCompleter<String> {

	public static final int SHORTEST_WORD = 4;

	// stores the strings as is
	private TreeSet<String> _index_casesensitive = new TreeSet<String>();
	
	// stores strings in lowercase
	private TreeSet<String> _index_caseinsensitive = new TreeSet<String>();
	
	// stores for a lowercase string the possible expanded strings
	private HashMap<String, TreeSet<String>> _possibleStringsForSearchString = new HashMap<String, TreeSet<String>>();

	public String[] complete(String str) {
		if (stringMinLength(str))
			return null;
		String lstr = str.toLowerCase();
		
		if (lstr.equals(str)) {
			// user typed in lower case word -> we do an case-insenstive search
			String ender = incrementLastCharacter(lstr);
			SortedSet<String> subset = _index_caseinsensitive.subSet(lstr, ender);
		
			// As subset only contains lower case strings, 
			// we have to to determine possible strings for each hit
			ArrayList<String> res = new ArrayList<String>();
			for (String s: subset) {
				res.addAll(_possibleStringsForSearchString.get(s));
			}		
			return res.toArray(new String[res.size()]);
		} else {
			// user typed in a mix of upper case and lower case,
			// we assume user wants to have exact search
			String ender = incrementLastCharacter(str);
			SortedSet<String> subset = _index_casesensitive.subSet(str, ender);
			return subset.toArray(new String[subset.size()]);
		}
	}

	/**
	 * Increments the last character of a string.
	 * 
	 * Example: incrementLastCharacter("abc") returns "abd".
	 */
	private static String incrementLastCharacter(String str) {
		char lastChar = str.charAt(str.length() - 1);
		String ender = str.substring(0, str.length() - 1) + Character.toString((char) (lastChar + 1));
		return ender;
	}

	private static boolean stringMinLength(String str) {
		return str.length() < AbstractAutoCompleter.SHORTEST_TO_COMPLETE;
	}

	public void addWordToIndex(String word) {
		if (word.length() >= SHORTEST_WORD) {
			_index_casesensitive.add(word);
			
			// insensitive treatment
			// first, add the lower cased word to search index
			// second, add a mapping from the lower cased word to the real word
			String word_lcase = word.toLowerCase();
			_index_caseinsensitive.add(word_lcase);
			TreeSet<String> set = _possibleStringsForSearchString.get(word_lcase);
			if (set==null) {
				set = new TreeSet<String>();
			}
			set.add(word);
			_possibleStringsForSearchString.put(word_lcase, set);
		}
	}

	public boolean indexContainsWord(String word) {
		return _index_caseinsensitive.contains(word.toLowerCase());
	}
}