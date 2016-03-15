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
package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.AbstractParamLayoutFormatter;

/**
 * Formatter that does regexp replacement.
 *
 * To use this formatter, a two-part argument must be given. The parts are
 * separated by a comma. To indicate the comma character, use an escape
 * sequence: \,
 * For inserting newlines and tabs in arguments, use \n and \t, respectively.
 *
 * The first part is the regular expression to search for. Remember that any commma
 * character must be preceded by a backslash, and consequently a literal backslash must
 * be written as a pair of backslashes. A description of Java regular expressions can be
 * found at:
 *   http://java.sun.com/j2se/1.4.2/docs/api/java/util/regex/Pattern.html
 *
 * The second part is the text to replace all matches with.
 *
 * For instance:
 *  \format[Replace(and,&)]{\author} :
 *      will output the "author" field after replacing all occurences of "and"
 *      by "&"
 *
 *  \format[Replace(\s,_)]{\author} :
 *      will output the "author" field after replacing all whitespace
 *      by underscores.
 *
 *  \format[Replace(\,,;)]{\author} :
 *      will output the "author" field after replacing all commas by semicolons.
 *
 */
public class Replace extends AbstractParamLayoutFormatter {

    private String regex;
    private String replaceWith;


    @Override
    public void setArgument(String arg) {
        String[] parts = AbstractParamLayoutFormatter.parseArgument(arg);

        if (parts.length < 2) {
            return; // TODO: too few arguments. Print an error message here?
        }
        regex = parts[0];
        replaceWith = parts[1];

    }

    @Override
    public String format(String fieldText) {
        if ((fieldText == null) || (regex == null)) {
            return fieldText; // TODO: argument missing or invalid. Print an error message here?
        }
        return fieldText.replaceAll(regex, replaceWith);
    }
}
