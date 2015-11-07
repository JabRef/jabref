package net.sf.jabref.logic.autocompleter;

import net.sf.jabref.model.entry.BibtexEntry;

/**
 * Delivers possible completions for a given string.
 */
public interface AutoCompleter<E> {

    /**
     * Formats the specified item. This method is called when an item is selected by the user and we need to determine
     * the text to be inserted in the textbox.
     * 
     * @param item the item to format
     * @return formated string representation of the item
     */
    String getAutoCompleteText(E item);
    
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
	 * Returns one or more possible completions for a given string. The returned
	 * completion depends on which informations were stored while adding
	 * BibtexEntries by the used implementation of {@link AutoCompleter}.
	 * 
	 * @see AutoCompleter#addBibtexEntry(BibtexEntry)
	 */
    E[] complete(String toComplete);

    boolean indexContainsWord(String word);

}
