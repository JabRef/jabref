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
package net.sf.jabref.logic.layout.format;

import net.sf.jabref.model.entry.AuthorList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jabref.Globals;
import net.sf.jabref.bst.BibtexNameFormatter;
import net.sf.jabref.logic.layout.LayoutFormatter;

/**
 * This layout formatter uses the Bibtex name.format$ method and provides ultimate flexibility:
 *
 * The formatter needs a parameter to be passed in that follows the following format:
 *
 * <case1>@<range11>@"<format>"@<range12>@"<format>"@<range13>...@@
 *
 * <case2>@<range21>@... and so on.
 *
 * Individual cases are separated by @@ and items in a case by @.
 *
 * Cases are just integers or the character * and will tell the formatter to apply the following formats if there are
 * less or equal authors given to it. The cases must be in strict increasing order with the * in the last position.
 *
 * For instance:
 *
 * case1 = 2
 * case2 = 3
 * case3 = *
 *
 * Ranges are either <integer>..<integer>, <integer> or the character * using a 1 based index for indexing
 * authors from the given authorlist. Integer indexes can be negative to denote them to start from
 * the end of the list where -1 is the last author.
 *
 * For instance with an authorlist of "Joe Doe and Mary Jane and Bruce Bar and Arthur Kay":
 *
 * 1..3 will affect Joe, Mary and Bruce
 *
 * 4..4 will affect Arthur
 *
 * * will affect all of them
 *
 * 2..-1 will affect Mary, Bruce and Arthur
 *
 * The <format> uses the Bibtex formatter format:
 *
 * The four letter v, f, l, j indicate the name parts von, first, last, jr which
 * are used within curly braces. A single letter v, f, l, j indicates that the name should be abbreviated.
 * To put a quote in the format string quote it using \" (mh. this doesn't work yet)
 *
 * I give some examples but would rather point you to the bibtex documentation.
 *
 * "{ll}, {f}." Will turn "Joe Doe" into "Doe, J."
 *
 * Complete example:
 *
 * To turn:
 *
 * "Joe Doe and Mary Jane and Bruce Bar and Arthur Kay"
 *
 * into
 *
 * "Doe, J., Jane, M., Bar, B. and Kay, A."
 *
 * you would use
 *
 * 1@*@{ll}, {f}.@@2@1@{ll}, {f}.@2@ and {ll}, {f}.@@*@1..-3@{ll}, {f}., @-2@{ll}, {f}.@-1@ and {ll}, {f}.
 *
 * Yeah this is trouble-some to write, but should work.
 *
 * For more examples see the test-cases.
 *
 */
public class NameFormatter implements LayoutFormatter {

    public static final String DEFAULT_FORMAT = "1@*@{ff }{vv }{ll}{, jj}@@*@1@{ff }{vv }{ll}{, jj}@*@, {ff }{vv }{ll}{, jj}";

    private String parameter = NameFormatter.DEFAULT_FORMAT;

    public static final String NAME_FORMATER_KEY = "nameFormatterNames";

    public static final String NAME_FORMATTER_VALUE = "nameFormatterFormats";

    private static String format(String toFormat, AuthorList al, String[] formats) {

        StringBuilder sb = new StringBuilder();

        int n = al.getNumberOfAuthors();

        for (int i = 1; i <= al.getNumberOfAuthors(); i++) {
            for (int j = 1; j < formats.length; j += 2) {
                if ("*".equals(formats[j])) {
                    sb.append(BibtexNameFormatter.formatName(toFormat, i, formats[j + 1], null));
                    break;
                } else {
                    String[] range = formats[j].split("\\.\\.");

                    int s;
                    int e;
                    if (range.length == 2) {
                        s = Integer.parseInt(range[0]);
                        e = Integer.parseInt(range[1]);
                    } else {
                        s = e = Integer.parseInt(range[0]);
                    }
                    if (s < 0) {
                        s += n + 1;
                    }
                    if (e < 0) {
                        e += n + 1;
                    }
                    if (e < s) {
                        int temp = e;
                        e = s;
                        s = temp;
                    }

                    if ((s <= i) && (i <= e)) {
                        sb.append(BibtexNameFormatter.formatName(toFormat, i, formats[j + 1], null));
                        break;
                    }
                }
            }
        }
        return sb.toString();

    }

    public String format(String toFormat, String inParameters) {

        AuthorList al = AuthorList.parse(toFormat);
        String parameters;

        if ((inParameters == null) || inParameters.isEmpty()) {
            parameters = "*:*:\"{ff}{vv}{ll}{,jj} \"";
        } else {
            parameters = inParameters;
        }

        String[] cases = parameters.split("@@");
        for (String aCase : cases) {
            String[] formatString = aCase.split("@");

            if (formatString.length < 3) {
                // Error
                return toFormat;
            }

            if ("*".equals(formatString[0])) {
                return format(toFormat, al, formatString);
            } else {
                if (al.getNumberOfAuthors() <= Integer.parseInt(formatString[0])) {
                    return format(toFormat, al, formatString);
                }
            }
        }
        return toFormat;
    }

    @Override
    public String format(String fieldText) {
        return format(fieldText, parameter);
    }



    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public static Map<String, String> getNameFormatters() {

        Map<String, String> result = new HashMap<>();

        List<String> names = Globals.prefs.getStringList(NameFormatter.NAME_FORMATER_KEY);
        List<String> formats = Globals.prefs.getStringList(NameFormatter.NAME_FORMATTER_VALUE);

        for (int i = 0; i < names.size(); i++) {
            if (i < formats.size()) {
                result.put(names.get(i), formats.get(i));
            } else {
                result.put(names.get(i), DEFAULT_FORMAT);
            }
        }

        return result;
    }
}
