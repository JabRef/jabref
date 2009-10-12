package net.sf.jabref.gui;

import net.sf.jabref.AuthorList;
import net.sf.jabref.BibtexEntry;

import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * Created by Morten O. Alver, 16 Feb. 2007
 */
public class AutoCompleter {
    final int SHORTEST_WORD = 4,
            SHORTEST_TO_COMPLETE = 2;

    private TreeSet<String> words = new TreeSet<String>();
    private TreeSet<String> lastNames = null, firstNames = null;
    private boolean nameField = false, // Attempt to store entire names?
    // Crossref autocompleter should store info from the key field:
        crossRefField = false,
        entireField = false; // Set to true if the entire field should be stored
		             // suitable e.g. for journal or publisher fields.
    public String fieldName;

    public AutoCompleter(String fieldName) {
        crossRefField = fieldName.equals("crossref");
        if (fieldName.equals("author") || fieldName.equals("editor")) {
            nameField = true;
            // Name fields separate first names and last names. First names are
            // put in the firstNames set, and last names in the lastNames set.
            // All names are stored in the words set:
            lastNames = new TreeSet<String>();
            firstNames = new TreeSet<String>();
        }
        else if (fieldName.equals("journal") || fieldName.equals("publisher"))
            entireField = true;
    }


    public boolean isNameField() {
        return nameField;
    }

    public boolean isSingleUnitField() {
        return entireField;
    }

    public void addWord(String word) {
        if (word.length() >= SHORTEST_WORD)
            words.add(word);
    }

    /**
     * Add the information from the given object to this autocompleter.
     * @param s The text string to add words from.
     * @param entry The entry containing the text, if any. If the autocompleter
     *  requires information from a different field, this can be looked up
     *  from the given entry. The entry can be null.
     */
    public void addAll(Object s, BibtexEntry entry) {
        if (crossRefField) {
            // Crossrefs reference bibtex keys, so we should add the words from
            // the key field instead of the crossref field:
            if (entry != null) {
                String key = entry.getCiteKey();
                if (key != null)
                    addWord(key.trim());
            }
        } else if (s == null) {
            return;
        } else if (nameField) {
            AuthorList list = AuthorList.getAuthorList(s.toString());
            for (int i=0; i<list.size(); i++) {
                AuthorList.Author a = list.getAuthor(i);
                addName(a.getFirst(), false);
                addName(a.getLast(), true);
            }
        } else if (entireField) {
            addWord(s.toString().trim());
        } else {
            StringTokenizer tok = new StringTokenizer(s.toString(), " .,\n");
            while (tok.hasMoreTokens()) {
                String word = tok.nextToken();
                //Util.pr(word);
                addWord(word);
            }
        }
    }

    public void addName(String s, boolean lastName) {
        if (s == null)
            return;
        String[] parts = s.replace("\\.", "").split(" ");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.length() >= SHORTEST_WORD) {
                words.add(part);
                if (lastName)
                    lastNames.add(part);
                else
                    firstNames.add(part);
            }
        }
    }

    public Object[] complete(String s) {
        return complete(s, words);
    }

    public Object[] completeName(String s, boolean lastName) {
        if (nameField) {
            if (lastName)
                return complete(s, lastNames);
            else
                return complete(s, firstNames);
        }
        else return null;
    }

    public Object[] complete(String s, TreeSet<String> set) {
        if (s.length() < SHORTEST_TO_COMPLETE)
            return null;

        char lastChar = s.charAt(s.length() - 1);
        String ender = s.substring(0, s.length() - 1)
                + Character.toString((char) (lastChar + 1));
        return set.subSet(s, ender).toArray();
    }

}
