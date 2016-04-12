package net.sf.jabref.model.entry;

import java.util.*;

public class AuthorListParser {

    /** the raw bibtex author/editor field */
    private String original;

    /** index of the start in original, for example to point to 'abc' in 'abc xyz', tokenStart=2 */
    private int tokenStart;

    /** index of the end in original, for example to point to 'abc' in 'abc xyz', tokenEnd=5 */
    private int tokenEnd;

    /** end of token abbreviation (always: tokenStart < tokenAbbr <= tokenEnd), only valid if getToken returns TOKEN_WORD */
    private int tokenAbbr;

    /** either space of dash */
    private char tokenTerm;

    /** true if upper-case token, false if lower-case */
    private boolean tokenCase;

    
    // Constant HashSet containing names of TeX special characters
    private static final Set<String> TEX_NAMES = new HashSet<>();

    // and static constructor to initialize it
    static {
        TEX_NAMES.add("aa");
        TEX_NAMES.add("ae");
        TEX_NAMES.add("l");
        TEX_NAMES.add("o");
        TEX_NAMES.add("oe");
        TEX_NAMES.add("i");
        TEX_NAMES.add("AA");
        TEX_NAMES.add("AE");
        TEX_NAMES.add("L");
        TEX_NAMES.add("O");
        TEX_NAMES.add("OE");
        TEX_NAMES.add("j");
    }


    private static final int TOKEN_GROUP_LENGTH = 4; // number of entries for

    // a token

    // the following are offsets of an entry in a group of entries for one token
    private static final int OFFSET_TOKEN = 0; // String -- token itself;

    private static final int OFFSET_TOKEN_ABBR = 1; // String -- token abbreviation;

    private static final int OFFSET_TOKEN_TERM = 2; // Character -- token terminator (either " " or
    // "-") comma)

    // Token types (returned by getToken procedure)
    private static final int TOKEN_EOF = 0;

    private static final int TOKEN_AND = 1;

    private static final int TOKEN_COMMA = 2;

    private static final int TOKEN_WORD = 3;

    /**
     * Parses the String containing person names and returns a list of person information.
     *
     * @param listOfNames the String containing the person names to be parsed
     * @return a parsed list of persons
     */
    public AuthorList parse(String listOfNames) {

        Objects.requireNonNull(listOfNames);

        // initialization of parser
        original = listOfNames;
        tokenStart = 0;
        tokenEnd = 0;

        // Parse author by author
        List<Author> authors = new ArrayList<>(5); // 5 seems to be reasonable initial size
        while (tokenStart < original.length()) {
            Author author = getAuthor();
            if (author != null) {
                authors.add(author);
            }
        }
        return new AuthorList(authors);
    }

    /**
     * Parses one author name and returns preformatted information.
     *
     * @return Preformatted author name; <CODE>null</CODE> if author name is
     * empty.
     */
    private Author getAuthor() {

        List<Object> tokens = new ArrayList<>(); // initialization
        int vonStart = -1;
        int lastStart = -1;
        int commaFirst = -1;
        int commaSecond = -1;

        // First step: collect tokens in 'tokens' Vector and calculate indices
        boolean continueLoop = true;
        while (continueLoop) {
            int token = getToken();
            switch (token) {
            case TOKEN_EOF:
            case TOKEN_AND:
                continueLoop = false;
                break;
            case TOKEN_COMMA:
                if (commaFirst < 0) {
                    commaFirst = tokens.size();
                } else if (commaSecond < 0) {
                    commaSecond = tokens.size();
                }
                break;
            case TOKEN_WORD:
                tokens.add(original.substring(tokenStart, tokenEnd));
                tokens.add(original.substring(tokenStart, tokenAbbr));
                tokens.add(tokenTerm);
                tokens.add(tokenCase);
                if (commaFirst >= 0) {
                    break;
                }
                if (lastStart >= 0) {
                    break;
                }
                if (vonStart < 0) {
                    if (!tokenCase) {
                        vonStart = tokens.size() - TOKEN_GROUP_LENGTH;
                        break;
                    }
                } else if ((lastStart < 0) && tokenCase) {
                    lastStart = tokens.size() - TOKEN_GROUP_LENGTH;
                    break;
                }
                break;
            default:
                break;
            }
        }

        // Second step: split name into parts (here: calculate indices
        // of parts in 'tokens' Vector)
        if (tokens.isEmpty()) {
            return null; // no author information
        }

        // the following negatives indicate absence of the corresponding part
        int firstPartStart = -1;
        int vonPartStart = -1;
        int lastPartStart = -1;
        int jrPartStart = -1;
        int firstPartEnd;
        int vonPartEnd = 0;
        int lastPartEnd = 0;
        int jrPartEnd = 0;
        if (commaFirst < 0) { // no commas
            if (vonStart < 0) { // no 'von part'
                lastPartEnd = tokens.size();
                lastPartStart = tokens.size() - TOKEN_GROUP_LENGTH;
                int index = (tokens.size() - (2 * TOKEN_GROUP_LENGTH)) + OFFSET_TOKEN_TERM;
                if (index > 0) {
                    Character ch = (Character) tokens.get(index);
                    if (ch == '-') {
                        lastPartStart -= TOKEN_GROUP_LENGTH;
                    }
                }
                firstPartEnd = lastPartStart;
                if (firstPartEnd > 0) {
                    firstPartStart = 0;
                }
            } else { // 'von part' is present
                if (lastStart >= 0) {
                    lastPartEnd = tokens.size();
                    lastPartStart = lastStart;
                    vonPartEnd = lastPartStart;
                } else {
                    vonPartEnd = tokens.size();
                }
                vonPartStart = vonStart;
                firstPartEnd = vonPartStart;
                if (firstPartEnd > 0) {
                    firstPartStart = 0;
                }
            }
        } else { // commas are present: it affects only 'first part' and
            // 'junior part'
            firstPartEnd = tokens.size();
            if (commaSecond < 0) { // one comma
                if (commaFirst < firstPartEnd) {
                    firstPartStart = commaFirst;
                }
            } else { // two or more commas
                if (commaSecond < firstPartEnd) {
                    firstPartStart = commaSecond;
                }
                jrPartEnd = commaSecond;
                if (commaFirst < jrPartEnd) {
                    jrPartStart = commaFirst;
                }
            }
            if (vonStart == 0) { // 'von part' is present
                if (lastStart < 0) {
                    vonPartEnd = commaFirst;
                } else {
                    lastPartEnd = commaFirst;
                    lastPartStart = lastStart;
                    vonPartEnd = lastPartStart;
                }
                vonPartStart = 0;
            } else { // no 'von part'
                lastPartEnd = commaFirst;
                if (lastPartEnd > 0) {
                    lastPartStart = 0;
                }
            }
        }

        if ((firstPartStart == -1) && (lastPartStart == -1) && (vonPartStart != -1)) {
            // There is no first or last name, but we have a von part. This is likely
            // to indicate a single-entry name without an initial capital letter, such
            // as "unknown".
            // We make the von part the last name, to facilitate handling by last-name formatters:
            lastPartStart = vonPartStart;
            lastPartEnd = vonPartEnd;
            vonPartStart = -1;
            vonPartEnd = -1;
        }

        // Third step: do actual splitting, construct Author object
        String firstPart = firstPartStart < 0 ? null : concatTokens(tokens, firstPartStart, firstPartEnd, OFFSET_TOKEN,
                false);
        String firstAbbr = firstPartStart < 0 ? null : concatTokens(tokens, firstPartStart, firstPartEnd,
                OFFSET_TOKEN_ABBR, true);
        String vonPart = vonPartStart < 0 ? null : concatTokens(tokens, vonPartStart, vonPartEnd, OFFSET_TOKEN, false);
        String lastPart = lastPartStart < 0 ? null : concatTokens(tokens, lastPartStart, lastPartEnd, OFFSET_TOKEN,
                false);
        String jrPart = jrPartStart < 0 ? null : concatTokens(tokens, jrPartStart, jrPartEnd, OFFSET_TOKEN, false);

        if(firstPart != null && lastPart != null && lastPart.equals(lastPart.toUpperCase()) && lastPart.length() < 5) {
            // The last part is a small string in complete upper case, so interpret it as initial of the first name
            // This is the case for example in "Smith SH" which we think of as lastname=Smith and firstname=SH
            // The length < 5 constraint should allow for "Smith S.H." as input
            return new Author(lastPart, lastPart, vonPart, firstPart, jrPart);
        } else {
            return new Author(firstPart, firstAbbr, vonPart, lastPart, jrPart);
        }
    }

    /**
     * Concatenates list of tokens from 'tokens' Vector. Tokens are separated by
     * spaces or dashes, depending on stored in 'tokens'. Callers always ensure
     * that start < end; thus, there exists at least one token to be
     * concatenated.
     *
     * @param start     index of the first token to be concatenated in 'tokens' Vector
     *                  (always divisible by TOKEN_GROUP_LENGTH).
     * @param end       index of the first token not to be concatenated in 'tokens'
     *                  Vector (always divisible by TOKEN_GROUP_LENGTH).
     * @param offset    offset within token group (used to request concatenation of
     *                  either full tokens or abbreviation).
     * @param dotAfter <CODE>true</CODE> -- add period after each token, <CODE>false</CODE> --
     *                  do not add.
     * @return the result of concatenation.
     */
    private String concatTokens(List<Object> tokens, int start, int end, int offset, boolean dotAfter) {
        StringBuilder result = new StringBuilder();
        // Here we always have start < end
        result.append((String) tokens.get(start + offset));
        if (dotAfter) {
            result.append('.');
        }
        int updatedStart = start + TOKEN_GROUP_LENGTH;
        while (updatedStart < end) {
            result.append(tokens.get((updatedStart - TOKEN_GROUP_LENGTH) + OFFSET_TOKEN_TERM));
            result.append((String) tokens.get(updatedStart + offset));
            if (dotAfter) {
                result.append('.');
            }
            updatedStart += TOKEN_GROUP_LENGTH;
        }
        return result.toString();
    }

    /**
     * Parses the next token.
     * <p>
     * The string being parsed is stored in global variable <CODE>orig</CODE>,
     * and position which parsing has to start from is stored in global variable
     * <CODE>token_end</CODE>; thus, <CODE>token_end</CODE> has to be set
     * to 0 before the first invocation. Procedure updates <CODE>token_end</CODE>;
     * thus, subsequent invocations do not require any additional variable
     * settings.
     * <p>
     * The type of the token is returned; if it is <CODE>TOKEN_WORD</CODE>,
     * additional information is given in global variables <CODE>token_start</CODE>,
     * <CODE>token_end</CODE>, <CODE>token_abbr</CODE>, <CODE>token_term</CODE>,
     * and <CODE>token_case</CODE>; namely: <CODE>orig.substring(token_start,token_end)</CODE>
     * is the thext of the token, <CODE>orig.substring(token_start,token_abbr)</CODE>
     * is the token abbreviation, <CODE>token_term</CODE> contains token
     * terminator (space or dash), and <CODE>token_case</CODE> is <CODE>true</CODE>,
     * if token is upper-case and <CODE>false</CODE> if token is lower-case.
     *
     * @return <CODE>TOKEN_EOF</CODE> -- no more tokens, <CODE>TOKEN_COMMA</CODE> --
     * token is comma, <CODE>TOKEN_AND</CODE> -- token is the word
     * "and" (or "And", or "aND", etc.) or a colon, <CODE>TOKEN_WORD</CODE> --
     * token is a word; additional information is given in global
     * variables <CODE>token_start</CODE>, <CODE>token_end</CODE>,
     * <CODE>token_abbr</CODE>, <CODE>token_term</CODE>, and
     * <CODE>token_case</CODE>.
     */
    private int getToken() {
        tokenStart = tokenEnd;
        while (tokenStart < original.length()) {
            char c = original.charAt(tokenStart);
            if (!((c == '~') || (c == '-') || Character.isWhitespace(c))) {
                break;
            }
            tokenStart++;
        }
        tokenEnd = tokenStart;
        if (tokenStart >= original.length()) {
            return TOKEN_EOF;
        }
        if (original.charAt(tokenStart) == ',') {
            tokenEnd++;
            return TOKEN_COMMA;
        }
        // Colon is considered to separate names like "and"
        if (original.charAt(tokenStart) == ';') {
            tokenEnd++;
            return TOKEN_AND;
        }
        tokenAbbr = -1;
        tokenTerm = ' ';
        tokenCase = true;
        int bracesLevel = 0;
        int currentBackslash = -1;
        boolean firstLetterIsFound = false;
        while (tokenEnd < original.length()) {
            char c = original.charAt(tokenEnd);
            if (c == '{') {
                bracesLevel++;
            }
            if ((c == '}') && (bracesLevel > 0)) {
                bracesLevel--;
            }
            if (firstLetterIsFound && (tokenAbbr < 0) && (bracesLevel == 0)) {
                tokenAbbr = tokenEnd;
            }
            if (!firstLetterIsFound && (currentBackslash < 0) && Character.isLetter(c)) {
                if (bracesLevel == 0) {
                    tokenCase = Character.isUpperCase(c);
                } else {
                    // If this is a particle in braces, always treat it as if it starts with
                    // an upper case letter. Otherwise a name such as "{van den Bergen}, Hans"
                    // will not yield a proper last name:
                    tokenCase = true;
                }
                firstLetterIsFound = true;
            }
            if ((currentBackslash >= 0) && !Character.isLetter(c)) {
                if (!firstLetterIsFound) {
                    String texCmdName = original.substring(currentBackslash + 1, tokenEnd);
                    if (TEX_NAMES.contains(texCmdName)) {
                        tokenCase = Character.isUpperCase(texCmdName.charAt(0));
                        firstLetterIsFound = true;
                    }
                }
                currentBackslash = -1;
            }
            if (c == '\\') {
                currentBackslash = tokenEnd;
            }
            if ((bracesLevel == 0) && ((",;~-".indexOf(c) != -1) || Character.isWhitespace(c))) {
                break;
            }
            // Morten Alver 18 Apr 2006: Removed check for hyphen '-' above to
            // prevent
            // problems with names like Bailey-Jones getting broken up and
            // sorted wrong.
            // Aaron Chen 14 Sep 2008: Enable hyphen check for first names like Chang-Chin
            tokenEnd++;
        }
        if (tokenAbbr < 0) {
            tokenAbbr = tokenEnd;
        }
        if ((tokenEnd < original.length()) && (original.charAt(tokenEnd) == '-')) {
            tokenTerm = '-';
        }
        if ("and".equalsIgnoreCase(original.substring(tokenStart, tokenEnd))) {
            return TOKEN_AND;
        } else {
            return TOKEN_WORD;
        }
    }

}
