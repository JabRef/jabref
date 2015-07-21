package net.sf.jabref.util;

import net.sf.jabref.Globals;

public class StringUtil {

    /**
     * Returns the string, after shaving off whitespace at the beginning and end,
     * and removing (at most) one pair of braces or " surrounding it.
     *
     * @param s
     * @return
     */
    public static String shaveString(String s) {

        if (s == null) {
            return null;
        }
        char ch, ch2;
        int beg = 0, end = s.length();
        // We start out assuming nothing will be removed.
        boolean begok = false, endok = false;
        while (!begok) {
            if (beg < s.length()) {
                ch = s.charAt(beg);
                if (Character.isWhitespace(ch)) {
                    beg++;
                } else {
                    begok = true;
                }
            } else {
                begok = true;
            }

        }
        while (!endok) {
            if (end > (beg + 1)) {
                ch = s.charAt(end - 1);
                if (Character.isWhitespace(ch)) {
                    end--;
                } else {
                    endok = true;
                }
            } else {
                endok = true;
            }
        }

        if (end > (beg + 1)) {
            ch = s.charAt(beg);
            ch2 = s.charAt(end - 1);
            if (((ch == '{') && (ch2 == '}')) || ((ch == '"') && (ch2 == '"'))) {
                beg++;
                end--;
            }
        }
        s = s.substring(beg, end);
        return s;
    }

    private static String rtrim(String s) {
        return s.replaceAll("\\s+$", "");
    }

    /**
     * Concatenate all strings in the array from index 'from' to 'to' (excluding
     * to) with the given separator.
     *
     * Example:
     *
     * String[] s = "ab/cd/ed".split("/"); join(s, "\\", 0, s.length) ->
     * "ab\\cd\\ed"
     *
     * @param strings
     * @param separator
     * @param from
     * @param to
     *            Excluding strings[to]
     * @return
     */
    public static String join(String[] strings, String separator, int from, int to) {
        if ((strings.length == 0) || (from >= to)) {
            return "";
        }

        from = Math.max(from, 0);
        to = Math.min(strings.length, to);

        StringBuilder sb = new StringBuilder();
        for (int i = from; i < (to - 1); i++) {
            sb.append(strings[i]).append(separator);
        }
        return sb.append(strings[to - 1]).toString();
    }

    public static String join(String[] strings, String separator) {
        return join(strings, separator, 0, strings.length);
    }

    /**
     * Returns the given string but with the first character turned into an
     * upper case character.
     *
     * Example: testTest becomes TestTest
     *
     * @param string
     *            The string to change the first character to upper case to.
     * @return A string has the first character turned to upper case and the
     *         rest unchanged from the given one.
     */
    public static String toUpperFirstLetter(String string) {
        if (string == null) {
            throw new IllegalArgumentException();
        }

        if (string.length() == 0) {
            return string;
        }

        return Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }

    /**
     * Takes a delimited string, splits it and returns
     *
     * @param names
     *            a <code>String</code> value
     * @return a <code>String[]</code> value
     */
    public static String[] split(String names, String delimiter) {
        if (names == null) {
            return null;
        }
        return names.split(delimiter);
    }

    public static String nCase(String s) {
        // Make first character of String uppercase, and the
        // rest lowercase.
        if (s.length() > 1) {
            return s.substring(0, 1).toUpperCase() + s.substring(1, s.length()).toLowerCase();
        } else {
            return s.toUpperCase();
        }

    }

    /**
     * Removes optional square brackets from the string s
     *
     * @param s
     * @return
     */
    public static String stripBrackets(String s) {
        int beginIndex = (s.startsWith("[") ? 1 : 0);
        int endIndex = (s.endsWith("]") ? s.length() - 1 : s.length());
        return s.substring(beginIndex, endIndex);
    }

    /**
     * extends the filename with a default Extension, if no Extension '.x' could
     * be found
     */
    public static String getCorrectFileName(String orgName, String defaultExtension) {
        if (orgName == null) {
            return "";
        }

        String back = orgName;
        int t = orgName.indexOf(".", 1); // hidden files Linux/Unix (?)
        if (t < 1) {
            back = back + "." + defaultExtension;
        }

        return back;
    }

    /**
     * Creates a substring from a text
     *
     * @param text
     * @param i
     * @param terminateOnEndBraceOnly
     * @return
     */
    public static String getPart(String text, int i, boolean terminateOnEndBraceOnly) {
        char c;
        int count = 0;

        StringBuilder part = new StringBuilder();

        // advance to first char and skip whitespace
        i++;
        while ((i < text.length()) && Character.isWhitespace(text.charAt(i))) {
            i++;
        }

        // then grab whathever is the first token (counting braces)
        while (i < text.length()) {
            c = text.charAt(i);
            if (!terminateOnEndBraceOnly && (count == 0) && Character.isWhitespace(c)) {
                // end argument and leave whitespace for further processing
                break;
            }
            if ((c == '}') && (--count < 0)) {
                break;
            } else if (c == '{') {
                count++;
            }
            part.append(c);
            i++;
        }
        return part.toString();
    }

    /**
     * Formats field contents for output. Must be "symmetric" with the parse method above,
     * so stored and reloaded fields are not mangled.
     * @param in
     * @param wrapAmount
     * @return the wrapped String.
     */
    public static String wrap(String in, int wrapAmount) {

        String[] lines = in.split("\n");
        StringBuffer res = new StringBuffer();
        addWrappedLine(res, lines[0], wrapAmount);
        for (int i = 1; i < lines.length; i++) {

            if (!lines[i].trim().equals("")) {
                res.append(Globals.NEWLINE);
                res.append('\t');
                res.append(Globals.NEWLINE);
                res.append('\t');
                String line = lines[i];
                // remove all whitespace at the end of the string, this especially includes \r created when the field content has \r\n as line separator
                line = rtrim(line);
                addWrappedLine(res, line, wrapAmount);
            } else {
                res.append(Globals.NEWLINE);
                res.append('\t');
            }
        }
        return res.toString();
    }

    private static void addWrappedLine(StringBuffer res, String line, int wrapAmount) {
        // Set our pointer to the beginning of the new line in the StringBuffer:
        int p = res.length();
        // Add the line, unmodified:
        res.append(line);

        while (p < res.length()) {
            int q = res.indexOf(" ", p + wrapAmount);
            if ((q < 0) || (q >= res.length())) {
                break;
            }

            res.deleteCharAt(q);
            res.insert(q, Globals.NEWLINE + "\t");
            p = q + Globals.NEWLINE_LENGTH;

        }
    }

    /**
     * Quotes each and every character, e.g. '!' as &#33;. Used for verbatim
     * display of arbitrary strings that may contain HTML entities.
     */
    public static String quoteForHTML(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            sb.append("&#").append((int) s.charAt(i)).append(";");
        }
        return sb.toString();
    }

    public static String quote(String s, String specials, char quoteChar) {
        return quote(s, specials, quoteChar, 0);
    }

    /**
     * Quote special characters.
     *
     * @param s
     *            The String which may contain special characters.
     * @param specials
     *            A String containing all special characters except the quoting
     *            character itself, which is automatically quoted.
     * @param quoteChar
     *            The quoting character.
     * @param linewrap
     *            The number of characters after which a linebreak is inserted
     *            (this linebreak is undone by unquote()). Set to 0 to disable.
     * @return A String with every special character (including the quoting
     *         character itself) quoted.
     */
    private static String quote(String s, String specials, char quoteChar, int linewrap) {
        StringBuilder sb = new StringBuilder();
        char c;
        int lineLength = 0;
        boolean isSpecial;
        for (int i = 0; i < s.length(); ++i) {
            c = s.charAt(i);
            isSpecial = (specials.indexOf(c) >= 0) || (c == quoteChar);
            // linebreak?
            if ((linewrap > 0)
                    && ((++lineLength >= linewrap) || (isSpecial && (lineLength >= (linewrap - 1))))) {
                sb.append(quoteChar);
                sb.append('\n');
                lineLength = 0;
            }
            if (isSpecial) {
                sb.append(quoteChar);
                ++lineLength;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Unquote special characters.
     *
     * @param s
     *            The String which may contain quoted special characters.
     * @param quoteChar
     *            The quoting character.
     * @return A String with all quoted characters unquoted.
     */
    public static String unquote(String s, char quoteChar) {
        StringBuilder sb = new StringBuilder();
        char c;
        boolean quoted = false;
        for (int i = 0; i < s.length(); ++i) {
            c = s.charAt(i);
            if (quoted) { // append literally...
                if (c != '\n') {
                    sb.append(c);
                }
                quoted = false;
            } else if (c != quoteChar) {
                sb.append(c);
            } else { // quote char
                quoted = true;
            }
        }
        return sb.toString();
    }

    /**
     * Append '.bib' to the string unless it ends with that.
     *
     * makeBibtexExtension("asfd") => "asdf.bib"
     * makeBibtexExtension("asdf.bib") => "asdf.bib"
     *
     * @param s the string
     * @return s or s + ".bib"
     */
    public static String makeBibtexExtension(String s) {
        if(!s.toLowerCase().endsWith(".bib")) {
            return s + ".bib";
        }
        return s;
    }

    public static String booleanToBinaryString(boolean expression) {
        return expression ? "1" : "0";
    }
}
