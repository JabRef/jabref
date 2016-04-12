package net.sf.jabref.model.entry;

import java.util.Objects;

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

    public static String addDotIfAbbreviation(String name) {
        // Avoid arrayindexoutof.... :
        if (name == null || name.isEmpty()) {
            return name;
        }
        // If only one character (uppercase letter), add a dot and return immediately:
        if ((name.length() == 1) && Character.isLetter(name.charAt(0)) &&
                Character.isUpperCase(name.charAt(0))) {
            return name + ".";
        }

        StringBuilder sb = new StringBuilder();
        char lastChar = name.charAt(0);
        for (int i = 0; i < name.length(); i++) {
            if(i > 0) {
                lastChar = name.charAt(i - 1);
            }
            char currentChar = name.charAt(i);
            sb.append(currentChar);

            if(currentChar == '.') {
                // A.A. -> A. A.
                if(i + 1 < name.length() && Character.isUpperCase(name.charAt(i + 1))) {
                    sb.append(' ');
                }
            }

            boolean currentIsUppercaseLetter = Character.isLetter(currentChar) && Character.isUpperCase(currentChar);
            if(!currentIsUppercaseLetter) {
                // No uppercase letter, hence nothing to do
                continue;
            }

            boolean lastIsLowercaseLetter = Character.isLetter(lastChar) && Character.isLowerCase(lastChar);
            if(lastIsLowercaseLetter) {
                // previous character was lowercase (probably an acronym like JabRef) -> don't change anything
                continue;
            }

            if(i + 1 >= name.length()) {
                // Current character is last character in input, so append dot
                sb.append('.');
                continue;
            }

            char nextChar = name.charAt(i + 1);
            if ('-' == nextChar) {
                // A-A -> A.-A.
                sb.append(".");
                continue;
            }
            if('.' == nextChar) {
                // Dot already there, so nothing to do
                continue;
            }

            // AA -> A. A.
            // Only append ". " if the rest of the 'word' is uppercase
            boolean nextWordIsUppercase = true;
            for (int j = i + 1; j < name.length(); j++) {
                char furtherChar = name.charAt(j);
                if(Character.isWhitespace(furtherChar) || furtherChar == '-' || furtherChar == '~' || furtherChar == '.') {
                    // end of word
                    break;
                }

                boolean furtherIsUppercaseLetter = Character.isLetter(furtherChar) && Character.isUpperCase(furtherChar);
                if(!furtherIsUppercaseLetter) {
                    nextWordIsUppercase = false;
                    break;
                }
            }
            if(nextWordIsUppercase) {
                sb.append(". ");
            }
        }

        return sb.toString().trim();
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof net.sf.jabref.model.entry.Author) {
            net.sf.jabref.model.entry.Author a = (net.sf.jabref.model.entry.Author) o;
            return Objects.equals(firstPart, a.firstPart) && Objects.equals(firstAbbr, a.firstAbbr) && Objects.equals(
                    vonPart, a.vonPart) && Objects.equals(lastPart, a.lastPart) && Objects.equals(jrPart, a.jrPart);
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
        loop:
        while (i < s.length()) {
            char c = s.charAt(i);
            switch (c) {
            case '{':
                level++;
                break;
            case '}':
                level--;
                if (level == -1) {
                    // the improper nesting
                    break loop;
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
        if (name == null) {
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
    public String getFirst() {
        return firstPart;
    }

    /**
     * Returns the abbreviated first name of the author stored in this
     * object ("F.").
     *
     * @return abbreviated first name of the author (may consist of several
     * tokens)
     */
    public String getFirstAbbr() {
        return firstAbbr;
    }

    /**
     * Returns the von part of the author's name stored in this object
     * ("von").
     *
     * @return von part of the author's name (may consist of several tokens)
     */
    public String getVon() {
        return vonPart;
    }

    /**
     * Returns the last name of the author stored in this object ("Last").
     *
     * @return last name of the author (may consist of several tokens)
     */
    public String getLast() {
        return lastPart;
    }

    /**
     * Returns the junior part of the author's name stored in this object
     * ("Jr").
     *
     * @return junior part of the author's name (may consist of several
     * tokens) or null if the author does not have a Jr. Part
     */
    public String getJr() {
        return jrPart;
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
            return lastPart == null ? "" : lastPart;
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
        if (jrPart != null) {
            res.append(", ").append(jrPart);
        }
        if (abbr) {
            if (firstAbbr != null) {
                res.append(", ").append(firstAbbr);
            }
        } else {
            if (firstPart != null) {
                res.append(", ").append(firstPart);
            }
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
            res.append(firstAbbr == null ? "" : firstAbbr + ' ').append(getLastOnly());
        } else {
            res.append(firstPart == null ? "" : firstPart + ' ').append(getLastOnly());
        }
        if (jrPart != null) {
            res.append(", ").append(jrPart);
        }
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
        if (lastPart != null) {
            res.append(lastPart);
        }
        if (jrPart != null) {
            res.append(", ");
            res.append(jrPart);
        }
        if (firstAbbr != null) {
            res.append(", ");
            res.append(firstAbbr);
        }
        while ((res.length() > 0) && (res.charAt(0) == '{')) {
            res.deleteCharAt(0);
        }
        return res.toString();
    }
}
