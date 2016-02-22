/*  Copyright (C) 2003-2016 JabRef contributors.
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

import java.util.Objects;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.layout.LayoutFormatter;
import net.sf.jabref.logic.util.strings.HTMLUnicodeConversionMaps;

public class UnicodeToLatexFormatter implements LayoutFormatter, Formatter {

    private static final Log LOGGER = LogFactory.getLog(UnicodeToLatexFormatter.class);


    public UnicodeToLatexFormatter() {
        super();
    }

    @Override
    public String format(String text) {
        Objects.requireNonNull(text);

        if (text.isEmpty()) {
            return text;
        }

        // Standard symbols
        Set<Character> chars = HTMLUnicodeConversionMaps.UNICODE_LATEX_CONVERSION_MAP.keySet();
        for (Character character : chars) {
            text = text.replaceAll(character.toString(), HTMLUnicodeConversionMaps.UNICODE_LATEX_CONVERSION_MAP.get(character));
        }

        // Combining accents
        StringBuilder sb = new StringBuilder();
        boolean consumed = false;
        for (int i = 0; i <= (text.length() - 2); i++) {
            if (!consumed && (i < (text.length() - 1))) {
                int cpCurrent = text.codePointAt(i);
                Integer cpNext = text.codePointAt(i + 1);
                String code = HTMLUnicodeConversionMaps.ESCAPED_ACCENTS.get(cpNext);
                if (code == null) {
                    sb.append((char) cpCurrent);
                } else {
                    sb.append("{\\" + code + '{' + (char) cpCurrent + "}}");
                    consumed = true;
                }
            } else {
                consumed = false;
            }
        }
        if (!consumed) {
            sb.append((char) text.codePointAt(text.length() - 1));
        }
        text = sb.toString();

        // Check if any symbols is not converted
        for (int i = 0; i <= (text.length() - 1); i++) {
            int cp = text.codePointAt(i);
            if (cp >= 129) {
                LOGGER.warn("Unicode character not converted: " + cp);
            }
        }
        return text;
    }

    @Override
    public String getName() {
        return "UnicodeConverter";
    }

    @Override
    public String getKey() {
        return "UnicodeConverter";
    }
}
