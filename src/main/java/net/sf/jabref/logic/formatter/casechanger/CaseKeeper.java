/*  Copyright (C) 2012 JabRef contributors.
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
package net.sf.jabref.logic.formatter.casechanger;

import java.util.Arrays;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.StringLengthComparator;

public class CaseKeeper implements Formatter {

    private String format(String text, String[] listOfWords) {
        if (text == null) {
            return null;
        }
        Arrays.sort(listOfWords, new StringLengthComparator());
        // For each word in the list
        for (String listOfWord : listOfWords) {
            // Add {} if the character before is a space, -, /, (, [, ", or } or if it is at the start of the string but not if it is followed by a }
            text = text.replaceAll("(^|[- /\\[(}\"])" + listOfWord + "($|[^}])", "$1\\{" + listOfWord + "\\}$2");
        }
        return text;
    }

    @Override
    public String format(String text) {
        if (text == null) {
            return null;
        }
        final CaseKeeperList list = new CaseKeeperList();
        return this.format(text, list.getAll());
    }

    @Override
    public String getName() {
        return Localization.lang("CaseKepper");
    }

}
