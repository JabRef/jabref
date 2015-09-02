/*  Copyright (C) 2003-2012 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.autocompleter;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.text.JTextComponent;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

/**
 * An autocompleter delivers possible completions for a given String. There are
 * different types of autocompleters for different use cases.
 * 
 * Example: {@link NameFieldAutoCompleter}, {@link EntireFieldAutoCompleter}
 * 
 * @author kahlert, cordes, olly98
 * @see AutoCompleterFactory
 */

public abstract class AbstractAutoCompleter<E> {

	public static int SHORTEST_TO_COMPLETE = Globals.prefs.getInt(JabRefPreferences.SHORTEST_TO_COMPLETE);
	
	/**
	 * Add a BibtexEntry to this autocompleter. The autocompleter (respectively
	 * to the concrete implementations of {@link AbstractAutoCompleter}) itself
	 * decides which information should be stored for later completion.
	 * 
	 */
	abstract public void addBibtexEntry(BibtexEntry entry);
	
	/**
	 * States whether the field consists of multiple values (false) or of a single value (true)
	 * 
	 * Symptom: if false, {@link net.sf.jabref.gui.AutoCompleteListener#getCurrentWord(JTextComponent comp)} 
	 * returns current word only, if true, it returns the text beginning from the buffer
	 */
	abstract public boolean isSingleUnitField();
	
	/**
	 * Returns one or more possible completions for a given String. The returned
	 * completion depends on which informations were stored while adding
	 * BibtexEntries by the used implementation of {@link AbstractAutoCompleter}
	 * .
	 * 
	 * @see AbstractAutoCompleter#addBibtexEntry(BibtexEntry)
	 */
	abstract public E[] complete(String str);
	
	abstract public boolean indexContainsWord(String word);
	
	abstract public void addWordToIndex(String word);
	
	public String getPrefix() {
        return "";
    }
}
