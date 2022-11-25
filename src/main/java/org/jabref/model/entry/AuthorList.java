package org.jabref.model.entry;

import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.importer.AuthorListParser;

/**
 * This is an immutable class representing information of either <CODE>author</CODE> or <CODE>editor</CODE> field in bibtex record.
 * <p>
 * Constructor performs parsing of raw field text and stores preformatted data. Various accessor methods return author/editor field in different formats.
 * <p>
 * Parsing algorithm is designed to satisfy two requirements: (a) when author's name is typed correctly, the result should coincide with the one of BiBTeX; (b) for erroneous names, output should be reasonable (but may differ from BiBTeX output). The following rules are used:
 * <ol>
 * <li> 'author field' is a sequence of tokens;
 * <ul>
 * <li> tokens are separated by sequences of whitespaces (<CODE>Character.isWhitespace(c)==true</CODE>),
 * commas (,), dashes (-), and tildas (~);
 * <li> every comma separates tokens, while sequences of other separators are
 * equivalent to a single separator; for example: "a - b" consists of 2 tokens
 * ("a" and "b"), while "a,-,b" consists of 3 tokens ("a", "", and "b")
 * <li> anything enclosed in braces belongs to a single token; for example:
 * "abc x{a,b,-~ c}x" consists of 2 tokens, while "abc xa,b,-~ cx" consists of 4
 * tokens ("abc", "xa","b", and "cx");
 * <li> a token followed immediately by a dash is "dash-terminated" token, and
 * all other tokens are "space-terminated" tokens; for example: in "a-b- c - d"
 * tokens "a" and "b" are dash-terminated and "c" and "d" are space-terminated;
 * <li> for the purposes of splitting of 'author name' into parts and
 * construction of abbreviation of first name, one needs definitions of first
 * letter of a token, case of a token, and abbreviation of a token:
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
 * <li> token is "lower-case" token if its first letter is defined and is
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
 * <li> 'author names' in 'author field' are subsequences of tokens separated by
 * token "and" ("and" is case-insensitive); if 'author name' is an empty
 * sequence of tokens, it is ignored; for examle, both "John Smith and Peter
 * Black" and "and and John Smith and and Peter Black" consists of 2 'author
 * name's "Johm Smith" and "Peter Black" (in erroneous situations, this is a bit
 * different from BiBTeX behavior);
 * <li> 'author name' consists of 'first-part', 'von-part', 'last-part', and
 * 'junior-part', each of which is a sequence of tokens; how a sequence of
 * tokens has to be split into these parts, depends the number of commas:
 * <ul>
 * <li> no commas, all tokens are upper-case: 'junior-part' and 'von-part' are
 * empty, 'last-part' consist of the last token, 'first-part' consists of all
 * other tokens ('first-part' is empty if 'author name' consists of a single
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
 * tokens before the first comma are split into 'von-part' and 'last-part'
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
@AllowedToUseLogic("because it needs access to AuthorList parser")
public class AuthorList {

    private static final WeakHashMap<String, AuthorList> AUTHOR_CACHE = new WeakHashMap<>();
    private final List<Author> authors;
    private AuthorList latexFreeAuthors;

    /**
     * Creates a new list of authors.
     * <p>
     * Don't call this constructor directly but rather use the getAuthorList()
     * method which caches its results.
     *
     * @param authors the list of authors which should underlie this instance
     */
    private AuthorList(List<Author> authors) {
        this.authors = Objects.requireNonNull(authors);
    }

    public static AuthorList of(List<Author> authors) {
        return new AuthorList(List.copyOf(authors));
    }

    public static AuthorList of(Author... authors) {
        return new AuthorList(List.of(authors));
    }

    public static Collector<Author, ?, AuthorList> collect() {
        return Collectors.collectingAndThen(Collectors.toUnmodifiableList(), AuthorList::new);
    }

    private static String andCoordinatedConjunction(List<Author> authors, Function<Author, String> style, boolean oxfordComma) {
        var formattedAuthors = authors.stream().map(style).collect(Collectors.toUnmodifiableList());
        return andCoordinatedConjunction(formattedAuthors, oxfordComma);
    }

    private static String andCoordinatedConjunction(List<String> authors, boolean oxfordComma) {
        String lastDelimiter = oxfordComma ? ", and " : " and ";
        int lastIndex = authors.size() - 1;
        return switch (authors.size()) {
            case 0 -> "";
            case 1 -> authors.get(0);
            case 2 -> authors.get(0) + " and " + authors.get(1);
            default -> String.join(", ", authors.subList(0, lastIndex)) + lastDelimiter + authors.get(lastIndex);
        };
    }

    /**
     * Retrieve an AuthorList for the given string of authors or editors.
     * <p>
     * This function tries to cache the parsed AuthorLists by the string passed in.
     *
     * @param authors The string of authors or editors in bibtex format to parse.
     * @return An AuthorList object representing the given authors.
     */
    public static AuthorList parse(final String authors) {
        Objects.requireNonNull(authors);

        AuthorList authorList = AUTHOR_CACHE.get(authors);
        if (authorList == null) {
            AuthorListParser parser = new AuthorListParser();
            authorList = parser.parse(authors);
            AUTHOR_CACHE.put(authors, authorList);
        }
        return authorList;
    }

    /**
     * This is a convenience method for getAuthorsFirstFirst()
     *
     * @see AuthorList#getAsFirstLastNames
     */
    public static String fixAuthorFirstNameFirstCommas(String authors, boolean abbreviate, boolean oxfordComma) {
        return AuthorList.parse(authors).getAsFirstLastNames(abbreviate, oxfordComma);
    }

    /**
     * This is a convenience method for getAuthorsFirstFirstAnds()
     *
     * @see AuthorList#getAsFirstLastNamesWithAnd
     */
    public static String fixAuthorFirstNameFirst(String authors) {
        return AuthorList.parse(authors).getAsFirstLastNamesWithAnd();
    }

    /**
     * This is a convenience method for getAuthorsLastFirst()
     *
     * @see AuthorList#getAsLastFirstNames
     */
    public static String fixAuthorLastNameFirstCommas(String authors, boolean abbreviate, boolean oxfordComma) {
        return AuthorList.parse(authors).getAsLastFirstNames(abbreviate, oxfordComma);
    }

    /**
     * This is a convenience method for getAuthorsLastFirstAnds(true)
     *
     * @see AuthorList#getAsLastFirstNamesWithAnd
     */
    public static String fixAuthorLastNameFirst(String authors) {
        return AuthorList.parse(authors).getAsLastFirstNamesWithAnd(false);
    }

    /**
     * This is a convenience method for getAuthorsLastFirstAnds()
     *
     * @see AuthorList#getAsLastFirstNamesWithAnd
     */
    public static String fixAuthorLastNameFirst(String authors, boolean abbreviate) {
        return AuthorList.parse(authors).getAsLastFirstNamesWithAnd(abbreviate);
    }

    /**
     * This is a convenience method for getAuthorsLastOnly()
     *
     * @see AuthorList#getAsLastNames
     */
    public static String fixAuthorLastNameOnlyCommas(String authors, boolean oxfordComma) {
        return AuthorList.parse(authors).getAsLastNames(oxfordComma);
    }

    /**
     * This is a convenience method for getAuthorsForAlphabetization()
     *
     * @see AuthorList#getForAlphabetization
     */
    public static String fixAuthorForAlphabetization(String authors) {
        return AuthorList.parse(authors).getForAlphabetization();
    }

    /**
     * This is a convenience method for getAuthorsNatbib()
     *
     * @see AuthorList#getAsNatbib
     */
    public static String fixAuthorNatbib(String authors) {
        return AuthorList.parse(authors).getAsNatbib();
    }

    /**
     * Returns the number of author names in this object.
     *
     * @return the number of author names in this object.
     */
    public int getNumberOfAuthors() {
        return authors.size();
    }

    /**
     * Returns true if there are no authors in the list.
     *
     * @return true if there are no authors in the list.
     */
    public boolean isEmpty() {
        return authors.isEmpty();
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
     * Returns the a list of <CODE>Author</CODE> objects.
     *
     * @return the <CODE>List&lt;Author></CODE> object.
     */
    public List<Author> getAuthors() {
        return authors;
    }

    /**
     * Returns a LaTeX-free version of this `AuthorList`.
     */
    public AuthorList latexFree() {
        if (latexFreeAuthors == null) {
            latexFreeAuthors = new AuthorList(authors.stream()
                                                     .map(Author::latexFree)
                                                     .collect(Collectors.toUnmodifiableList()));
            latexFreeAuthors.latexFreeAuthors = latexFreeAuthors;
        }
        return latexFreeAuthors;
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
    public String getAsNatbib() {
        var authors = getAuthors();
        return switch (authors.size()) {
            case 0 -> "";
            case 1 -> authors.get(0).getLastOnly();
            case 2 -> authors.get(0).getLastOnly() + " and " + authors.get(1).getLastOnly();
            default -> authors.get(0).getLastOnly() + " et al.";
        };
    }

    /**
     * Returns the list of authors separated by commas with last name only; If the list consists of two or more authors, "and" is inserted before the last author's name.
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
     * @see <a href="http://en.wikipedia.org/wiki/Serial_comma">serial comma for an detailed explaination about the
     * Oxford comma.</a>
     */
    public String getAsLastNames(boolean oxfordComma) {
        return andCoordinatedConjunction(getAuthors(), Author::getLastOnly, oxfordComma);
    }

    /**
     * Returns the list of authors separated by commas with first names after last name; first names are abbreviated or not depending on parameter. If the list consists of three or more authors, "and" is inserted before the last author's name.
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
     * @see <a href="http://en.wikipedia.org/wiki/Serial_comma">serial comma for an detailed explaination about the
     * Oxford comma.</a>
     */
    public String getAsLastFirstNames(boolean abbreviate, boolean oxfordComma) {
        return andCoordinatedConjunction(getAuthors(), (auth) -> auth.getLastFirst(abbreviate), oxfordComma);
    }

    @Override
    public String toString() {
        return authors.toString();
    }

    /**
     * Returns the list of authors separated by "and"s with first names after last name; first names are not abbreviated.
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
    public String getAsLastFirstNamesWithAnd(boolean abbreviate) {
        return getAuthors().stream()
                           .map(author -> author.getLastFirst(abbreviate))
                           .collect(Collectors.joining(" and "));
    }

    /**
     * Returns a list of authors separated with "and". The first author is formatted with {@link Author#getLastFirst(boolean)} and each subsequent author is formatted with {@link Author#getFirstLast(boolean)}.
     *
     * @param abbreviate first names.
     */
    public String getAsLastFirstFirstLastNamesWithAnd(boolean abbreviate) {
        return switch (authors.size()) {
            case 0 -> "";
            case 1 -> authors.get(0).getLastFirst(abbreviate);
            default -> authors.stream()
                              .skip(1)
                              .map(author -> author.getFirstLast(abbreviate))
                              .collect(Collectors.joining(
                                      " and ",
                                      authors.get(0).getLastFirst(abbreviate) + " and ",
                                      ""));
        };
    }

    /**
     * Returns the list of authors separated by commas with first names before last name; first names are abbreviated or not depending on parameter. If the list consists of three or more authors, "and" is inserted before the last author's name.
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
     * @param abbreviate  whether to abbreivate first names.
     * @param oxfordComma Whether to put a comma before the and at the end.
     * @return formatted list of authors.
     * @see <a href="http://en.wikipedia.org/wiki/Serial_comma">serial comma for an detailed explaination about the
     * Oxford comma.</a>
     */
    public String getAsFirstLastNames(boolean abbreviate, boolean oxfordComma) {
        return andCoordinatedConjunction(getAuthors(), author -> author.getFirstLast(abbreviate), oxfordComma);
    }

    /**
     * Compare this object with the given one.
     *
     * @return `true` iff the other object is an AuthorList, all contained authors are in the same order (and these authors' fields are `Objects.equals`)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthorList)) {
            return false;
        }
        AuthorList a = (AuthorList) o;

        return this.authors.equals(a.authors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authors);
    }

    /**
     * Returns the list of authors separated by "and"s with first names before last name; first names are not abbreviated.
     * <p>
     * <ul>
     * <li>"John Smith" ==> "John Smith"</li>
     * <li>"John Smith and Black Brown, Peter" ==> "John Smith and Peter Black
     * Brown"</li>
     * <li>"John von Neumann and John Smith and Black Brown, Peter" ==> "John
     * von Neumann and John Smith and Peter Black Brown" </li>
     * </ul>
     *
     * @return formatted list of authors.
     */
    public String getAsFirstLastNamesWithAnd() {
        return getAuthors().stream()
                           .map(author -> author.getFirstLast(false))
                           .collect(Collectors.joining(" and "));
    }

    /**
     * Returns the list of authors in a form suitable for alphabetization. This means that last names come first, never preceded by "von" particles, and that any braces are removed. First names are abbreviated so the same name is treated similarly if abbreviated in one case and not in another. This form is not intended to be suitable for presentation, only for sorting.
     * <p>
     * <ul>
     * <li>"John Smith" ==> "Smith, J.";</li>
     * </ul>
     *
     * @return formatted list of authors
     */
    public String getForAlphabetization() {
        return getAuthors().stream()
                           .map(Author::getNameForAlphabetization)
                           .collect(Collectors.joining(" and "));
    }
}
