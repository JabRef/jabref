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
 * This formatter outputs the input value after adding a prefix and a postfix,
 * as long as the input value is non-empty. If the input value is empty, an
 * empty string is output (the prefix and postfix are not output in this case).
 *
 * The formatter requires an argument containing the prefix and postix separated
 * by a comma. To include a the comma character in either, use an escape sequence
 * (\,).
 */
public class WrapContent extends AbstractParamLayoutFormatter {

    private String before;
    private String after;


    @Override
    public void setArgument(String arg) {
        String[] parts = AbstractParamLayoutFormatter.parseArgument(arg);
        if (parts.length < 2) {
            return;
        }
        before = parts[0];
        after = parts[1];
    }

    @Override
    public String format(String fieldText) {
        if (fieldText == null) {
            return null;
        }
        if (before == null) {
            return fieldText;
        }
        if (fieldText.isEmpty()) {
            return "";
        } else {
            return before + fieldText + after;
        }
    }
}
