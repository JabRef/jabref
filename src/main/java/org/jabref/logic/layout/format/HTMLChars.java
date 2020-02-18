package org.jabref.logic.layout.format;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.util.strings.HTMLUnicodeConversionMaps;
import org.jabref.model.strings.StringUtil;

/**
 * This formatter escapes characters so they are suitable for HTML.
 */
public class HTMLChars implements LayoutFormatter {

    private static final Map<String, String> HTML_CHARS = HTMLUnicodeConversionMaps.LATEX_HTML_CONVERSION_MAP;
    private static boolean[] visited = new boolean[41];

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
            visited[0] = true;
            c = field.charAt(i);
            if (escaped && (c == '\\')) {
                visited[1] = true;
                sb.append('\\');
                escaped = false;
            } else if (c == '\\') {
                visited[2] = true;
                if (incommand) {
                    visited[3] = true;
                    /* Close Command */
                    String command = currentCommand.toString();
                    String result = HTML_CHARS.get(command);
                    if (result == null) {
                        visited[4] = true;
                        sb.append(command);
                    } else {
                        visited[5] = true;
                        sb.append(result);
                    }
                } else {
                    visited[6] = true;
                }
                escaped = true;
                incommand = true;
                currentCommand = new StringBuilder();
            } else if (!incommand && ((c == '{') || (c == '}'))) {
                visited[7] = true;
                // Swallow the brace.
            } else if (Character.isLetter(c) || (c == '%')
                    || StringUtil.SPECIAL_COMMAND_CHARS.contains(String.valueOf(c))) {
                visited[8] = true;
                escaped = false;

                if (!incommand) {
                    visited[9] = true;
                    sb.append(c);
                } else {
                    visited[10] = true;
                    currentCommand.append(c);
                    testCharCom: if ((currentCommand.length() == 1)
                            && StringUtil.SPECIAL_COMMAND_CHARS.contains(currentCommand.toString())) {
                        visited[11] = true;
                        // This indicates that we are in a command of the type
                        // \^o or \~{n}
                        if (i >= (field.length() - 1)) {
                            visited[12] = true;
                            break testCharCom;
                        } else {
                            visited[13] = true;
                        }

                        String command = currentCommand.toString();
                        i++;
                        c = field.charAt(i);
                        String commandBody;
                        if (c == '{') {
                            visited[14] = true;
                            String part = StringUtil.getPart(field, i, false);
                            i += part.length();
                            commandBody = part;
                        } else {
                            visited[15] = true;
                            commandBody = field.substring(i, i + 1);
                        }
                        String result = HTML_CHARS.get(command + commandBody);

                        if (result == null) {
                            visited[16] = true;
                            sb.append(commandBody);
                        } else {
                            visited[17] = true;
                            sb.append(result);
                        }
                        incommand = false;
                        escaped = false;
                    } else {
                        visited[18] = true;
                        //	Are we already at the end of the string?
                        if ((i + 1) == field.length()) {
                            visited[19] = true;
                            String command = currentCommand.toString();
                            String result = HTML_CHARS.get(command);
                            /* If found, then use translated version. If not,
                             * then keep
                             * the text of the parameter intact.
                             */
                            if (result == null) {
                                visited[20] = true;
                                sb.append(command);
                            } else {
                                visited[21] = true;
                                sb.append(result);
                            }

                        } else {
                            visited[22] = true;
                        }
                    }
                }
            } else {
                visited[23] = true;
                if (!incommand) {
                    visited[24] = true;
                    sb.append(c);
                } else if (Character.isWhitespace(c) || (c == '{') || (c == '}')) {
                    visited[25] = true;
                    String command = currentCommand.toString();

                    // Test if we are dealing with a formatting
                    // command.
                    // If so, handle.
                    String tag = getHTMLTag(command);
                    if (!tag.isEmpty()) {
                        visited[26] = true;
                        String part = StringUtil.getPart(field, i, true);
                        i += part.length();
                        sb.append('<').append(tag).append('>').append(part).append("</").append(tag).append('>');
                    } else if (c == '{') {
                        visited[27] = true;
                        String argument = StringUtil.getPart(field, i, true);
                        i += argument.length();
                        // handle common case of general latex command
                        String result = HTML_CHARS.get(command + argument);
                        // If found, then use translated version. If not, then keep
                        // the text of the parameter intact.

                        if (result == null) {
                            visited[28] = true;
                            if (argument.isEmpty()) {
                                visited[29] = true;
                                // Maybe a separator, such as in \LaTeX{}, so use command
                                sb.append(command);
                            } else {
                                visited[30] = true;
                                // Otherwise, use argument
                                sb.append(argument);
                            }
                        } else {
                            visited[31] = true;
                            sb.append(result);
                        }
                    } else if (c == '}') {
                        visited[32] = true;
                        // This end brace terminates a command. This can be the case in
                        // constructs like {\aa}. The correct behaviour should be to
                        // substitute the evaluated command and swallow the brace:
                        String result = HTML_CHARS.get(command);
                        if (result == null) {
                            visited[33] = true;
                            // If the command is unknown, just print it:
                            sb.append(command);
                        } else {
                            visited[34] = true;
                            sb.append(result);
                        }
                    } else {
                        visited[35] = true;
                        String result = HTML_CHARS.get(command);
                        if (result == null) {
                            visited[36] = true;
                            sb.append(command);
                        } else {
                            visited[37] = true;
                            sb.append(result);
                        }
                        sb.append(' ');
                    }
                } else {
                    visited[38] = true;
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
        } if (field.length() == 0) {
            visited[39] = true;
        }
        try {
            File directory = new File("/Temp");
            if (!directory.exists()) {
                directory.mkdir();
            }
            File f = new File(directory + "/format.txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            double frac = 0;
            for (int j = 0; j < visited.length; ++j) {
                frac += (visited[j] ? 1 : 0);
                bw.write("branch " + j + " was" + (visited[j] ? " visited. " : " not visited. ") + "\n");
        }
        bw.write("" + frac / visited.length);
        bw.close();
        } catch (Exception e) {
            System.err.println("ye");
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
