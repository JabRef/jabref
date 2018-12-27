package org.jabref.logic.bst;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;

/**
 * From Bibtex:
 *
 * "The |built_in| function {\.{format.name\$}} pops the
 * top three literals (they are a string, an integer, and a string
 * literal, in that order). The last string literal represents a
 * name list (each name corresponding to a person), the integer
 * literal specifies which name to pick from this list, and the
 * first string literal specifies how to format this name, as
 * described in the \BibTeX\ documentation. Finally, this function
 * pushes the formatted name. If any of the types is incorrect, it
 * complains and pushes the null string."
 *
 * Sounds easy - is a nightmare... X-(
 *
 */
public class BibtexNameFormatter {

    private BibtexNameFormatter() {
    }

    /**
     * Formats the nth author of the author name list by a given format string
     *
     * @param authorsNameList The string from an author field
     * @param whichName index of the list, starting with 1
     * @param formatString TODO
     * @param warn collects the warnings, may-be-null
     * @return
     */
    public static String formatName(String authorsNameList, int whichName, String formatString, Warn warn) {
        AuthorList al = AuthorList.parse(authorsNameList);

        if ((whichName < 1) && (whichName > al.getNumberOfAuthors())) {
            warn.warn("AuthorList " + authorsNameList + " does not contain an author with number " + whichName);
            return "";
        }
        return BibtexNameFormatter.formatName(al.getAuthor(whichName - 1), formatString, warn);
    }

    /**
     * @param warn collects the warnings, may-be-null
     */
    public static String formatName(Author author, String format, Warn warn) {

        StringBuilder sb = new StringBuilder();

        char[] c = format.toCharArray();
        int n = c.length;
        int braceLevel = 0;
        int group = 0;

        int i = 0;
        while (i < n) {
            if (c[i] == '{') {
                group++;
                i++;
                braceLevel++;
                StringBuilder level1Chars = new StringBuilder();
                StringBuilder wholeChar = new StringBuilder();
                while ((i < n) && (braceLevel > 0)) {
                    wholeChar.append(c[i]);
                    if (c[i] == '{') {
                        braceLevel++;
                        i++;
                        continue;
                    }
                    if (c[i] == '}') {
                        braceLevel--;
                        i++;
                        continue;
                    }
                    if ((braceLevel == 1) && Character.isLetter(c[i])) {
                        if ("fvlj".indexOf(c[i]) == -1) {
                            if (warn != null) {
                                warn.warn(
                                        "Format string in format.name$ may only contain fvlj on brace level 1 in group "
                                                + group + ": " + format);
                            }
                        } else {
                            level1Chars.append(c[i]);
                        }
                    }
                    i++;
                }
                i--; // unskip last brace (for last i++ at the end)
                String control = level1Chars.toString().toLowerCase(Locale.ROOT);

                if (control.isEmpty()) {
                    continue;
                }

                if ((control.length() > 2) && (warn != null)) {
                    warn.warn("Format string in format.name$ may only be one or two character long on brace level 1 in group " + group + ": " + format);
                }

                char type = control.charAt(0);

                Optional<String> tokenS;
                switch (type) {
                case 'f':
                    tokenS = author.getFirst();
                    break;
                case 'v':
                    tokenS = author.getVon();
                    break;
                case 'l':
                    tokenS = author.getLast();
                    break;
                case 'j':
                    tokenS = author.getJr();
                    break;
                default:
                    throw new VMException("Internal error");
                }

                if (!tokenS.isPresent()) {
                    i++;
                    continue;
                }
                String[] tokens = tokenS.get().split(" ");

                boolean abbreviateThatIsSingleLetter = true;

                if (control.length() == 2) {
                    if (control.charAt(1) == control.charAt(0)) {
                        abbreviateThatIsSingleLetter = false;
                    } else {
                        if (warn != null) {
                            warn.warn("Format string in format.name$ may only contain one type of vlfj on brace level 1 in group " + group + ": " + format);
                        }
                    }
                }

                // Now we know what to do

                if ((braceLevel == 0) && (wholeChar.charAt(wholeChar.length() - 1) == '}')) {
                    wholeChar.deleteCharAt(wholeChar.length() - 1);
                }

                char[] d = wholeChar.toString().toCharArray();

                int bLevel = 1;

                String interToken = null;
                int groupStart = sb.length();

                for (int j = 0; j < d.length; j++) {

                    if (Character.isLetter(d[j]) && (bLevel == 1)) {
                        groupStart = sb.length();
                        if (!abbreviateThatIsSingleLetter) {
                            j++;
                        }
                        if (((j + 1) < d.length) && (d[j + 1] == '{')) {
                            StringBuilder interTokenSb = new StringBuilder();
                            j = BibtexNameFormatter.consumeToMatchingBrace(interTokenSb, d, j + 1);
                            interToken = interTokenSb.substring(1, interTokenSb.length() - 1);
                        }

                        for (int k = 0; k < tokens.length; k++) {
                            String token = tokens[k];
                            if (abbreviateThatIsSingleLetter) {
                                String[] dashes = token.split("-");

                                token = Arrays.asList(dashes).stream().map(BibtexNameFormatter::getFirstCharOfString)
                                        .collect(Collectors.joining(".-"));
                            }

                            // Output token
                            sb.append(token);

                            if (k < (tokens.length - 1)) {
                                // Output Intertoken String
                                if (interToken == null) {
                                    if (abbreviateThatIsSingleLetter) {
                                        sb.append('.');
                                    }
                                    // No clue what this means (What the hell are tokens anyway???
                                    // if (lex_class[name_sep_char[cur_token]] = sep_char) then
                                    //    append_ex_buf_char_and_check (name_sep_char[cur_token])
                                    if ((k == (tokens.length - 2)) || (BibtexNameFormatter.numberOfChars(sb.substring(groupStart, sb.length()), 3) < 3)) {
                                        sb.append('~');
                                    } else {
                                        sb.append(' ');
                                    }
                                } else {
                                    sb.append(interToken);
                                }
                            }
                        }
                    } else if (d[j] == '}') {
                        bLevel--;
                        if (bLevel > 0) {
                            sb.append('}');
                        }
                    } else if (d[j] == '{') {
                        bLevel++;
                        sb.append('{');
                    } else {
                        sb.append(d[j]);
                    }
                }
                if (sb.length() > 0) {
                    boolean noDisTie = false;
                    if ((sb.charAt(sb.length() - 1) == '~') &&
                            ((BibtexNameFormatter.numberOfChars(sb.substring(groupStart, sb.length()), 4) >= 4) ||
                            ((sb.length() > 1) && (noDisTie = sb.charAt(sb.length() - 2) == '~')))) {
                        sb.deleteCharAt(sb.length() - 1);
                        if (!noDisTie) {
                            sb.append(' ');
                        }
                    }
                }
            } else if (c[i] == '}') {
                if (warn != null) {
                    warn.warn("Unmatched brace in format string: " + format);
                }
            } else {
                sb.append(c[i]); // verbatim
            }
            i++;
        }
        if ((braceLevel != 0) && (warn != null)) {
            warn.warn("Unbalanced brace in format string for nameFormat: " + format);
        }

        return sb.toString();
    }

    /**
     * Including the matching brace.
     *
     * @param interTokenSb
     * @param c
     * @param pos
     * @return
     */
    public static int consumeToMatchingBrace(StringBuilder interTokenSb, char[] c, int pos) {

        int braceLevel = 0;

        for (int i = pos; i < c.length; i++) {
            if (c[i] == '}') {
                braceLevel--;
                if (braceLevel == 0) {
                    interTokenSb.append('}');
                    return i;
                }
            } else if (c[i] == '{') {
                braceLevel++;
            }
            interTokenSb.append(c[i]);
        }
        return c.length;
    }

    /**
     * Takes care of special characters too
     *
     * @param s
     * @return
     */
    public static String getFirstCharOfString(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (Character.isLetter(c[i])) {
                return String.valueOf(c[i]);
            }
            if ((c[i] == '{') && ((i + 1) < c.length) && (c[i + 1] == '\\')) {
                StringBuilder sb = new StringBuilder();
                BibtexNameFormatter.consumeToMatchingBrace(sb, c, i);
                return sb.toString();
            }
        }
        return "";
    }

    public static int numberOfChars(String token, int inStop) {
        int stop = inStop;
        if (stop < 0) {
            stop = Integer.MAX_VALUE;
        }

        int result = 0;
        int i = 0;
        char[] c = token.toCharArray();
        int n = c.length;

        int braceLevel = 0;
        while ((i < n) && (result < stop)) {
            i++;
            if (c[i - 1] == '{') {
                braceLevel++;
                if ((braceLevel == 1) && (i < n) && (c[i] == '\\')) {
                    i++;
                    while ((i < n) && (braceLevel > 0)) {
                        if (c[i] == '}') {
                            braceLevel--;
                        } else if (c[i] == '{') {
                            braceLevel++;
                        }
                        i++;
                    }
                }
            } else if (c[i - 1] == '}') {
                braceLevel--;
            }
            result++;
        }
        return result;
    }

}
