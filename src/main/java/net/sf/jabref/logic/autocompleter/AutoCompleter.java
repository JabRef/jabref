/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.logic.autocompleter;

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

    String[] complete(String toComplete);

    boolean indexContainsWord(String word);

}
