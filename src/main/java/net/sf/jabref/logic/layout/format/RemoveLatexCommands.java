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
package net.sf.jabref.logic.layout.format;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.layout.LayoutFormatter;

public class RemoveLatexCommands implements LayoutFormatter {


    @Override
    public String format(String field) {

        StringBuilder sb = new StringBuilder("");
        StringBuilder currentCommand = null;
        char c;
        boolean escaped = false;
        boolean incommand = false;
        int i;
        for (i = 0; i < field.length(); i++) {
            c = field.charAt(i);
            if (escaped && (c == '\\')) {
                sb.append('\\');
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
                incommand = true;
                currentCommand = new StringBuilder();
            } else if (!incommand && ((c == '{') || (c == '}'))) {
                // Swallow the brace.
            } else if (Character.isLetter(c) ||
                    Globals.SPECIAL_COMMAND_CHARS.contains(String.valueOf(c))) {
                escaped = false;
                if (incommand) {
                    currentCommand.append(c);
                    if ((currentCommand.length() == 1)
                            && Globals.SPECIAL_COMMAND_CHARS.contains(currentCommand.toString())) {
                        // This indicates that we are in a command of the type \^o or \~{n}
                        incommand = false;
                        escaped = false;

                    }
                } else {
                    sb.append(c);
                }
            } else if (Character.isLetter(c)) {
                escaped = false;
                if (incommand) {
                    // We are in a command, and should not keep the letter.
                    currentCommand.append(c);
                } else {
                    sb.append(c);
                }
            } else {
                if (!incommand || (!Character.isWhitespace(c) && (c != '{'))) {
                    sb.append(c);
                } else {
                    if (c != '{') {
                        sb.append(c);
                    }
                }
                incommand = false;
                escaped = false;
            }
        }

        return sb.toString();
    }
}
