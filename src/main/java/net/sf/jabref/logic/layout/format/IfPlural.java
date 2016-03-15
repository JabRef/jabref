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
 * @author ralmond
 *
 * This formatter takes two arguments and examines the field text.
 * If the field text represents multiple individuals, that is it contains the string "and"
 * then the field text is replaced with the first argument, otherwise it is replaced with the second.
 * For example:
 *
 * \format[IfPlural(Eds.,Ed.)]{\editor}
 *
 * Should expand to 'Eds.' if the document has more than one editor and 'Ed.' if it only has one.
 *
 *
 */
public class IfPlural extends AbstractParamLayoutFormatter {

    private String pluralText;
    private String singularText;


    @Override
    public void setArgument(String arg) {
        String[] parts = AbstractParamLayoutFormatter.parseArgument(arg);

        if (parts.length < 2) {
            return; // TODO: too few arguments. Print an error message here?
        }
        pluralText = parts[0];
        singularText = parts[1];

    }

    @Override
    public String format(String fieldText) {
        if ((fieldText == null) || fieldText.isEmpty() || (pluralText == null)) {
            return ""; // TODO: argument missing or invalid. Print an error message here?
        }
        if (fieldText.matches(".*\\sand\\s.*")) {
            return pluralText;
        } else {
            return singularText;
        }
    }

}
