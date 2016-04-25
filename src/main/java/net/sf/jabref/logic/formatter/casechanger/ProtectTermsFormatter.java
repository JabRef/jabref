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

import java.util.List;
import java.util.Objects;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.StringLengthComparator;

public class ProtectTermsFormatter implements Formatter {

    private String format(String text, List<String> listOfWords) {
        String result = text;
        listOfWords.sort(new StringLengthComparator());
        // For each word in the list
        for (String listOfWord : listOfWords) {
            // Add {} if the character before is a space, -, /, (, [, ", or } or if it is at the start of the string but not if it is followed by a }
            result = result.replaceAll("(^|[- /\\[(}\"])" + listOfWord + "($|[^}])", "$1\\{" + listOfWord + "\\}$2");
        }
        return result;
    }

    @Override
    public String format(String text) {
        Objects.requireNonNull(text);

        if (text.isEmpty()) {
            return text;
        }
        return this.format(text, CaseKeeperList.getAll());
    }

    @Override
    public String getDescription() {
        return Localization.lang(
                "Adds {} brackets around acronyms, month names and countries to preserve their case.");
    }

    @Override
    public String getExampleInput() {
        return "In CDMA";
    }

    @Override
    public String getName() {
        return Localization.lang("Protect terms");
    }

    @Override
    public String getKey() {
        return "protect_terms";
    }

}
