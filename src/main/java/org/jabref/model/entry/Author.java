package org.jabref.model.entry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.strings.StringUtil;

/**
 * This is an immutable class that keeps information regarding single
 * author. It is just a container for the information, with very simple
 * methods to access it.
 * <p>
 * Current usage: only methods <code>getLastOnly</code>,
 * <code>getFirstLast</code>, and <code>getLastFirst</code> are used;
 * all other methods are provided for completeness.
 */
public class Author {
    static boolean[] visited = new boolean[33];

    private final String firstPart;

    private final String firstAbbr;

    private final String vonPart;

    private final String lastPart;

    private final String jrPart;

    /**
     * Creates the Author object. If any part of the name is absent, <CODE>null</CODE>
     * must be passed; otherwise other methods may return erroneous results.
     *
     * @param first     the first name of the author (may consist of several
     *                  tokens, like "Charles Louis Xavier Joseph" in "Charles
     *                  Louis Xavier Joseph de la Vall{\'e}e Poussin")
     * @param firstabbr the abbreviated first name of the author (may consist of
     *                  several tokens, like "C. L. X. J." in "Charles Louis
     *                  Xavier Joseph de la Vall{\'e}e Poussin"). It is a
     *                  responsibility of the caller to create a reasonable
     *                  abbreviation of the first name.
     * @param von       the von part of the author's name (may consist of several
     *                  tokens, like "de la" in "Charles Louis Xavier Joseph de la
     *                  Vall{\'e}e Poussin")
     * @param last      the last name of the author (may consist of several
     *                  tokens, like "Vall{\'e}e Poussin" in "Charles Louis Xavier
     *                  Joseph de la Vall{\'e}e Poussin")
     * @param jr        the junior part of the author's name (may consist of
     *                  several tokens, like "Jr. III" in "Smith, Jr. III, John")
     */
    public Author(String first, String firstabbr, String von, String last, String jr) {
        firstPart = addDotIfAbbreviation(removeStartAndEndBraces(first));
        firstAbbr = removeStartAndEndBraces(firstabbr);
        vonPart = removeStartAndEndBraces(von);
        lastPart = removeStartAndEndBraces(last);
        jrPart = removeStartAndEndBraces(jr);
    }

    @SuppressWarnings("checkstyle:WhitespaceAround")
    public static String addDotIfAbbreviation(String name) {
        if ((name == null) || name.isEmpty()) {
            visited[0] = true;
            getBranchCoverage(visited);
            return name;
        } else {
            visited[1] = true;
        }
        // If only one character (uppercase letter), add a dot and return immediately:
        if ((name.length() == 1) && Character.isLetter(name.charAt(0)) &&
                Character.isUpperCase(name.charAt(0))) {
            visited[2] = true;
            getBranchCoverage(visited);
            return name + ".";
        } else {
            visited[3] = true;
        }

        StringBuilder sb = new StringBuilder();
        char lastChar = name.charAt(0);
        for (int i = 0; i < name.length(); i++) {
            visited[4] = true;
            if (i > 0) {
                visited[5] = true;
                lastChar = name.charAt(i - 1);
            } else {
                visited[6] = true;
            }
            char currentChar = name.charAt(i);
            sb.append(currentChar);

            if (currentChar == '.') {
                visited[7] = true;
                // A.A. -> A. A.
                if (((i + 1) < name.length()) && Character.isUpperCase(name.charAt(i + 1))) {
                    visited[8] = true;
                    sb.append(' ');
                }
                else {
                    visited[9] = true;
                }
            } else {
                visited[10] = true;
            }

            boolean currentIsUppercaseLetter = Character.isLetter(currentChar) && Character.isUpperCase(currentChar);
            if (!currentIsUppercaseLetter) {
                visited[11] = true;
                // No uppercase letter, hence nothing to do
                continue;
            } else {
                visited[12] = true;
            }

            boolean lastIsLowercaseLetter = Character.isLetter(lastChar) && Character.isLowerCase(lastChar);
            if (lastIsLowercaseLetter) {
                visited[13] = true;
                // previous character was lowercase (probably an acronym like JabRef) -> don't change anything
                continue;
            } else {
                visited[14] = true;
            }

            if ((i + 1) >= name.length()) {
                visited[15] = true;
                // Current character is last character in input, so append dot
                sb.append('.');
                continue;
            } else {
                visited[16] = true;
            }

            char nextChar = name.charAt(i + 1);
            if ('-' == nextChar) {
                visited[17] = true;
                // A-A -> A.-A.
                sb.append(".");
                continue;
            } else {
                visited[18] = true;
            }
            if ('.' == nextChar) {
                visited[19] = true;
                // Dot already there, so nothing to do
                continue;
            } else {
                visited[20] = true;
            }

            // AA -> A. A.
            // Only append ". " if the rest of the 'word' is uppercase
            boolean nextWordIsUppercase = true;
            char furtherChar = Character.MIN_VALUE;
            for (int j = i + 1; j < name.length(); j++) {
                visited[21] = true;
                furtherChar = name.charAt(j);
                if (Character.isWhitespace(furtherChar) || (furtherChar == '-') || (furtherChar == '~') || (furtherChar == '.')) {
                    visited[22] = true;
                    // end of word
                    break;
                } else {
                    visited[23] = true;
                }

                boolean furtherIsUppercaseLetter = Character.isLetter(furtherChar) && Character.isUpperCase(furtherChar);
                if (!furtherIsUppercaseLetter) {
                    visited[24] = true;
                    nextWordIsUppercase = false;
                    break;
                } else {
                    visited[25] = true;
                }

            }
            if (!(name.length() > i + 1)) {
                visited[31] = true;
            }
            if (nextWordIsUppercase) {
                visited[26] = true;
                if (Character.isWhitespace(furtherChar)) {
                    visited[27] = true;
                    sb.append(".");
                } else {
                    visited[28] = true;
                    sb.append(". ");
                }
            } else {
                visited[29] = true;
            }
        }
        if (name.length() == 0) {
            visited[32] = true;
        }
        getBranchCoverage(visited);
        return sb.toString().trim();
    }

    private static void getBranchCoverage(boolean[] visited) {
        try {
            File directory = new File("/Temp");
            if (!directory.exists()) {
                directory.mkdir();
            }
            File f = new File(directory + "/addDotIfAbbreviation.txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            double frac = 0;
            for (int i = 0; i < visited.length; ++i) {
                frac += (visited[i] ? 1 : 0);
                bw.write("branch " + i + " was" + (visited[i] ? " visited." : " not visited.") + "\n");
            }
            bw.write("" + frac / visited.length);
            bw.close();
        } catch (Exception e) {
            System.err.println("ye");
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstAbbr, firstPart, jrPart, lastPart, vonPart);
    }

    /**
     * Compare this object with the given one.
     * <p>
     * Will return true iff the other object is an Author and all fields are identical on a string comparison.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof Author) {
            Author that = (Author) other;
            return Objects.equals(firstPart, that.firstPart) && Objects.equals(firstAbbr, that.firstAbbr)
                    && Objects.equals(
                            vonPart, that.vonPart)
                    && Objects.equals(lastPart, that.lastPart) && Objects.equals(jrPart, that.jrPart);
        }
        return false;
    }

    /**
     * @return true if the brackets in s are properly paired
     */
    private boolean properBrackets(String s) {
        // nested construct is there, check for "proper" nesting
        int i = 0;
        int level = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            switch (c) {
            case '{':
                level++;
                break;
            case '}':
                level--;
                if (level == -1) { // improper nesting
                    return false;
                }
                break;
            default:
                break;
            }
            i++;
        }
        return level == 0;
    }

    /**
     * Removes start and end brace at a string
     * <p>
     * E.g.,
     * * {Vall{\'e}e Poussin} -> Vall{\'e}e Poussin
     * * {Vall{\'e}e} {Poussin} -> Vall{\'e}e Poussin
     * * Vall{\'e}e Poussin -> Vall{\'e}e Poussin
     */
    private String removeStartAndEndBraces(String name) {
        if (StringUtil.isBlank(name)) {
            return null;
        }

        if (!name.contains("{")) {
            return name;
        }

        String[] split = name.split(" ");
        StringBuilder b = new StringBuilder();
        for (String s : split) {
            if ((s.length() > 2) && s.startsWith("{") && s.endsWith("}")) {
                // quick solution (which we don't do: just remove first "{" and last "}"
                // however, it might be that s is like {A}bbb{c}, where braces may not be removed

                // inner
                String inner = s.substring(1, s.length() - 1);

                if (inner.contains("}")) {
                    if (properBrackets(inner)) {
                        s = inner;
                    }
                } else {
                    //  no inner curly brackets found, no check needed, inner can just be used as s
                    s = inner;
                }
            }
            b.append(s).append(' ');
        }
        // delete last
        b.deleteCharAt(b.length() - 1);

        // now, all inner words are cleared
        // case {word word word} remains
        // as above, we have to be aware of {w}ord word wor{d} and {{w}ord word word}

        String newName = b.toString();

        if (newName.startsWith("{") && newName.endsWith("}")) {
            String inner = newName.substring(1, newName.length() - 1);
            if (properBrackets(inner)) {
                return inner;
            } else {
                return newName;
            }
        } else {
            return newName;
        }
    }

    /**
     * Returns the first name of the author stored in this object ("First").
     *
     * @return first name of the author (may consist of several tokens)
     */
    public Optional<String> getFirst() {
        return Optional.ofNullable(firstPart);
    }

    /**
     * Returns the abbreviated first name of the author stored in this
     * object ("F.").
     *
     * @return abbreviated first name of the author (may consist of several
     * tokens)
     */
    public Optional<String> getFirstAbbr() {
        return Optional.ofNullable(firstAbbr);
    }

    /**
     * Returns the von part of the author's name stored in this object
     * ("von").
     *
     * @return von part of the author's name (may consist of several tokens)
     */
    public Optional<String> getVon() {
        return Optional.ofNullable(vonPart);
    }

    /**
     * Returns the last name of the author stored in this object ("Last").
     *
     * @return last name of the author (may consist of several tokens)
     */
    public Optional<String> getLast() {
        return Optional.ofNullable(lastPart);
    }

    /**
     * Returns the junior part of the author's name stored in this object
     * ("Jr").
     *
     * @return junior part of the author's name (may consist of several
     * tokens) or null if the author does not have a Jr. Part
     */
    public Optional<String> getJr() {
        return Optional.ofNullable(jrPart);
    }

    /**
     * Returns von-part followed by last name ("von Last"). If both fields
     * were specified as <CODE>null</CODE>, the empty string <CODE>""</CODE>
     * is returned.
     *
     * @return 'von Last'
     */
    public String getLastOnly() {
        if (vonPart == null) {
            return getLast().orElse("");
        } else {
            return lastPart == null ? vonPart : vonPart + ' ' + lastPart;
        }
    }

    /**
     * Returns the author's name in form 'von Last, Jr., First' with the
     * first name full or abbreviated depending on parameter.
     *
     * @param abbr <CODE>true</CODE> - abbreviate first name, <CODE>false</CODE> -
     *             do not abbreviate
     * @return 'von Last, Jr., First' (if <CODE>abbr==false</CODE>) or
     * 'von Last, Jr., F.' (if <CODE>abbr==true</CODE>)
     */
    public String getLastFirst(boolean abbr) {
        StringBuilder res = new StringBuilder(getLastOnly());
        getJr().ifPresent(jr -> res.append(", ").append(jr));
        if (abbr) {
            getFirstAbbr().ifPresent(firstA -> res.append(", ").append(firstA));
        } else {
            getFirst().ifPresent(first -> res.append(", ").append(first));
        }
        return res.toString();
    }

    /**
     * Returns the author's name in form 'First von Last, Jr.' with the
     * first name full or abbreviated depending on parameter.
     *
     * @param abbr <CODE>true</CODE> - abbreviate first name, <CODE>false</CODE> -
     *             do not abbreviate
     * @return 'First von Last, Jr.' (if <CODE>abbr==false</CODE>) or 'F.
     * von Last, Jr.' (if <CODE>abbr==true</CODE>)
     */
    public String getFirstLast(boolean abbr) {
        StringBuilder res = new StringBuilder();
        if (abbr) {
            getFirstAbbr().map(firstA -> firstA + ' ').ifPresent(res::append);
        } else {
            getFirst().map(first -> first + ' ').ifPresent(res::append);
        }
        res.append(getLastOnly());
        getJr().ifPresent(jr -> res.append(", ").append(jr));
        return res.toString();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Author{");
        sb.append("firstPart='").append(firstPart).append('\'');
        sb.append(", firstAbbr='").append(firstAbbr).append('\'');
        sb.append(", vonPart='").append(vonPart).append('\'');
        sb.append(", lastPart='").append(lastPart).append('\'');
        sb.append(", jrPart='").append(jrPart).append('\'');
        sb.append('}');
        return sb.toString();
    }

    /**
     * Returns the name as "Last, Jr, F." omitting the von-part and removing
     * starting braces.
     *
     * @return "Last, Jr, F." as described above or "" if all these parts
     * are empty.
     */
    public String getNameForAlphabetization() {
        StringBuilder res = new StringBuilder();
        getLast().ifPresent(res::append);
        getJr().ifPresent(jr -> res.append(", ").append(jr));
        getFirstAbbr().ifPresent(firstA -> res.append(", ").append(firstA));
        while ((res.length() > 0) && (res.charAt(0) == '{')) {
            res.deleteCharAt(0);
        }
        return res.toString();
    }
}
