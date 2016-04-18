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
package net.sf.jabref.importer.fileformat;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.util.strings.StringUtil;

/**
 * This class provides the reformatting needed when reading BibTeX fields formatted
 * in JabRef style. The reformatting must undo all formatting done by JabRef when
 * writing the same fields.
 */
public class FieldContentParser {
    private final HashSet<String> multiLineFields;

    // 's' matches a space, tab, new line, carriage return.
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public FieldContentParser() {
        multiLineFields = new HashSet<>();
        // the following two are also coded in net.sf.jabref.exporter.LatexFieldFormatter.format(String, String)
        multiLineFields.add("abstract");
        multiLineFields.add("review");
        // the file field should not be formatted, therefore we treat it as a multi line field
        List<String> nonWrappableFields = Globals.prefs.getStringList(JabRefPreferences.NON_WRAPPABLE_FIELDS);
        multiLineFields.addAll(nonWrappableFields);
    }

    /**
     * Performs the reformatting
     *
     * @param fieldContent the content to format
     * @param bibtexField the name of the bibtex field
     * @return the formatted field content.
     */
    public String format(String fieldContent, String bibtexField) {

        if (multiLineFields.contains(bibtexField)) {
            // Unify line breaks
            return StringUtil.unifyLineBreaksToConfiguredLineBreaks(fieldContent);
        }

        return WHITESPACE.matcher(fieldContent).replaceAll(" ");
    }

    public String format(StringBuilder fieldContent, String bibtexField) {
        return format(fieldContent.toString(), bibtexField);
    }
}
