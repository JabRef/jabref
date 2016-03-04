/*  Copyright (C) 2016 JabRef contributors.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.logic.layout.LayoutFormatter;

/**
 * Converts number to ordinal
 */
public class Ordinal implements LayoutFormatter {

    // Detect last digit in number not directly followed by a letter plus the number 11
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(1?\\d\\b)");

    @Override
    public String format(String fieldText) {
        if (fieldText == null) {
            return null;
        }
        Matcher m = NUMBER_PATTERN.matcher(fieldText);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String result = m.group(1);
            int value = Integer.parseInt(result);
            String ordinalString;
            switch (value) {
            case 1:
                ordinalString = "st";
                break;
            case 2:
                ordinalString = "nd";
                break;
            case 3:
                ordinalString = "rd";
                break;
            default:
                ordinalString = "th";
                break;
            }
            m.appendReplacement(sb, result + ordinalString);
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
