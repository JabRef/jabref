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
package net.sf.jabref.exporter.layout.format;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.logic.util.strings.HTMLUnicodeConversionMaps;
import net.sf.jabref.exporter.layout.LayoutFormatter;

import java.util.Map;

/**
 * This formatter converts LaTeX character sequences their equivalent unicode characters,
 * and removes other LaTeX commands without handling them.
 */
public class FormatChars implements LayoutFormatter {

    private static final Map<String, String> CHARS = HTMLUnicodeConversionMaps.LATEX_UNICODE_CONVERSION_MAP;

    @Override
    public String format(String field) {
        int i;
        field = field.replaceAll("&|\\\\&", "&amp;").replaceAll("[\\n]{1,}", "<p>");

        StringBuilder sb = new StringBuilder();
        StringBuffer currentCommand = null;

        char c;
        boolean escaped = false;
        boolean incommand = false;

        for (i = 0; i < field.length(); i++) {
            c = field.charAt(i);
            if (escaped && (c == '\\')) {
                sb.append('\\');
                escaped = false;
            } else if (c == '\\') {
                if (incommand) {
                    /* Close Command */
                    String command = currentCommand.toString();
                    Object result = FormatChars.CHARS.get(command);
                    if (result == null) {
                        sb.append(command);
                    } else {
                        sb.append((String) result);
                    }
                }
                escaped = true;
                incommand = true;
                currentCommand = new StringBuffer();
            } else if (!incommand && ((c == '{') || (c == '}'))) {
                // Swallow the brace.
            } else if (Character.isLetter(c) || (c == '%')
                    || Globals.SPECIAL_COMMAND_CHARS.contains(String.valueOf(c))) {
                escaped = false;

                if (!incommand) {
                    sb.append(c);
                } else {
                    currentCommand.append(c);
                    testCharCom: if ((currentCommand.length() == 1)
                            && Globals.SPECIAL_COMMAND_CHARS.contains(currentCommand.toString())) {
                        // This indicates that we are in a command of the type
                        // \^o or \~{n}
                        if (i >= (field.length() - 1)) {
                            break testCharCom;
                        }

                        String command = currentCommand.toString();
                        i++;
                        c = field.charAt(i);
                        // System.out.println("next: "+(char)c);
                        String combody;
                        if (c == '{') {
                            String part = StringUtil.getPart(field, i, false);
                            i += part.length();
                            combody = part;
                        } else {
                            combody = field.substring(i, i + 1);
                            // System.out.println("... "+combody);
                        }
                        Object result = FormatChars.CHARS.get(command + combody);

                        if (result != null) {
                            sb.append((String) result);
                        }

                        incommand = false;
                        escaped = false;
                    } else {
                        //	Are we already at the end of the string?
                        if ((i + 1) == field.length()) {
                            String command = currentCommand.toString();
                            Object result = FormatChars.CHARS.get(command);
                            /* If found, then use translated version. If not,
                             * then keep
                             * the text of the parameter intact.
                             */
                            if (result == null) {
                                sb.append(command);
                            } else {
                                sb.append((String) result);
                            }

                        }
                    }
                }
            } else {
                String argument;

                if (!incommand) {
                    sb.append(c);
                } else if (Character.isWhitespace(c) || (c == '{') || (c == '}')) {
                    // First test if we are already at the end of the string.
                    // if (i >= field.length()-1)
                    // break testContent;

                    String command = currentCommand.toString();

                    if (c == '{') {
                        String part = StringUtil.getPart(field, i, true);
                        i += part.length();
                        argument = part;
                        if (argument != null) {
                            // handle common case of general latex command
                            Object result = FormatChars.CHARS.get(command + argument);
                            // System.out.print("command: "+command+", arg: "+argument);
                            // System.out.print(", result: ");
                            // If found, then use translated version. If not, then keep
                            // the
                            // text of the parameter intact.
                            if (result == null) {
                                sb.append(argument);
                            } else {
                                sb.append((String) result);
                            }
                        }
                    } else if (c == '}') {
                        // This end brace terminates a command. This can be the case in
                        // constructs like {\aa}. The correct behaviour should be to
                        // substitute the evaluated command and swallow the brace:
                        Object result = FormatChars.CHARS.get(command);
                        if (result == null) {
                            // If the command is unknown, just print it:
                            sb.append(command);
                        } else {
                            sb.append((String) result);
                        }
                    } else {
                        Object result = FormatChars.CHARS.get(command);
                        if (result == null) {
                            sb.append(command);
                        } else {
                            sb.append((String) result);
                        }
                        sb.append(' ');
                    }
                }/* else if (c == '}') {
                    System.out.printf("com term by }: '%s'\n", currentCommand.toString());

                    argument = "";
                 }*/else {
                     /*
                      * TODO: this point is reached, apparently, if a command is
                      * terminated in a strange way, such as with "$\omega$".
                      * Also, the command "\&" causes us to get here. The former
                      * issue is maybe a little difficult to address, since it
                      * involves the LaTeX math mode. We don't have a complete
                      * LaTeX parser, so maybe it's better to ignore these
                      * commands?
                      */
                 }

                incommand = false;
                escaped = false;
            }
        }

        return sb.toString();
    }

}
