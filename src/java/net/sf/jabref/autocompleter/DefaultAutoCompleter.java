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

import java.util.StringTokenizer;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;

/**
 * Stores all words which are separated by Globals.SEPARATING_CHARS. This
 * autocompleter only processes the field which is given by the fieldname.
 * 
 * @author kahlert, cordes
 * 
 */
public class DefaultAutoCompleter extends AbstractAutoCompleter {

	public String _fieldName;

	/**
	 * @see AutoCompleterFactory
	 */
	protected DefaultAutoCompleter(String fieldName) {
		_fieldName = fieldName;
	}

	public boolean isSingleUnitField() {
		return false;
	}

	public String[] complete(String s) {
		return super.complete(s);
	}

	@Override
	public void addBibtexEntry(BibtexEntry entry) {
		if (entry != null) {
			String fieldValue = entry.getField(_fieldName);
			if (fieldValue == null) {
				return;
			} else {
				StringTokenizer tok = new StringTokenizer(fieldValue.toString(), Globals.SEPARATING_CHARS);
				while (tok.hasMoreTokens()) {
					String word = tok.nextToken();
					addWordToIndex(word);
				}
			}
		}
	}
}
