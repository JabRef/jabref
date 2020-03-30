package org.jabref.logic.openoffice;

import java.util.Map;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.util.strings.HTMLUnicodeConversionMaps;
import org.jabref.model.strings.StringUtil;

/**
 * This formatter preprocesses JabRef fields before they are run through the layout of the
 * bibliography style. It handles translation of LaTeX italic/bold commands into HTML tags.
 */
public class OOPreFormatter implements LayoutFormatter {

    private static final Map<String, String> CHARS = HTMLUnicodeConversionMaps.LATEX_UNICODE_CONVERSION_MAP;

    @Override
    public String format(String field) {
        int i;
        String finalResult = field.replaceAll("&|\\\\&", "&") // Replace & and \& with &
                .replace("\\$", "&dollar;") // Replace \$ with &dollar;
                .replaceAll("\\$([^\\$]*)\\$", "\\{$1\\}"); // Replace $...$ with {...} to simplify conversion

        StringBuilder sb = new StringBuilder();
        StringBuilder currentCommand = null;

        char c;
        boolean escaped = false;
        boolean incommand = false;

        for (i = 0; i < finalResult.length(); i++) {
            c = finalResult.charAt(i);
            if (escaped && (c == '\\')) {
                sb.append('\\');
                escaped = false;
            } else if (c == '\\') {
                if (incommand) {
                    /* Close Command */
                    String command = currentCommand.toString();
                    String result = OOPreFormatter.CHARS.get(command);
                    if (result == null) {
                        sb.append(command);
                    } else {
                        sb.append(result);
                    }
                }
                escaped = true;
                incommand = true;
                currentCommand = new StringBuilder();
            } else if (!incommand && ((c == '{') || (c == '}'))) {
                //Swallow braces, necessary for replacing encoded characters

            } else if (Character.isLetter(c) || (c == '%')
                    || StringUtil.SPECIAL_COMMAND_CHARS.contains(String.valueOf(c))) {
                escaped = false;

                if (!incommand) {
                    sb.append(c);
                } else {
                    currentCommand.append(c);
                    testCharCom: if ((currentCommand.length() == 1)
                            && StringUtil.SPECIAL_COMMAND_CHARS.contains(currentCommand.toString())) {
                        // This indicates that we are in a command of the type
                        // \^o or \~{n}
                        if (i >= (finalResult.length() - 1)) {
                            break testCharCom;
                        }

                        String command = currentCommand.toString();
                        i++;
                        c = finalResult.charAt(i);
                        String combody;
                        if (c == '{') {
                            String part = StringUtil.getPart(finalResult, i, false);
                            i += part.length();
                            combody = part;
                        } else {
                            combody = finalResult.substring(i, i + 1);
                        }
                        String result = OOPreFormatter.CHARS.get(command + combody);

                        if (result != null) {
                            sb.append(result);
                        }

                        incommand = false;
                        escaped = false;
                    } else {
                        //	Are we already at the end of the string?
                        if ((i + 1) == finalResult.length()) {
                            String command = currentCommand.toString();
                            String result = OOPreFormatter.CHARS.get(command);
                            /* If found, then use translated version. If not,
                             * then keep
                             * the text of the parameter intact.
                             */
                            if (result == null) {
                                sb.append(command);
                            } else {
                                sb.append(result);
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
                        String part = StringUtil.getPart(finalResult, i, true);
                        i += part.length();
                        sb.append('<').append(tag).append('>').append(part).append("</").append(tag).append('>');
                    } else if (c == '{') {
                        String part = StringUtil.getPart(finalResult, i, true);
                        i += part.length();
                        argument = part;
                        // handle common case of general latex command
                        String result = OOPreFormatter.CHARS.get(command + argument);
                        // If found, then use translated version. If not, then keep
                        // the
                        // text of the parameter intact.
                        if (result == null) {
                            sb.append(argument);
                        } else {
                            sb.append(result);
                        }
                    } else if (c == '}') {
                        // This end brace terminates a command. This can be the case in
                        // constructs like {\aa}. The correct behaviour should be to
                        // substitute the evaluated command and swallow the brace:
                        String result = OOPreFormatter.CHARS.get(command);
                        if (result == null) {
                            // If the command is unknown, just print it:
                            sb.append(command);
                        } else {
                            sb.append(result);
                        }
                    } else {
                        String result = OOPreFormatter.CHARS.get(command);
                        if (result == null) {
                            sb.append(command);
                        } else {
                            sb.append(result);
                        }
                        sb.append(' ');
                    }
                } /* else if (c == '}') {
                    System.out.printf("com term by }: '%s'\n", currentCommand.toString());

                    argument = "";
                 }*/ else {
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

        return sb.toString().replace("&dollar;", "$"); // Replace &dollar; with $
    }

    private String getHTMLTag(String latexCommand) {
        String result = "";
        switch (latexCommand) {
        // Italic
        case "textit":
        case "it":
        case "emph": // Should really separate between emphasized and italic but since in later stages both are converted to italic...
        case "em":
            result = "i";
            break;
        // Bold font
        case "textbf":
        case "bf":
            result = "b";
            break;
        // Small capitals
        case "textsc":
            result = "smallcaps"; // Not a proper HTML tag, but used here for convenience
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
