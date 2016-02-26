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

import java.util.Map;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.layout.LayoutFormatter;
import net.sf.jabref.logic.util.strings.HTMLUnicodeConversionMaps;
import net.sf.jabref.logic.util.strings.StringUtil;

/**
 * This formatter escapes characters so they are suitable for HTML.
 */
public class HTMLChars implements LayoutFormatter {

    private static final Map<String, String> HTML_CHARS = HTMLUnicodeConversionMaps.LATEX_HTML_CONVERSION_MAP;

    @Override
    public String format(String inField) {
        int i;
        String field = inField.replaceAll("&|\\\\&", "&amp;") // Replace & and \& with &amp;
                .replaceAll("[\\n]{2,}", "<p>") // Replace double line breaks with <p>
                .replace("\n", "<br>") // Replace single line breaks with <br>
                .replace("\\$", "&dollar;") // Replace \$ with &dollar;
                .replaceAll("\\$([^\\$]*)\\$", "\\{$1\\}"); // Replace $...$ with {...} to simplify conversion

        StringBuilder sb = new StringBuilder();
        StringBuilder currentCommand = null;

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
                    Object result = HTML_CHARS.get(command);
                    if (result == null) {
                        sb.append(command);
                    } else {
                        sb.append((String) result);
                    }
                }
                escaped = true;
                incommand = true;
                currentCommand = new StringBuilder();
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
                        String combody;
                        if (c == '{') {
                            String part = StringUtil.getPart(field, i, false);
                            i += part.length();
                            combody = part;
                        } else {
                            combody = field.substring(i, i + 1);
                        }
                        Object result = HTML_CHARS.get(command + combody);

                        if (result != null) {
                            sb.append((String) result);
                        }

                        incommand = false;
                        escaped = false;
                    } else {
                        //	Are we already at the end of the string?
                        if ((i + 1) == field.length()) {
                            String command = currentCommand.toString();
                            Object result = HTML_CHARS.get(command);
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
                    String command = currentCommand.toString();

                    // Test if we are dealing with a formatting
                    // command.
                    // If so, handle.
                    String tag = getHTMLTag(command);
                    if (!tag.isEmpty()) {
                        String part = StringUtil.getPart(field, i, true);
                        i += part.length();
                        sb.append('<').append(tag).append('>').append(part).append("</").append(tag).append('>');
                    } else if (c == '{') {
                        String part = StringUtil.getPart(field, i, true);
                        i += part.length();
                        argument = part;
                        if (argument != null) {
                            // handle common case of general latex command
                            Object result = HTML_CHARS.get(command + argument);
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
                        Object result = HTML_CHARS.get(command);
                        if (result == null) {
                            // If the command is unknown, just print it:
                            sb.append(command);
                        } else {
                            sb.append((String) result);
                        }
                    } else {
                        Object result = HTML_CHARS.get(command);
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

        return sb.toString().replace("~", "&nbsp;"); // Replace any remaining ~ with &nbsp; (non-breaking spaces)
    }

    private String getHTMLTag(String latexCommand) {
        String result = "";
        switch (latexCommand) {
        // Italic
        case "textit":
        case "it":
            result = "i";
            break;
        // Emphasize
        case "emph":
        case "em":
            result = "em";
            break;
        // Bold font
        case "textbf":
        case "bf":
            result = "b";
            break;
        // Underline
        case "underline":
            result = "u";
            break;
        // Strikeout, sout is the "standard" command, although it is actually based on the package ulem
        case "sout":
            result = "s";
            break;
        // Monospace font
        case "texttt":
            result = "tt";
            break;
        // Superscript
        case "textsuperscript":
            result = "sup";
            break;
        // Subscript
        case "textsubscript":
            result = "sub";
            break;
        default:
            break;
        }
        return result;
    }

}
