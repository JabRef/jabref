/*  Copyright (C) 2015 JabRef contributors.
    Copyright (C) 2015 Oscar Gustafsson.

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
package net.sf.jabref.logic.util.strings;

import java.util.*;

import net.sf.jabref.importer.HTMLConverter;

/**
 * Class with static methods for converting strings from HTML and Unicode to LaTeX encoding.
 *
 * @author Oscar Gustafsson
 */
public class Converters {

    private static final HTMLConverter htmlConverter = new HTMLConverter();


    public interface Converter {

        String getName();

        String convert(String input);
    }

    public static class UnicodeToLatexConverter implements Converter {

        @Override
        public String getName() {
            return "Unicode to LaTeX";
        }

        @Override
        public String convert(String input) {
            return Converters.htmlConverter.formatUnicode(input);
        }
    }

    public static class HTMLToLatexConverter implements Converter {

        @Override
        public String getName() {
            return "HTML to LaTeX";
        }

        @Override
        public String convert(String input) {
            return Converters.htmlConverter.format(input);
        }
    }


    public static final UnicodeToLatexConverter UNICODE_TO_LATEX = new UnicodeToLatexConverter();
    public static final HTMLToLatexConverter HTML_TO_LATEX = new HTMLToLatexConverter();

    public static final List<Converter> ALL = Arrays.asList(Converters.HTML_TO_LATEX, Converters.UNICODE_TO_LATEX);
}
