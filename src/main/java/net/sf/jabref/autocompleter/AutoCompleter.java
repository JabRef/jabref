package net.sf.jabref.autocompleter;

import net.sf.jabref.BibtexEntry;

public interface AutoCompleter {

    /**
     * Add a BibtexEntry to this autocompleter. The autocompleter (respectively
     * to the concrete implementations of {@link AbstractAutoCompleter}) itself
     * decides which information should be stored for later completion.
     */
    void addBibtexEntry(BibtexEntry entry);

    /**
     * States whether the field consists of multiple values (false) or of a single value (true)
     * <p/>
     * Symptom: if false, net.sf.jabref.gui.AutoCompleteListener#getCurrentWord(JTextComponent comp)
     * returns current word only, if true, it returns the text beginning from the buffer
     */
    boolean isSingleUnitField();

    void addWordToIndex(String word);

    String getPrefix();

    String[] complete(String str);

    boolean indexContainsWord(String word);

}
