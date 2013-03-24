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
package net.sf.jabref.search;

import antlr.CommonAST;
import java.util.regex.Pattern;

public class RegExNode extends CommonAST {
	private Pattern pattern = null;
	public RegExNode(int tokenType, String text, boolean caseSensitive, boolean regex) {
		initialize(tokenType,text);
		pattern = Pattern.compile(
			regex ? text : "\\Q" + text + "\\E", // quote if !regex
			caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
	}
	public Pattern getPattern() {
		return pattern;
	}
}
