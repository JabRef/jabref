package net.sf.jabref.model.strings;

import java.util.Map;
import java.util.regex.Pattern;

public class LatexToUnicode {

    private static final Map<String, String> CHARS = HTMLUnicodeConversionMaps.LATEX_UNICODE_CONVERSION_MAP;
    private static final Map<String, String> ACCENTS = HTMLUnicodeConversionMaps.UNICODE_ESCAPED_ACCENTS;

    private static final Pattern AMP_LATEX = Pattern.compile("&|\\\\&");
    private static final Pattern P_LATEX = Pattern.compile("[\\n]{1,}");
    private static final Pattern DOLLAR_LATEX = Pattern.compile("\\\\\\$");
    private static final Pattern DOLLARS_LATEX = Pattern.compile("\\$([^\\$]*)\\$");

    private static final Pattern AMP = Pattern.compile("\\&amp;");
    private static final Pattern P = Pattern.compile("<p>");
    private static final Pattern DOLLAR = Pattern.compile("\\&dollar;");
    private static final Pattern TILDE = Pattern.compile("~");

    public String format(String inField) {
        if (inField.isEmpty()) {
            return "";
        }
        int i;
        // TODO: document what does this do
        String field = AMP_LATEX.matcher(inField).replaceAll("&amp;");
        field = P_LATEX.matcher(field).replaceAll("<p>");
        field = DOLLAR_LATEX.matcher(field).replaceAll("&dollar;");
        field = DOLLARS_LATEX.matcher(field).replaceAll("\\{$1\\}");

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
                    String result = CHARS.get(command);
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
                // Swallow the brace.
            } else if (Character.isLetter(c) || (c == '%')
                    || StringUtil.SPECIAL_COMMAND_CHARS.contains(String.valueOf(c))) {
                escaped = false;

                if (!incommand) {
                    sb.append(c);
                } else {
                    currentCommand.append(c);
                    if ((currentCommand.length() == 1)
                            && StringUtil.SPECIAL_COMMAND_CHARS.contains(currentCommand.toString())
                            && !(i >= (field.length() - 1))) {
                        // This indicates that we are in a command of the type
                        // \^o or \~{n}

                        String command = currentCommand.toString();
                        i++;
                        c = field.charAt(i);
                        String commandBody;
                        if (c == '{') {
                            String part = StringUtil.getPart(field, i, false);
                            i += part.length();
                            commandBody = part;
                        } else {
                            commandBody = field.substring(i, i + 1);
                        }
                        String result = CHARS.get(command + commandBody);

                        if (result == null) {
                            // Use combining accents if argument is single character or empty
                            if (commandBody.length() <= 1) {
                                String accent = ACCENTS.get(command);
                                if (accent == null) {
                                    // Shouldn't happen
                                    sb.append(commandBody);
                                } else {
                                    sb.append(commandBody).append(accent);
                                }
                            }
                        } else {
                            sb.append(result);
                        }

                        incommand = false;
                        escaped = false;
                    } else {
                        //  Are we already at the end of the string?
                        if ((i + 1) == field.length()) {
                            String command = currentCommand.toString();
                            String result = CHARS.get(command);
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
                if (!incommand) {
                    sb.append(c);
                } else if (Character.isWhitespace(c) || (c == '{') || (c == '}')) {
                    // First test if we are already at the end of the string.
                    // if (i >= field.length()-1)
                    // break testContent;

                    String command = currentCommand.toString();

                    if (c == '{') {
                        String argument = StringUtil.getPart(field, i, true);
                        i += argument.length();
                        // handle common case of general latex command
                        String result = CHARS.get(command + argument);

                        // If found, then use translated version. If not, then keep
                        // the
                        // text of the parameter intact.
                        if (result == null) {
                            // Use combining accents if argument is single character or empty
                            if (argument.length() <= 1) {
                                String accent = ACCENTS.get(command);
                                if (accent == null) {
                                    if (argument.isEmpty()) {
                                        // Empty argument, may be used as separator as in \LaTeX{}, so keep the command
                                        sb.append(command);
                                    } else {
                                        sb.append(argument);
                                    }
                                } else {
                                    sb.append(argument).append(accent);
                                }
                            } else {
                                sb.append(argument);
                            }
                        } else {
                            sb.append(result);
                        }
                    } else if (c == '}') {
                        // This end brace terminates a command. This can be the case in
                        // constructs like {\aa}. The correct behaviour should be to
                        // substitute the evaluated command and swallow the brace:
                        String result = CHARS.get(command);

                        if (result == null) {
                            // If the command is unknown, just print it:
                            sb.append(command);
                        } else {
                            sb.append(result);
                        }

                    } else {
                        String result = CHARS.get(command);

                        if (result == null) {
                            sb.append(command);
                        } else {
                            sb.append(result);
                        }
                        sb.append(' ');
                    }
                } else {
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

        String result = AMP.matcher(sb.toString()).replaceAll("&");
        result = P.matcher(result).replaceAll("\n");
        result = DOLLAR.matcher(result).replaceAll("\\$");
        result = TILDE.matcher(result).replaceAll("\u00A0");
        return result;

    }
}
