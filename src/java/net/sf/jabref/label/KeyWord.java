/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.label;

import java.util.HashSet;

public class KeyWord extends HashSet<String> {

	private static KeyWord singleton;

	private KeyWord() {
		// puts all keywords in
		add("society");
		add("transaction");
		add("transactions");
		add("journal");
		add("review");
		add("revue");
		add("communication");
		add("communications");
		add("letters");
		add("advances");
		add("proceedings");
		add("proceeding");
		add("international");
		add("joint");
		add("conference");
	}
 
	public static KeyWord getKeyWord() {
		if (singleton == null)
			singleton = new KeyWord();
		return singleton;
	}

	public boolean isKeyWord(String matchWord) {
		if (contains(matchWord.toLowerCase())) {
			return true;
		}
		return false;
	}

	public boolean isKeyWordMatchCase(String matchWord) {
		if (contains(matchWord)) {
			return true;
		}
		return false;
	}

}
