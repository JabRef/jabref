/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.model.entry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * This is an immutable class representing information of either <CODE>author</CODE>
 * or <CODE>editor</CODE> field in bibtex record.
 * <p>
 * Constructor performs parsing of raw field text and stores preformatted data.
 * Various accessor methods return author/editor field in different formats.
 * <p>
 * Parsing algorithm is designed to satisfy two requirements: (a) when author's
 * name is typed correctly, the result should coincide with the one of BiBTeX;
 * (b) for erroneous names, output should be reasonable (but may differ from
 * BiBTeX output). The following rules are used:
 * <ol>
 * <li> 'author field' is a sequence of tokens;
 * <ul>
 * <li> tokens are separated by sequences of whitespaces (<CODE>Character.isWhitespace(c)==true</CODE>),
 * commas (,), dashes (-), and tildas (~);
 * <li> every comma separates tokens, while sequences of other separators are
 * equivalent to a single separator; for example: "a - b" consists of 2 tokens
 * ("a" and "b"), while "a,-,b" consists of 3 tokens ("a", "", and "b")
 * <li> anything enclosed in braces belonges to a single token; for example:
 * "abc x{a,b,-~ c}x" consists of 2 tokens, while "abc xa,b,-~ cx" consists of 4
 * tokens ("abc", "xa","b", and "cx");
 * <li> a token followed immediately by a dash is "dash-terminated" token, and
 * all other tokens are "space-terminated" tokens; for example: in "a-b- c - d"
 * tokens "a" and "b" are dash-terminated and "c" and "d" are space-terminated;
 * <li> for the purposes of splitting of 'author name' into parts and
 * construction of abbreviation of first name, one needs definitions of first
 * latter of a token, case of a token, and abbreviation of a token:
 * <ul>
 * <li> 'first letter' of a token is the first letter character (<CODE>Character.isLetter(c)==true</CODE>)
 * that does not belong to a sequence of letters that immediately follows "\"
 * character, with one exception: if "\" is followed by "aa", "AA", "ae", "AE",
 * "l", "L", "o", "O", "oe", "OE", "i", or "j" followed by non-letter, the
 * 'first letter' of a token is a letter that follows "\"; for example: in
 * "a{x}b" 'first letter' is "a", in "{\"{U}}bel" 'first letter' is "U", in
 * "{\noopsort{\"o}}xyz" 'first letter' is "o", in "{\AE}x" 'first letter' is
 * "A", in "\aex\ijk\Oe\j" 'first letter' is "j"; if there is no letter
 * satisfying the above rule, 'first letter' is undefined;
 * <li> token is "lower-case" token, if its first letter id defined and is
 * lower-case (<CODE>Character.isLowerCase(c)==true</CODE>), and token is
 * "upper-case" token otherwise;
 * <li> 'abbreviation' of a token is the shortest prefix of the token that (a)
 * contains 'first letter' and (b) is braces-balanced; if 'first letter' is
 * undefined, 'abbreviation' is the token itself; in the above examples,
 * 'abbreviation's are "a", "{\"{U}}", "{\noopsort{\"o}}", "{\AE}",
 * "\aex\ijk\Oe\j";
 * </ul>
 * <li> the behavior based on the above definitions will be erroneous only in
 * one case: if the first-name-token is "{\noopsort{A}}john", we abbreviate it
 * as "{\noopsort{A}}.", while BiBTeX produces "j."; fixing this problem,
 * however, requires processing of the preabmle;
 * </ul>
 * <li> 'author name's in 'author field' are subsequences of tokens separated by
 * token "and" ("and" is case-insensitive); if 'author name' is an empty
 * sequence of tokens, it is ignored; for examle, both "John Smith and Peter
 * Black" and "and and John Smith and and Peter Black" consists of 2 'author
 * name's "Johm Smith" and "Peter Black" (in erroneous situations, this is a bit
 * different from BiBTeX behavior);
 * <li> 'author name' consists of 'first-part', 'von-part', 'last-part', and
 * 'junior-part', each of which is a sequence of tokens; how a sequence of
 * tokens has to be splitted into these parts, depends the number of commas:
 * <ul>
 * <li> no commas, all tokens are upper-case: 'junior-part' and 'von-part' are
 * empty, 'last-part' consist of the last token, 'first-part' consists of all
 * other tokens ('first-part' is empty, if 'author name' consists of a single
 * token); for example, in "John James Smith", 'last-part'="Smith" and
 * 'first-part'="John James";
 * <li> no commas, there exists lower-case token: 'junior-part' is empty,
 * 'first-part' consists of all upper-case tokens before the first lower-case
 * token, 'von-part' consists of lower-case tokens starting the first lower-case
 * token and ending the lower-case token that is followed by upper-case token,
 * 'last-part' consists of the rest of tokens; note that both 'first-part' and
 * 'latst-part' may be empty and 'last-part' may contain lower-case tokens; for
 * example: in "von der", 'first-part'='last-part'="", 'von-part'="von der"; in
 * "Charles Louis Xavier Joseph de la Vall{\'e}e la Poussin",
 * 'first-part'="Charles Louis Xavier Joseph", 'von-part'="de la",
 * 'last-part'="Vall{\'e}e la Poussin";
 * <li> one comma: 'junior-part' is empty, 'first-part' consists of all tokens
 * after comma, 'von-part' consists of the longest sequence of lower-case tokens
 * in the very beginning, 'last-part' consists of all tokens after 'von-part'
 * and before comma; note that any part can be empty; for example: in "de la
 * Vall{\'e}e la Poussin, Charles Louis Xavier Joseph", 'first-part'="Charles
 * Louis Xavier Joseph", 'von-part'="de la", 'last-part'="Vall{\'e}e la
 * Poussin"; in "Joseph de la Vall{\'e}e la Poussin, Charles Louis Xavier",
 * 'first-part'="Charles Louis Xavier", 'von-part'="", 'last-part'="Joseph de la
 * Vall{\'e}e la Poussin";
 * <li> two or more commas (any comma after the second one is ignored; it merely
 * separates tokens): 'junior-part' consists of all tokens between first and
 * second commas, 'first-part' consists of all tokens after the second comma,
 * tokens before the first comma are splitted into 'von-part' and 'last-part'
 * similarly to the case of one comma; for example: in "de la Vall{\'e}e
 * Poussin, Jr., Charles Louis Xavier Joseph", 'first-part'="Charles Louis
 * Xavier Joseph", 'von-part'="de la", 'last-part'="Vall{\'e}e la Poussin", and
 * 'junior-part'="Jr.";
 * </ul>
 * <li> when 'first-part', 'last-part', 'von-part', or 'junior-part' is
 * reconstructed from tokens, tokens in a part are separated either by space or
 * by dash, depending on whether the token before the separator was
 * space-terminated or dash-terminated; for the last token in a part it does not
 * matter whether it was dash- or space-terminated;
 * <li> when 'first-part' is abbreviated, each token is replaced by its
 * abbreviation followed by a period; separators are the same as in the case of
 * non-abbreviated name; for example: in "Heinrich-{\"{U}}bel Kurt von Minich",
 * 'first-part'="Heinrich-{\"{U}}bel Kurt", and its abbreviation is "H.-{\"{U}}.
 * K."
 * </ol>
 */
public class AuthorList {

    private final List<Author> authors;

    // Variables for storing computed strings, so they only need be created
    // once:
    private String authorsNatbib;
    private String authorsFirstFirstAnds;
    private String authorsAlph;

    private final String[] authorsFirstFirst = new String[4];
    private final String[] authorsLastOnly = new String[2];
    private final String[] authorLastFirstAnds = new String[2];
    private final String[] authorsLastFirst = new String[4];
    private final String[] authorsLastFirstFirstLast = new String[2];

    // The following variables are used only during parsing

    private String original; // the raw bibtex author/editor field

    // the following variables are updated by getToken procedure
    private int tokenStart; // index in orig

    private int tokenEnd; // to point 'abc' in ' abc xyz', start=2 and end=5

    // the following variables are valid only if getToken returns TOKEN_WORD
    private int tokenAbbr; // end of token abbreviation (always: token_start <

    // token_abbr <= token_end)

    private char tokenTerm; // either space or dash

    private boolean tokenCase; // true if upper-case token, false if lower-case

    // token

    // Tokens of one author name.
    // Each token occupies TGL consecutive entries in this vector (as described
    // below)
    private List<Object> tokens;

    private static final int TOKEN_GROUP_LENGTH = 4; // number of entries for

    // a token

    // the following are offsets of an entry in a group of entries for one token
    private static final int OFFSET_TOKEN = 0; // String -- token itself;

    private static final int OFFSET_TOKEN_ABBR = 1; // String -- token

    // abbreviation;

    private static final int OFFSET_TOKEN_TERM = 2; // Character -- token

    // terminator (either " " or
    // "-")

    // comma)

    // Token types (returned by getToken procedure)
    private static final int TOKEN_EOF = 0;

    private static final int TOKEN_AND = 1;

    private static final int TOKEN_COMMA = 2;

    private static final int TOKEN_WORD = 3;

    // Constant Hashtable containing names of TeX special characters
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

    private static final WeakHashMap<String, AuthorList> AUTHOR_CACHE = new WeakHashMap<>();


    /**
     * Parses the parameter strings and stores preformatted author information.
     * <p>
     * Don't call this constructor directly but rather use the getAuthorList()
     * method which caches its results.
     *
     * @param bibtexAuthors contents of either <CODE>author</CODE> or <CODE>editor</CODE>
     *                      bibtex field.
     */
    private AuthorList(String bibtexAuthors) {
        authors = new ArrayList<>(5); // 5 seems to be reasonable initial size
        original = bibtexAuthors; // initialization
        tokenStart = 0;
        tokenEnd = 0; // of parser
        while (tokenStart < original.length()) {
            Author author = getAuthor();
            if (author != null) {
                authors.add(author);
            }
        }
        // clean-up
        original = null;
        tokens = null;
    }

    /**
     * Retrieve an AuthorList for the given string of authors or editors.
     * <p>
     * This function tries to cache AuthorLists by string passed in.
     *
     * @param authors The string of authors or editors in bibtex format to parse.
     * @return An AuthorList object representing the given authors.
     */
    public static AuthorList getAuthorList(String authors) {
        AuthorList authorList = AUTHOR_CACHE.get(authors);
        if (authorList == null) {
            authorList = new AuthorList(authors);
            AUTHOR_CACHE.put(authors, authorList);
        }
        return authorList;
    }

    /**
     * This is a convenience method for getAuthorsFirstFirst()
     *
     * @see AuthorList#getAuthorsFirstFirst
     */
    public static String fixAuthor_firstNameFirstCommas(String authors, boolean abbr, boolean oxfordComma) {
        return AuthorList.getAuthorList(authors).getAuthorsFirstFirst(abbr, oxfordComma);
    }

    /**
     * This is a convenience method for getAuthorsFirstFirstAnds()
     *
     * @see AuthorList#getAuthorsFirstFirstAnds
     */
    public static String fixAuthor_firstNameFirst(String authors) {
        return AuthorList.getAuthorList(authors).getAuthorsFirstFirstAnds();
    }

    /**
     * This is a convenience method for getAuthorsLastFirst()
     *
     * @see AuthorList#getAuthorsLastFirst
     */
    public static String fixAuthor_lastNameFirstCommas(String authors, boolean abbr, boolean oxfordComma) {
        return AuthorList.getAuthorList(authors).getAuthorsLastFirst(abbr, oxfordComma);
    }

    /**
     * This is a convenience method for getAuthorsLastFirstAnds(true)
     *
     * @see AuthorList#getAuthorsLastFirstAnds
     */
    public static String fixAuthor_lastNameFirst(String authors) {
        return AuthorList.getAuthorList(authors).getAuthorsLastFirstAnds(false);
    }

    /**
     * This is a convenience method for getAuthorsLastFirstAnds()
     *
     * @see AuthorList#getAuthorsLastFirstAnds
     */
    public static String fixAuthor_lastNameFirst(String authors, boolean abbreviate) {
        return AuthorList.getAuthorList(authors).getAuthorsLastFirstAnds(abbreviate);
    }

    /**
     * This is a convenience method for getAuthorsLastOnly()
     *
     * @see AuthorList#getAuthorsLastOnly
     */
    public static String fixAuthor_lastNameOnlyCommas(String authors, boolean oxfordComma) {
        return AuthorList.getAuthorList(authors).getAuthorsLastOnly(oxfordComma);
    }

    /**
     * This is a convenience method for getAuthorsForAlphabetization()
     *
     * @see AuthorList#getAuthorsForAlphabetization
     */
    public static String fixAuthorForAlphabetization(String authors) {
        return AuthorList.getAuthorList(authors).getAuthorsForAlphabetization();
    }

    /**
     * This is a convenience method for getAuthorsNatbib()
     *
     * @see AuthorList#getAuthorsNatbib
     */
    public static String fixAuthor_Natbib(String authors) {
        return AuthorList.getAuthorList(authors).getAuthorsNatbib();
    }

    /**
     * Parses one author name and returns preformatted information.
     *
     * @return Preformatted author name; <CODE>null</CODE> if author name is
     * empty.
     */
    private Author getAuthor() {

        tokens = new ArrayList<>(); // initialization
        int vonStart = -1;
        int lastStart = -1;
        int commaFirst = -1;
        int commaSecond = -1;

        // First step: collect tokens in 'tokens' Vector and calculate indices
        token_loop:
        while (true) {
            int token = getToken();
            switch (token) {
                case TOKEN_EOF:
                case TOKEN_AND:
                    break token_loop;
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
                            vonStart = tokens.size() - AuthorList.TOKEN_GROUP_LENGTH;
                            break;
                        }
                    } else if ((lastStart < 0) && tokenCase) {
                        lastStart = tokens.size() - AuthorList.TOKEN_GROUP_LENGTH;
                        break;
                    }
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
                lastPartStart = tokens.size() - AuthorList.TOKEN_GROUP_LENGTH;
                int index = (tokens.size() - (2 * AuthorList.TOKEN_GROUP_LENGTH)) + AuthorList.OFFSET_TOKEN_TERM;
                if (index > 0) {
                    Character ch = (Character) tokens.get(index);
                    if (ch == '-') {
                        lastPartStart -= AuthorList.TOKEN_GROUP_LENGTH;
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
        return new Author(firstPartStart < 0 ? null : concatTokens(firstPartStart,
                firstPartEnd, AuthorList.OFFSET_TOKEN, false), firstPartStart < 0 ? null : concatTokens(
                firstPartStart, firstPartEnd, AuthorList.OFFSET_TOKEN_ABBR, true), vonPartStart < 0 ? null
                : concatTokens(vonPartStart, vonPartEnd, AuthorList.OFFSET_TOKEN, false),
                lastPartStart < 0 ? null : concatTokens(lastPartStart, lastPartEnd,
                        AuthorList.OFFSET_TOKEN, false), jrPartStart < 0 ? null : concatTokens(jrPartStart,
                jrPartEnd, AuthorList.OFFSET_TOKEN, false));
    }

    /**
     * Concatenates list of tokens from 'tokens' Vector. Tokens are separated by
     * spaces or dashes, dependeing on stored in 'tokens'. Callers always ensure
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
    private String concatTokens(int start, int end, int offset, boolean dotAfter) {
        StringBuilder result = new StringBuilder();
        // Here we always have start < end
        result.append((String) tokens.get(start + offset));
        if (dotAfter) {
            result.append('.');
        }
        start += AuthorList.TOKEN_GROUP_LENGTH;
        while (start < end) {
            result.append(tokens.get((start - AuthorList.TOKEN_GROUP_LENGTH) + AuthorList.OFFSET_TOKEN_TERM));
            result.append((String) tokens.get(start + offset));
            if (dotAfter) {
                result.append('.');
            }
            start += AuthorList.TOKEN_GROUP_LENGTH;
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
     * "and" (or "And", or "aND", etc.), <CODE>TOKEN_WORD</CODE> --
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
            return AuthorList.TOKEN_EOF;
        }
        if (original.charAt(tokenStart) == ',') {
            tokenEnd++;
            return AuthorList.TOKEN_COMMA;
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
                    if (AuthorList.TEX_NAMES.contains(texCmdName)) {
                        tokenCase = Character.isUpperCase(texCmdName.charAt(0));
                        firstLetterIsFound = true;
                    }
                }
                currentBackslash = -1;
            }
            if (c == '\\') {
                currentBackslash = tokenEnd;
            }
            if (bracesLevel == 0) {
                if ((c == ',') || (c == '~') || (c == '-') || Character.isWhitespace(c)) {
                    break;
                }
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
            return AuthorList.TOKEN_AND;
        } else {
            return AuthorList.TOKEN_WORD;
        }
    }

    /**
     * Returns the number of author names in this object.
     *
     * @return the number of author names in this object.
     */
    public int size() {
        return authors.size();
    }

    /**
     * Returns the <CODE>Author</CODE> object for the i-th author.
     *
     * @param i Index of the author (from 0 to <CODE>size()-1</CODE>).
     * @return the <CODE>Author</CODE> object.
     */
    public Author getAuthor(int i) {
        return authors.get(i);
    }

    /**
     * Returns the list of authors in "natbib" format.
     * <p>
     * <ul>
     * <li>"John Smith" -> "Smith"</li>
     * <li>"John Smith and Black Brown, Peter" ==> "Smith and Black Brown"</li>
     * <li>"John von Neumann and John Smith and Black Brown, Peter" ==> "von
     * Neumann et al." </li>
     * </ul>
     *
     * @return formatted list of authors.
     */
    public String getAuthorsNatbib() {
        // Check if we've computed this before:
        if (authorsNatbib != null) {
            return authorsNatbib;
        }

        StringBuilder res = new StringBuilder();
        if (size() > 0) {
            res.append(getAuthor(0).getLastOnly());
            if (size() == 2) {
                res.append(" and ");
                res.append(getAuthor(1).getLastOnly());
            } else if (size() > 2) {
                res.append(" et al.");
            }
        }
        authorsNatbib = res.toString();
        return authorsNatbib;
    }

    /**
     * Returns the list of authors separated by commas with last name only; If
     * the list consists of three or more authors, "and" is inserted before the
     * last author's name.
     * <p>
     * <p>
     * <ul>
     * <li> "John Smith" ==> "Smith"</li>
     * <li> "John Smith and Black Brown, Peter" ==> "Smith and Black Brown"</li>
     * <li> "John von Neumann and John Smith and Black Brown, Peter" ==> "von
     * Neumann, Smith and Black Brown".</li>
     * </ul>
     *
     * @param oxfordComma Whether to put a comma before the and at the end.
     * @return formatted list of authors.
     * @see <a href="http://en.wikipedia.org/wiki/Serial_comma">serial comma for an detailed explaination about the Oxford comma.</a>
     */
    public String getAuthorsLastOnly(boolean oxfordComma) {
        int abbrInt = oxfordComma ? 0 : 1;

        // Check if we've computed this before:
        if (authorsLastOnly[abbrInt] != null) {
            return authorsLastOnly[abbrInt];
        }

        StringBuilder result = new StringBuilder();
        if (size() > 0) {
            result.append(getAuthor(0).getLastOnly());
            int i = 1;
            while (i < (size() - 1)) {
                result.append(", ");
                result.append(getAuthor(i).getLastOnly());
                i++;
            }
            if ((size() > 2) && oxfordComma) {
                result.append(',');
            }
            if (size() > 1) {
                result.append(" and ");
                result.append(getAuthor(i).getLastOnly());
            }
        }
        authorsLastOnly[abbrInt] = result.toString();
        return authorsLastOnly[abbrInt];
    }

    /**
     * Returns the list of authors separated by commas with first names after
     * last name; first names are abbreviated or not depending on parameter. If
     * the list consists of three or more authors, "and" is inserted before the
     * last author's name.
     * <p>
     * <p>
     * <ul>
     * <li> "John Smith" ==> "Smith, John" or "Smith, J."</li>
     * <li> "John Smith and Black Brown, Peter" ==> "Smith, John and Black
     * Brown, Peter" or "Smith, J. and Black Brown, P."</li>
     * <li> "John von Neumann and John Smith and Black Brown, Peter" ==> "von
     * Neumann, John, Smith, John and Black Brown, Peter" or "von Neumann, J.,
     * Smith, J. and Black Brown, P.".</li>
     * </ul>
     *
     * @param abbreviate  whether to abbreivate first names.
     * @param oxfordComma Whether to put a comma before the and at the end.
     * @return formatted list of authors.
     * @see <a href="http://en.wikipedia.org/wiki/Serial_comma">serial comma for an detailed explaination about the Oxford comma.</a>
     */
    public String getAuthorsLastFirst(boolean abbreviate, boolean oxfordComma) {
        int abbrInt = abbreviate ? 0 : 1;
        abbrInt += oxfordComma ? 0 : 2;

        // Check if we've computed this before:
        if (authorsLastFirst[abbrInt] != null) {
            return authorsLastFirst[abbrInt];
        }

        StringBuilder result = new StringBuilder();
        if (size() > 0) {
            result.append(getAuthor(0).getLastFirst(abbreviate));
            int i = 1;
            while (i < (size() - 1)) {
                result.append(", ");
                result.append(getAuthor(i).getLastFirst(abbreviate));
                i++;
            }
            if ((size() > 2) && oxfordComma) {
                result.append(',');
            }
            if (size() > 1) {
                result.append(" and ");
                result.append(getAuthor(i).getLastFirst(abbreviate));
            }
        }
        authorsLastFirst[abbrInt] = result.toString();
        return authorsLastFirst[abbrInt];
    }

    @Override
    public String toString() {
        return getAuthorsLastFirstAnds(false);
    }

    /**
     * Returns the list of authors separated by "and"s with first names after
     * last name; first names are not abbreviated.
     * <p>
     * <ul>
     * <li>"John Smith" ==> "Smith, John"</li>
     * <li>"John Smith and Black Brown, Peter" ==> "Smith, John and Black
     * Brown, Peter"</li>
     * <li>"John von Neumann and John Smith and Black Brown, Peter" ==> "von
     * Neumann, John and Smith, John and Black Brown, Peter".</li>
     * </ul>
     *
     * @return formatted list of authors.
     */
    public String getAuthorsLastFirstAnds(boolean abbreviate) {
        int abbrInt = abbreviate ? 0 : 1;
        // Check if we've computed this before:
        if (authorLastFirstAnds[abbrInt] != null) {
            return authorLastFirstAnds[abbrInt];
        }

        StringBuilder result = new StringBuilder();
        if (size() > 0) {
            result.append(getAuthor(0).getLastFirst(abbreviate));
            for (int i = 1; i < size(); i++) {
                result.append(" and ");
                result.append(getAuthor(i).getLastFirst(abbreviate));
            }
        }

        authorLastFirstAnds[abbrInt] = result.toString();
        return authorLastFirstAnds[abbrInt];
    }

    public String getAuthorsLastFirstFirstLastAnds(boolean abbreviate) {
        int abbrInt = abbreviate ? 0 : 1;
        // Check if we've computed this before:
        if (authorsLastFirstFirstLast[abbrInt] != null) {
            return authorsLastFirstFirstLast[abbrInt];
        }

        StringBuilder result = new StringBuilder();
        if (size() > 0) {
            result.append(getAuthor(0).getLastFirst(abbreviate));
            for (int i = 1; i < size(); i++) {
                result.append(" and ");
                result.append(getAuthor(i).getFirstLast(abbreviate));
            }
        }

        authorsLastFirstFirstLast[abbrInt] = result.toString();
        return authorsLastFirstFirstLast[abbrInt];
    }

    /**
     * Returns the list of authors separated by commas with first names before
     * last name; first names are abbreviated or not depending on parameter. If
     * the list consists of three or more authors, "and" is inserted before the
     * last author's name.
     * <p>
     * <ul>
     * <li>"John Smith" ==> "John Smith" or "J. Smith"</li>
     * <li>"John Smith and Black Brown, Peter" ==> "John Smith and Peter Black
     * Brown" or "J. Smith and P. Black Brown"</li>
     * <li> "John von Neumann and John Smith and Black Brown, Peter" ==> "John
     * von Neumann, John Smith and Peter Black Brown" or "J. von Neumann, J.
     * Smith and P. Black Brown" </li>
     * </ul>
     *
     * @param abbr        whether to abbreivate first names.
     * @param oxfordComma Whether to put a comma before the and at the end.
     * @return formatted list of authors.
     * @see <a href="http://en.wikipedia.org/wiki/Serial_comma">serial comma for an detailed explaination about the Oxford comma.</a>
     */
    public String getAuthorsFirstFirst(boolean abbr, boolean oxfordComma) {

        int abbrInt = abbr ? 0 : 1;
        abbrInt += oxfordComma ? 0 : 2;

        // Check if we've computed this before:
        if (authorsFirstFirst[abbrInt] != null) {
            return authorsFirstFirst[abbrInt];
        }

        StringBuilder result = new StringBuilder();
        if (size() > 0) {
            result.append(getAuthor(0).getFirstLast(abbr));
            int i = 1;
            while (i < (size() - 1)) {
                result.append(", ");
                result.append(getAuthor(i).getFirstLast(abbr));
                i++;
            }
            if ((size() > 2) && oxfordComma) {
                result.append(',');
            }
            if (size() > 1) {
                result.append(" and ");
                result.append(getAuthor(i).getFirstLast(abbr));
            }
        }
        authorsFirstFirst[abbrInt] = result.toString();
        return authorsFirstFirst[abbrInt];
    }


    /**
     * Compare this object with the given one.
     * <p>
     * Will return true iff the other object is an Author and all fields are identical on a string comparison.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AuthorList)) {
            return false;
        }
        AuthorList a = (AuthorList) o;

        return this.authors.equals(a.authors);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + (authors == null ? 0 : authors.hashCode());
        return result;
    }


    /**
     * Returns the list of authors separated by "and"s with first names before
     * last name; first names are not abbreviated.
     * <p>
     * <ul>
     * <li>"John Smith" ==> "John Smith"</li>
     * <li>"John Smith and Black Brown, Peter" ==> "John Smith and Peter Black
     * Brown"</li>
     * <li>"John von Neumann and John Smith and Black Brown, Peter" ==> "John
     * von Neumann and John Smith and Peter Black Brown" </li>
     * </li>
     *
     * @return formatted list of authors.
     */
    public String getAuthorsFirstFirstAnds() {
        // Check if we've computed this before:
        if (authorsFirstFirstAnds != null) {
            return authorsFirstFirstAnds;
        }

        StringBuilder result = new StringBuilder();
        if (size() > 0) {
            result.append(getAuthor(0).getFirstLast(false));
            for (int i = 1; i < size(); i++) {
                result.append(" and ");
                result.append(getAuthor(i).getFirstLast(false));
            }
        }
        authorsFirstFirstAnds = result.toString();
        return authorsFirstFirstAnds;
    }

    /**
     * Returns the list of authors in a form suitable for alphabetization. This
     * means that last names come first, never preceded by "von" particles, and
     * that any braces are removed. First names are abbreviated so the same name
     * is treated similarly if abbreviated in one case and not in another. This
     * form is not intended to be suitable for presentation, only for sorting.
     * <p>
     * <p>
     * <ul>
     * <li>"John Smith" ==> "Smith, J.";</li>
     *
     * @return formatted list of authors
     */
    public String getAuthorsForAlphabetization() {
        if (authorsAlph != null) {
            return authorsAlph;
        }

        StringBuilder result = new StringBuilder();
        if (size() > 0) {
            result.append(getAuthor(0).getNameForAlphabetization());
            for (int i = 1; i < size(); i++) {
                result.append(" and ");
                result.append(getAuthor(i).getNameForAlphabetization());
            }
        }
        authorsAlph = result.toString();
        return authorsAlph;
    }


    /**
     * This is an immutable class that keeps information regarding single
     * author. It is just a container for the information, with very simple
     * methods to access it.
     * <p>
     * Current usage: only methods <code>getLastOnly</code>,
     * <code>getFirstLast</code>, and <code>getLastFirst</code> are used;
     * all other methods are provided for completeness.
     */
    public static class Author {

        private final String firstPart;

        private final String firstAbbr;

        private final String vonPart;

        private final String lastPart;

        private final String jrPart;


        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = (prime * result)
                    + (firstAbbr == null ? 0 : firstAbbr.hashCode());
            result = (prime * result)
                    + (firstPart == null ? 0 : firstPart.hashCode());
            result = (prime * result)
                    + (jrPart == null ? 0 : jrPart.hashCode());
            result = (prime * result)
                    + (lastPart == null ? 0 : lastPart.hashCode());
            result = (prime * result)
                    + (vonPart == null ? 0 : vonPart.hashCode());
            return result;
        }

        /**
         * Compare this object with the given one.
         * <p>
         * Will return true iff the other object is an Author and all fields are identical on a string comparison.
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Author)) {
                return false;
            }
            Author a = (Author) o;
            return EntryUtil.equals(firstPart, a.firstPart)
                    && EntryUtil.equals(firstAbbr, a.firstAbbr)
                    && EntryUtil.equals(vonPart, a.vonPart)
                    && EntryUtil.equals(lastPart, a.lastPart)
                    && EntryUtil.equals(jrPart, a.jrPart);
        }



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
         * @param last      the lats name of the author (may consist of several
         *                  tokens, like "Vall{\'e}e Poussin" in "Charles Louis Xavier
         *                  Joseph de la Vall{\'e}e Poussin")
         * @param jr        the junior part of the author's name (may consist of
         *                  several tokens, like "Jr. III" in "Smith, Jr. III, John")
         */
        public Author(String first, String firstabbr, String von, String last, String jr) {
            firstPart = removeStartAndEndBraces(first);
            firstAbbr = removeStartAndEndBraces(firstabbr);
            vonPart = removeStartAndEndBraces(von);
            lastPart = removeStartAndEndBraces(last);
            jrPart = removeStartAndEndBraces(jr);
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
                if (s.length() > 2) {
                    if (s.startsWith("{") && s.endsWith("}")) {
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
                }
                b.append(s);
                b.append(' ');
            }
            // delete last
            b.deleteCharAt(b.length() - 1);

            // now, all inner words are cleared
            // case {word word word} remains
            // as above, we have to be aware of {w}ord word wor{d} and {{w}ord word word}

            name = b.toString();

            if (name.startsWith("{") && name.endsWith("}")) {
                String inner = name.substring(1, name.length() - 1);
                if (properBrackets(inner)) {
                    return inner;
                } else {
                    return name;
                }
            } else {
                return name;
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
            StringBuffer res = new StringBuffer(getLastOnly());
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
            StringBuffer res = new StringBuffer();
            getLastOnly();
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

}
