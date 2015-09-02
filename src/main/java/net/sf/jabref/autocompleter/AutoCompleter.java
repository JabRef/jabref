package net.sf.jabref.autocompleter;

import net.sf.jabref.BibtexEntry;

/**
 * An AutoCompleter delivers possible completions for a given string.
 */
public interface AutoCompleter<E> {

    /**
     * Add a BibtexEntry to this AutoCompleter. The AutoCompleter (respectively
     * to the concrete implementations of {@link AutoCompleter}) itself
     * decides which information should be stored for later completion.
     */
    void addBibtexEntry(BibtexEntry entry);

    /**
     * States whether the field consists of multiple values (false) or of a single value (true)
     *
     * Symptom: if false, net.sf.jabref.gui.AutoCompleteListener#getCurrentWord(JTextComponent comp)
     * returns current word only, if true, it returns the text beginning from the buffer
     */
    boolean isSingleUnitField();

    void addWordToIndex(String word);

    String getPrefix();

    /**
	 * Returns one or more possible completions for a given String. The returned
	 * completion depends on which informations were stored while adding
	 * BibtexEntries by the used implementation of {@link AutoCompleter}
	 * .
	 * 
	 * @see AutoCompleter#addBibtexEntry(BibtexEntry)
	 */
    E[] complete(String str);

    boolean indexContainsWord(String word);

}
