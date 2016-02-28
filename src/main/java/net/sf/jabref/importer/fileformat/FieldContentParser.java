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

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.util.strings.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides the reformatting needed when reading BibTeX fields formatted
 * in JabRef style. The reformatting must undo all formatting done by JabRef when
 * writing the same fields.
 */
public class FieldContentParser {
    private final List<String> multiLineFields;

    public FieldContentParser() {
        multiLineFields = new ArrayList<>();
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
     * @param text2     StringBuffer containing the field to format. bibtexField contains field name according to field
     * @param bibtexField
     * @return The formatted field content. The StringBuffer returned may or may not be the same as the argument given.
     */
    public StringBuilder format(StringBuilder text2, String bibtexField) {

        // Unify line breaks
        String text = StringUtil.unifyLineBreaksToConfiguredLineBreaks(text2.toString());

        // Do not format multiline fields
        if (multiLineFields.contains(bibtexField)) {
            return new StringBuilder(text);
        }

        // 's' matches a space, tab, new line, carriage return.
        text = text.replaceAll("\\s+", " ");

        return new StringBuilder(text);
    }

    public String format(String content, String bibtexField) {
        return format(new StringBuilder(content), bibtexField).toString();
    }
}
