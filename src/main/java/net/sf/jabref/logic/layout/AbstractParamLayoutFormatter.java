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
package net.sf.jabref.logic.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * This is an abstract implementation of ParamLayoutFormatter, which provides some
 * functionality for the handling of argument strings.
 */
public abstract class AbstractParamLayoutFormatter implements ParamLayoutFormatter {

    private static final char SEPARATOR = ',';


    /**
     * Parse an argument string and return the parts of the argument. The parts are
     * separated by commas, and escaped commas are reduced to literal commas.
     * @param arg The argument string.
     * @return An array of strings representing the parts of the argument.
     */
    protected static String[] parseArgument(String arg) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < arg.length(); i++) {
            if ((arg.charAt(i) == AbstractParamLayoutFormatter.SEPARATOR) && !escaped) {
                parts.add(current.toString());
                current = new StringBuilder();
            } else if (arg.charAt(i) == '\\') {
                if (escaped) {
                    escaped = false;
                    current.append(arg.charAt(i));
                } else {
                    escaped = true;
                }
            } else if (escaped) {
                // Handle newline and tab:
                if (arg.charAt(i) == 'n') {
                    current.append('\n');
                } else if (arg.charAt(i) == 't') {
                    current.append('\t');
                } else {
                    if ((arg.charAt(i) != ',') && (arg.charAt(i) != '"')) {
                        current.append('\\');
                    }
                    current.append(arg.charAt(i));
                }
                escaped = false;
            } else {
                current.append(arg.charAt(i));
            }
        }
        parts.add(current.toString());
        return parts.toArray(new String[parts.size()]);
    }
}
