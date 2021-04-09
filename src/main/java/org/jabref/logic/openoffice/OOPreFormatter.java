package org.jabref.logic.openoffice;

import java.util.Map;
import java.util.Objects;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.util.strings.HTMLUnicodeConversionMaps;
import org.jabref.model.strings.StringUtil;

/**
 * This formatter preprocesses JabRef fields before they are run through the layout of the
 * bibliography style. It handles translation of LaTeX italic/bold commands into HTML tags.
 *
 * This is very similar to org.jabref.logic.layout.format.HTMLChars
 *
 * Escapes: \\ becomes \
 * \ starts command
 * '{' and '}' are dropped, unless {@code incommand}
 */
public class OOPreFormatter implements LayoutFormatter {

    private static final Map<String, String> CHARS =
        HTMLUnicodeConversionMaps.LATEX_UNICODE_CONVERSION_MAP;

    @Override
    public String format(String inField) {
        int i;
        String field = (inField
                        .replaceAll("&|\\\\&", "&") // Replace & and \& with &
                        // Replace \$ with &dollar;
                        // Question: why not replaceAll?
                        // Question: we are using LATEX_UNICODE_CONVERSION_MAP,
                        //           but insert HTML entity here. Why?
                        //           Before return, &dollar; is changed to "$".
                        //           We probably just want to avoid matches to the
                        //           next replaceAll.
                        .replace("\\$", "&dollar;")
                        // Replace $...$ with {...} to simplify conversion
                        // Question: is $$ impossible here?
                        // Question: is the replacement {...} or \{...\} ?
                        .replaceAll("\\$([^$]*)\\$", "\\{$1\\}")
                        /* Is there a reason we do not change "&dollar;" to "$" here? */);

        StringBuilder sb = new StringBuilder();
        StringBuilder currentCommand = null;

        char c;
        boolean escaped = false;
        boolean incommand = false;

        for (i = 0; i < field.length(); i++) {
            c = field.charAt(i);
            if (escaped && (c == '\\')) {
                // escaped backslash
                sb.append('\\');
                escaped = false;
            } else if (c == '\\') {
                // unescaped backslash
                if (incommand) {
                    /* Close Command */
                    String command = currentCommand.toString();
                    String result = OOPreFormatter.CHARS.get(command);
                    sb.append(Objects.requireNonNullElse(result, command));
                }
                escaped = true;
                incommand = true;
                currentCommand = new StringBuilder();
            } else if (!incommand && ((c == '{') || (c == '}'))) {
                // Swallow braces, necessary for replacing encoded characters
            } else if (Character.isLetter(c)
                       || (c == '%')
                       || StringUtil.SPECIAL_COMMAND_CHARS.contains(String.valueOf(c))) {
                // SPECIAL_COMMAND_CHARS = "\"`^~'=.|";
                escaped = false;

                if (!incommand) {
                    sb.append(c);
                } else {
                    currentCommand.append(c);
                    testCharCom:
                    if ((currentCommand.length() == 1)
                        && StringUtil.SPECIAL_COMMAND_CHARS.contains(currentCommand.toString())) {
                        // SPECIAL_COMMAND_CHARS: \" \` \^ \~ \' \= \. \|

                        // This indicates that we are in a command of the type
                        // \^o or \~{n} or \'{\i}

                        // These all require at least one more character.
                        if (i >= (field.length() - 1)) {
                            break testCharCom;
                        }

                        String command = currentCommand.toString();

                        i++;
                        c = field.charAt(i);
                        String commandBody;
                        if (c == '{') {
                            String part = StringUtil.getPart(field,
                                                             i,
                                                             false /* terminateOnEndBraceOnly */);
                            // For "{abc def}" part is "abc" now, not "abc def".
                            i += part.length();
                            commandBody = part;
                        } else {
                            // No "{}", take a single character.
                            commandBody = field.substring(i, i + 1);
                        }

                        String result = OOPreFormatter.CHARS.get(command + commandBody);

                        // if (result != null) {
                        //     sb.append(result);
                        // }
                        // Behave as HTMLChars.format: if the command is unknown,
                        // keep the body.
                        sb.append(Objects.requireNonNullElse(result, commandBody));

                        incommand = false;
                        escaped = false;
                    } else {
                        // incommand, not single-letter SPECIAL_COMMAND_CHAR
                        // Are we already at the end of the string?
                        if ((i + 1) == field.length()) {
                            String command = currentCommand.toString();
                            String result = OOPreFormatter.CHARS.get(command);
                            /* If found, then use translated version. If not,
                             * then keep
                             * the text of the parameter intact.
                             */
                            sb.append(Objects.requireNonNullElse(result, command));
                            // Consumed currentCommand, yet it is not reset. We are at the end anyway.
                        }
                        // else fall through
                    }
                    // "break testCharCom;" jumps here
                }
            } else {
                // Not backslash, not (brace && !incommand), not letter, not %, not SPECIAL_COMMAND_CHAR

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
                        sb.append('<').append(tag).append('>')
                          .append(part)
                          .append("</").append(tag).append('>');
                    } else if (c == '{') {
                        String argument = StringUtil.getPart(field, i, true);
                        i += argument.length();
                        // handle common case of general latex command
                        String result = OOPreFormatter.CHARS.get(command + argument);
                        // If found, then use translated version. If not, then keep
                        // the text of the parameter intact.

                        if (result == null) {
                            if (argument.isEmpty()) {
                                // Maybe a separator, such as in \LaTeX{}, so use command
                                sb.append(command);
                            } else {
                                // Otherwise, use argument
                                sb.append(argument);
                            }
                        } else {
                            sb.append(result);
                        }

                    } else if (c == '}') {
                        // This end brace terminates a command. This can be the case in
                        // constructs like {\aa}. The correct behaviour should be to
                        // substitute the evaluated command and swallow the brace:
                        String result = OOPreFormatter.CHARS.get(command);
                        // If the command is unknown, just print it:
                        sb.append(Objects.requireNonNullElse(result, command));
                    } else {
                        String result = OOPreFormatter.CHARS.get(command);
                        sb.append(Objects.requireNonNullElse(result, command));
                        sb.append(' ');
                    }
                } else if (c == '}') {
                    // Note: this branch is not present in HTMLChars.format
                    // System.out.printf("com term by }: '%s'\n", currentCommand.toString());
                    // argument = "";
                } else {
                    sb.append(c);
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
                // Should really separate between emphasized and
                // italic but since in later stages both are converted
                // to italic...
            case "emph":
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
            // Strikeout, sout is the "standard" command, although it
            // is actually based on the package ulem
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
