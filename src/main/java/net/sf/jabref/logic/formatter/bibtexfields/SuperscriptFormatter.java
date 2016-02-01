/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.logic.formatter.bibtexfields;

import net.sf.jabref.logic.formatter.Formatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class transforms ordinal numbers into LaTex superscripts.
 */
public class SuperscriptFormatter implements Formatter {

    // find possible superscripts on word boundaries
    private static final Pattern PATTERN = Pattern.compile("\\b(\\d+)(st|nd|rd|th)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);


    @Override
    public String getName() {
        return "Superscripts";
    }

    @Override
    public String getKey() {
        return "SuperscriptFormatter";
    }

    /**
     * Converts ordinal numbers to superscripts, e.g. 1st, 2nd or 3rd.
     * Will replace ordinal numbers even if they are semantically wrong, e.g. 21rd
     *
     * <example>
     * 1st Conf. Cloud Computing -> 1\textsuperscript{st} Conf. Cloud Computing
     * </example>
     */
    @Override
    public String format(String value) {
        // adds superscript tag
        final String replace = "$1\\\\textsuperscript{$2}";

        // nothing to do
        if ((value == null) || value.isEmpty()) {
            return value;
        }

        Matcher matcher = PATTERN.matcher(value);
        // replace globally

        return matcher.replaceAll(replace);
    }
}
