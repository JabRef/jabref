package net.sf.jabref.logic.autocompleter;

import java.util.List;

import net.sf.jabref.model.entry.BibEntry;

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
     * Add a BibEntry to this AutoCompleter.
     * @note The AutoCompleter itself decides which information should be stored for later completion.
     */
    void addBibtexEntry(BibEntry entry);

    /**
     * States whether the field consists of multiple values (false) or of a single value (true)
     *
     * Symptom: if false, net.sf.jabref.gui.AutoCompleteListener#getCurrentWord(JTextComponent comp)
     * returns current word only, if true, it returns the text beginning from the buffer.
     */
    boolean isSingleUnitField();

    /**
     * Unclear what this method should do.
     * TODO: Remove this method once the AutoCompleteListener is removed.
     */
    String getPrefix();

    /**
     * Returns one or more possible completions for a given string. The returned
     * completion depends on which informations were stored while adding
     * BibtexEntries. If no suggestions for completions are found, then an empty list is returned.
     *
     * @see AutoCompleter#addBibtexEntry(BibEntry)
     */
    List<E> complete(String toComplete);

    /**
     * Directly adds an item to the AutoCompleter.
     * This method should be called only if the information does not comes directly from a BibEntry.
     * Otherwise the {@link #addBibtexEntry(BibEntry)} is preferred.
     * @param item item to add
     */
    void addItemToIndex(E item);
}
