package org.jabref.logic.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.StringJoiner;
import java.lang.StringBuilder;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.formatter.Formatters;
import org.jabref.logic.formatter.casechanger.Word;
import org.jabref.logic.layout.format.RemoveLatexCommandsFormatter;
import org.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import org.jabref.model.cleanup.Formatter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.strings.StringUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author saulius
 * The BracketedExpressionExpander provides methods to expand bracketed expressions,
 * such as [year]_[author]_[firstpage], using information from a provided BibEntry.
 * The above-mentioned expression would yield 2017_Kizune_123 when expanded using the
 * BibTeX entry "@Article{ authors = {A. Kizune}, year = {2017}, pages={123-6}}".
 */
public class BracketedExpressionExpander {

    private static final int CHARS_OF_FIRST = 5;

    private final BibEntry bibentry;

    /**
     * @param bibentry
     */
    public BracketedExpressionExpander(BibEntry bibentry) {
        this.bibentry = bibentry;
    }

    @Override
    public String toString() {
        return "BracketedExpressionExpander [bibentry=" + bibentry + "]";
    }

    public String expandBrackets(String pattern) {
        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(pattern,"\\[]",true);

        while(st.hasMoreTokens()) {
            String token = st.nextToken();
            if(token.equals("\\")) {
                if(st.hasMoreTokens()) {
                    sb.append(st.nextToken());
                }
                // FIXME: else -> raise exception or log? (S.G.)
            } else {
                if(token.equals("[")) {
                    // Fetch the next token after the '[':
                    token = st.nextToken();
                    sb.append(getFieldValue(bibentry,token,';'));
                    // Fetch and discard the closing ']'
                    token = st.nextToken();
                    // if( st.nextToken().equals("]")) {
                    //     FIXME: raise esception or log? S.G.
                    // }
                } else {
                    sb.append(token);
                }
            }
        }

        return sb.toString();
    }

    public static String getFieldValue(BibEntry entry, String value, Character keywordDelimiter) {
        String val = value;
        try {
            if (val.startsWith("auth") || val.startsWith("pureauth")) {

                /*
                 * For label code "auth...": if there is no author, but there
                 * are editor(s) (e.g. for an Edited Book), use the editor(s)
                 * instead. (saw27@mrao.cam.ac.uk). This is what most people
                 * want, but in case somebody really needs a field which expands
                 * to nothing if there is no author (e.g. someone who uses both
                 * "auth" and "ed" in the same label), we provide an alternative
                 * form "pureauth..." which does not do this fallback
                 * substitution of editor.
                 */
                String authString;
                authString = entry.getField(FieldName.AUTHOR).orElse("");

                if (val.startsWith("pure")) {
                    // remove the "pure" prefix so the remaining
                    // code in this section functions correctly
                    val = val.substring(4);
                }

                if (authString.isEmpty()) {
                    authString = entry.getField(FieldName.EDITOR).orElse("");
                }

                // Gather all author-related checks, so we don't
                // have to check all the time.
                if ("auth".equals(val)) {
                    return firstAuthor(authString);
                } else if ("authForeIni".equals(val)) {
                    return firstAuthorForenameInitials(authString);
                } else if ("authFirstFull".equals(val)) {
                    return firstAuthorVonAndLast(authString);
                } else if ("authors".equals(val)) {
                    return allAuthors(authString);
                } else if ("authorsAlpha".equals(val)) {
                    return authorsAlpha(authString);
                }
                // Last author's last name
                else if ("authorLast".equals(val)) {
                    return lastAuthor(authString);
                } else if ("authorLastForeIni".equals(val)) {
                    return lastAuthorForenameInitials(authString);
                } else if ("authorIni".equals(val)) {
                    return oneAuthorPlusIni(authString);
                } else if (val.matches("authIni[\\d]+")) {
                    int num = Integer.parseInt(val.substring(7));
                    return authIniN(authString, num);
                } else if ("auth.auth.ea".equals(val)) {
                    return authAuthEa(authString);
                } else if ("auth.etal".equals(val)) {
                    return authEtal(authString, ".", ".etal");
                } else if ("authEtAl".equals(val)) {
                    return authEtal(authString, "", "EtAl");
                } else if ("authshort".equals(val)) {
                    return authshort(authString);
                } else if (val.matches("auth[\\d]+_[\\d]+")) {
                    String[] nums = val.substring(4).split("_");
                    return authNofMth(authString, Integer.parseInt(nums[0]),
                            Integer.parseInt(nums[1]));
                } else if (val.matches("auth\\d+")) {
                    // authN. First N chars of the first author's last
                    // name.

                    String fa = firstAuthor(authString);
                    int num = Integer.parseInt(val.substring(4));
                    if (num > fa.length()) {
                        num = fa.length();
                    }
                    return fa.substring(0, num);
                } else if (val.matches("authors\\d+")) {
                    return nAuthors(authString, Integer.parseInt(val.substring(7)));
                } else {
                    // This "auth" business was a dead end, so just
                    // use it literally:
                    return entry.getFieldOrAlias(val).orElse("");
                }
            } else if (val.startsWith("ed")) {
                // Gather all markers starting with "ed" here, so we
                // don't have to check all the time.
                if ("edtr".equals(val)) {
                    return firstAuthor(entry.getField(FieldName.EDITOR).orElse(""));
                } else if ("edtrForeIni".equals(val)) {
                    return firstAuthorForenameInitials(entry.getField(FieldName.EDITOR).orElse(""));
                } else if ("editors".equals(val)) {
                    return allAuthors(entry.getField(FieldName.EDITOR).orElse(""));
                    // Last author's last name
                } else if ("editorLast".equals(val)) {
                    return lastAuthor(entry.getField(FieldName.EDITOR).orElse(""));
                } else if ("editorLastForeIni".equals(val)) {
                    return lastAuthorForenameInitials(entry.getField(FieldName.EDITOR).orElse(""));
                } else if ("editorIni".equals(val)) {
                    return oneAuthorPlusIni(entry.getField(FieldName.EDITOR).orElse(""));
                } else if (val.matches("edtrIni[\\d]+")) {
                    int num = Integer.parseInt(val.substring(7));
                    return authIniN(entry.getField(FieldName.EDITOR).orElse(""), num);
                } else if (val.matches("edtr[\\d]+_[\\d]+")) {
                    String[] nums = val.substring(4).split("_");
                    return authNofMth(entry.getField(FieldName.EDITOR).orElse(""),
                            Integer.parseInt(nums[0]),
                            Integer.parseInt(nums[1]) - 1);
                } else if ("edtr.edtr.ea".equals(val)) {
                    return authAuthEa(entry.getField(FieldName.EDITOR).orElse(""));
                } else if ("edtrshort".equals(val)) {
                    return authshort(entry.getField(FieldName.EDITOR).orElse(""));
                }
                // authN. First N chars of the first author's last
                // name.
                else if (val.matches("edtr\\d+")) {
                    String fa = firstAuthor(entry.getField(FieldName.EDITOR).orElse(""));
                    int num = Integer.parseInt(val.substring(4));
                    if (num > fa.length()) {
                        num = fa.length();
                    }
                    return fa.substring(0, num);
                } else {
                    // This "ed" business was a dead end, so just
                    // use it literally:
                    return entry.getFieldOrAlias(val).orElse("");
                }
            } else if ("firstpage".equals(val)) {
                return firstPage(entry.getField(FieldName.PAGES).orElse(""));
            } else if ("pageprefix".equals(val)) {
                return pagePrefix(entry.getField(FieldName.PAGES).orElse(""));
            } else if ("lastpage".equals(val)) {
                return lastPage(entry.getField(FieldName.PAGES).orElse(""));
            } else if ("title".equals(val)) {
                return camelizeSignificantWordsInTitle(entry.getField(FieldName.TITLE).orElse(""));
            } else if ("shorttitle".equals(val)) {
                return getTitleWords(3, entry.getField(FieldName.TITLE).orElse(""));
            } else if ("shorttitleINI".equals(val)) {
                return keepLettersAndDigitsOnly(
                        applyModifiers(getTitleWordsWithSpaces(3, entry.getField(FieldName.TITLE).orElse("")),
                                Collections.singletonList("abbr"), 0));
            } else if ("veryshorttitle".equals(val)) {
                return getTitleWords(1,
                        removeSmallWords(entry.getField(FieldName.TITLE).orElse("")));
            } else if ("camel".equals(val)) {
                return getCamelizedTitle(entry.getField(FieldName.TITLE).orElse(""));
            } else if ("shortyear".equals(val)) {
                String yearString = entry.getFieldOrAlias(FieldName.YEAR).orElse("");
                if (yearString.isEmpty()) {
                    return yearString;
                    // In press/in preparation/submitted
                } else if (yearString.startsWith("in") || yearString.startsWith("sub")) {
                    return "IP";
                } else if (yearString.length() > 2) {
                    return yearString.substring(yearString.length() - 2);
                } else {
                    return yearString;
                }
            } else if (val.matches("keyword\\d+")) {
                // according to LabelPattern.php, it returns keyword number n
                int num = Integer.parseInt(val.substring(7));
                KeywordList separatedKeywords = entry.getKeywords(keywordDelimiter);
                if (separatedKeywords.size() < num) {
                    // not enough keywords
                    return "";
                } else {
                    // num counts from 1 to n, but index in arrayList count from 0 to n-1
                    return separatedKeywords.get(num - 1).toString();
                }
            } else if (val.matches("keywords\\d*")) {
                // return all keywords, not separated
                int num;
                if (val.length() > 8) {
                    num = Integer.parseInt(val.substring(8));
                } else {
                    num = Integer.MAX_VALUE;
                }
                KeywordList separatedKeywords = entry.getKeywords(keywordDelimiter);
                StringBuilder sb = new StringBuilder();
                int i = 0;
                for (Keyword keyword : separatedKeywords) {
                    // remove all spaces
                    sb.append(keyword.toString().replaceAll("\\s+", ""));

                    i++;
                    if (i >= num) {
                        break;
                    }
                }
                return sb.toString();
            } else {
                // we haven't seen any special demands
                return entry.getFieldOrAlias(val).orElse("");
            }
        }
        catch (NullPointerException ex) {
            // LOGGER.debug("Problem making label", ex);
            return "";
        }
    }

    /**
     * Applies modifiers to a label generated based on a field marker.
     * @param label The generated label.
     * @param parts String array containing the modifiers.
     * @param offset The number of initial items in the modifiers array to skip.
     * @return The modified label.
     */
    public static String applyModifiers(final String label, final List<String> parts, final int offset) {
        String resultingLabel = label;
        if (parts.size() > offset) {
            for (int j = offset; j < parts.size(); j++) {
                String modifier = parts.get(j);

                if ("abbr".equals(modifier)) {
                    // Abbreviate - that is,
                    StringBuilder abbreviateSB = new StringBuilder();
                    String[] words = resultingLabel.replaceAll("[\\{\\}']", "")
                            .split("[\\(\\) \r\n\"]");
                    for (String word : words) {
                        if (!word.isEmpty()) {
                            abbreviateSB.append(word.charAt(0));
                        }
                    }
                    resultingLabel =  abbreviateSB.toString();
                } else {
                    Optional<Formatter> formatter = Formatters.getFormatterForModifier(modifier);
                    if (formatter.isPresent()) {
                        resultingLabel = formatter.get().format(label);
                    } else if (!modifier.isEmpty() && (modifier.length() >= 2) && (modifier.charAt(0) == '(') && modifier.endsWith(")")) {
                        // Alternate text modifier in parentheses. Should be inserted if
                        // the label is empty:
                        if (label.isEmpty() && (modifier.length() > 2)) {
                            resultingLabel = modifier.substring(1, modifier.length() - 1);
                        } else {
                            resultingLabel = label;
                        }
                    } else {
                        // LOGGER.info("Key generator warning: unknown modifier '"
                        //        + modifier + "'.");
                        resultingLabel = label;
                    }
                }
            }
        }

        return resultingLabel;
    }

    /**
     * Determines "number" words out of the "title" field in the given BibTeX entry
     */
    public static String getTitleWords(int number, String title) {
        return keepLettersAndDigitsOnly(getTitleWordsWithSpaces(number, title));
    }

    /**
     * Removes any '-', unnecessary whitespace and latex commands formatting
     */
    private static String formatTitle(String title) {
        String ss = new RemoveLatexCommandsFormatter().format(title);
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder current;
        int piv = 0;

        while (piv < ss.length()) {
            current = new StringBuilder();
            // Get the next word:
            while ((piv < ss.length()) && !Character.isWhitespace(ss.charAt(piv))
                    && (ss.charAt(piv) != '-')) {
                current.append(ss.charAt(piv));
                piv++;
            }
            piv++;
            // Check if it is ok:
            String word = current.toString().trim();
            if (word.isEmpty()) {
                continue;
            }

            // If we get here, the word was accepted.
            if (stringBuilder.length() > 0) {
                stringBuilder.append(' ');
            }
            stringBuilder.append(word);
        }

        return stringBuilder.toString();
    }

    /**
     * Capitalises and concatenates the words out of the "title" field in the given BibTeX entry
     */
    public static String getCamelizedTitle(String title) {
        return keepLettersAndDigitsOnly(camelizeTitle(title));
    }

    private static String camelizeTitle(String title) {
        StringBuilder stringBuilder = new StringBuilder();
        String formattedTitle = formatTitle(title);

        try (Scanner titleScanner = new Scanner(formattedTitle)) {
            while (titleScanner.hasNext()) {
                String word = titleScanner.next();

                // Camelize the word
                word = word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);

                if (stringBuilder.length() > 0) {
                    stringBuilder.append(' ');
                }
                stringBuilder.append(word);
            }
        }

        return stringBuilder.toString();
    }

    /**
     * Capitalises the significant words of the "title" field in the given BibTeX entry
     */
    public static String camelizeSignificantWordsInTitle(String title) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        String formattedTitle = formatTitle(title);
        Boolean camelize;

        try (Scanner titleScanner = new Scanner(formattedTitle)) {
            while (titleScanner.hasNext()) {
                String word = titleScanner.next();
                camelize = true;

                // Camelize the word if it is significant
                for (String smallWord : Word.SMALLER_WORDS) {
                    if (word.equalsIgnoreCase(smallWord)) {
                        camelize = false;
                        continue;
                    }
                }
                // We want to capitalize significant words and the first word of the title
                if (camelize || (stringJoiner.length() == 0)) {
                    word = word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
                } else {
                    word = word.substring(0, 1).toLowerCase(Locale.ROOT) + word.substring(1);
                }

                stringJoiner.add(word);
            }
        }

        return stringJoiner.toString();
    }

    public static String removeSmallWords(String title) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        String formattedTitle = formatTitle(title);

        try (Scanner titleScanner = new Scanner(formattedTitle)) {
            mainl: while (titleScanner.hasNext()) {
                String word = titleScanner.next();

                for (String smallWord : Word.SMALLER_WORDS) {
                    if (word.equalsIgnoreCase(smallWord)) {
                        continue mainl;
                    }
                }

                stringJoiner.add(word);
            }
        }

        return stringJoiner.toString();
    }

    private static String getTitleWordsWithSpaces(int number, String title) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        String formattedTitle = formatTitle(title);
        int words = 0;

        try (Scanner titleScanner = new Scanner(formattedTitle)) {
            while (titleScanner.hasNext() && (words < number)) {
                String word = titleScanner.next();

                stringJoiner.add(word);
                words++;
            }
        }

        return stringJoiner.toString();
    }

    private static String keepLettersAndDigitsOnly(String in) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < in.length(); i++) {
            if (Character.isLetterOrDigit(in.charAt(i))) {
                stringBuilder.append(in.charAt(i));
            }
        }
        return stringBuilder.toString();
    }

    /**
     * Gets the last name of the first author/editor
     *
     * @param authorField
     *            a <code>String</code>
     * @return the surname of an author/editor or "" if no author was found
     *    This method is guaranteed to never return null.
     *
     * @throws NullPointerException
     *             if authorField == null
     */
    public static String firstAuthor(String authorField) {
        AuthorList authorList = AuthorList.parse(authorField);
        if (authorList.isEmpty()) {
            return "";
        }
        return authorList.getAuthor(0).getLast().orElse("");

    }

    /**
     * Gets the first name initials of the first author/editor
     *
     * @param authorField
     *            a <code>String</code>
     * @return the first name initial of an author/editor or "" if no author was found
     *    This method is guaranteed to never return null.
     *
     * @throws NullPointerException
     *             if authorField == null
     */
    public static String firstAuthorForenameInitials(String authorField) {
        AuthorList authorList = AuthorList.parse(authorField);
        if (authorList.isEmpty()) {
            return "";
        }
        return authorList.getAuthor(0).getFirstAbbr().map(s -> s.substring(0, 1)).orElse("");
    }

    /**
     * Gets the von part and the last name of the first author/editor
     * No spaces are returned
     *
     * @param authorField
     *            a <code>String</code>
     * @return the von part and surname of an author/editor or "" if no author was found.
     *  This method is guaranteed to never return null.
     *
     * @throws NullPointerException
     *             if authorField == null
     */
    public static String firstAuthorVonAndLast(String authorField) {
        AuthorList authorList = AuthorList.parse(authorField);
        if (authorList.isEmpty()) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();
        authorList.getAuthor(0).getVon().ifPresent(vonAuthor -> stringBuilder.append(vonAuthor.replaceAll(" ", "")));
        authorList.getAuthor(0).getLast().ifPresent(stringBuilder::append);
        return stringBuilder.toString();
    }

    /**
     * Gets the last name of the last author/editor
     * @param authorField a <code>String</code>
     * @return the surname of an author/editor
     */
    public static String lastAuthor(String authorField) {
        String[] tokens = AuthorList.fixAuthorForAlphabetization(authorField).split("\\s+\\band\\b\\s+");
        if (tokens.length > 0) {
            String[] lastAuthor = tokens[tokens.length - 1].split(",");
            return lastAuthor[0];
        } else {
            // if author is empty
            return "";
        }
    }

    /**
     * Gets the forename initials of the last author/editor
     *
     * @param authorField
     *            a <code>String</code>
     * @return the forename initial of an author/editor or "" if no author was found
     *    This method is guaranteed to never return null.
     *
     * @throws NullPointerException
     *             if authorField == null
     */
    public static String lastAuthorForenameInitials(String authorField) {
        AuthorList authorList = AuthorList.parse(authorField);
        if (authorList.isEmpty()) {
            return "";
        }
        return authorList.getAuthor(authorList.getNumberOfAuthors() - 1).getFirstAbbr().map(s -> s.substring(0, 1))
                .orElse("");
    }

    /**
     * Gets the last name of all authors/editors
     * @param authorField a <code>String</code>
     * @return the sur name of all authors/editors
     */
    public static String allAuthors(String authorField) {
        // Quick hack to use NAuthors to avoid code duplication
        return nAuthors(authorField, Integer.MAX_VALUE);
    }

    /**
     * Returns the authors according to the BibTeX-alpha-Style
     * @param authorField string containing the value of the author field
     * @return the initials of all authornames
     */
    public static String authorsAlpha(String authorField) {
        String authors = "";

        String fixedAuthors = AuthorList.fixAuthorLastNameOnlyCommas(authorField, false);

        // drop the "and" before the last author
        // -> makes processing easier
        fixedAuthors = fixedAuthors.replace(" and ", ", ");

        String[] tokens = fixedAuthors.split(",");
        int max = tokens.length > 4 ? 3 : tokens.length;
        if (max == 1) {
            String[] firstAuthor = tokens[0].replaceAll("\\s+", " ").trim().split(" ");
            // take first letter of any "prefixes" (e.g. van der Aalst -> vd)
            for (int j = 0; j < (firstAuthor.length - 1); j++) {
                authors = authors.concat(firstAuthor[j].substring(0, 1));
            }
            // append last part of last name completely
            authors = authors.concat(firstAuthor[firstAuthor.length - 1].substring(0,
                    Math.min(3, firstAuthor[firstAuthor.length - 1].length())));
        } else {
            for (int i = 0; i < max; i++) {
                // replace all whitespaces by " "
                // split the lastname at " "
                String[] curAuthor = tokens[i].replaceAll("\\s+", " ").trim().split(" ");
                for (String aCurAuthor : curAuthor) {
                    // use first character of each part of lastname
                    authors = authors.concat(aCurAuthor.substring(0, 1));
                }
            }
            if (tokens.length > 4) {
                authors = authors.concat("+");
            }
        }
        return authors;
    }

    /**
     * Gets the surnames of the first N authors and appends EtAl if there are more than N authors
     * @param authorField a <code>String</code>
     * @param n the number of desired authors
     * @return Gets the surnames of the first N authors and appends EtAl if there are more than N authors
     */
    public static String nAuthors(String authorField, int n) {
        String[] tokens = AuthorList.fixAuthorForAlphabetization(authorField).split("\\s+\\band\\b\\s+");
        int i = 0;
        StringBuilder authorSB = new StringBuilder();
        while ((tokens.length > i) && (i < n)) {
            String lastName = tokens[i].replaceAll(",\\s+.*", "");
            authorSB.append(lastName);
            i++;
        }
        if (tokens.length > n) {
            authorSB.append("EtAl");
        }
        return authorSB.toString();
    }

    /**
     * Gets the first part of the last name of the first
     * author/editor, and appends the last name initial of the
     * remaining authors/editors.
     * Maximum 5 characters
     * @param authorField a <code>String</code>
     * @return the surname of all authors/editors
     */
    public static String oneAuthorPlusIni(String authorField) {
        String fixedAuthorField = AuthorList.fixAuthorForAlphabetization(authorField);
        String[] tokens = fixedAuthorField.split("\\s+\\band\\b\\s+");
        if (tokens.length == 0) {
            return "";
        }

        String firstAuthor = tokens[0].split(",")[0];
        StringBuilder authorSB = new StringBuilder();
        authorSB.append(firstAuthor.substring(0, Math.min(CHARS_OF_FIRST, firstAuthor.length())));
        int i = 1;
        while (tokens.length > i) {
            // convert lastname, firstname to firstname lastname
            authorSB.append(tokens[i].charAt(0));
            i++;
        }
        return authorSB.toString();
    }

    /**
     * auth.auth.ea format:
     * Isaac Newton and James Maxwell and Albert Einstein (1960)
     * Isaac Newton and James Maxwell (1960)
     *  give:
     * Newton.Maxwell.ea
     * Newton.Maxwell
     */
    public static String authAuthEa(String authorField) {
        String fixedAuthorField = AuthorList.fixAuthorForAlphabetization(authorField);

        String[] tokens = fixedAuthorField.split("\\s+\\band\\b\\s+");
        if (tokens.length == 0) {
            return "";
        }

        StringBuilder author = new StringBuilder();
        // append first author
        author.append((tokens[0].split(","))[0]);
        if (tokens.length >= 2) {
            // append second author
            author.append('.').append((tokens[1].split(","))[0]);
        }
        if (tokens.length > 2) {
            // append ".ea" if more than 2 authors
            author.append(".ea");
        }

        return author.toString();
    }

    /**
     * auth.etal, authEtAl, ... format:
     * Isaac Newton and James Maxwell and Albert Einstein (1960)
     * Isaac Newton and James Maxwell (1960)
     *
     *  auth.etal give (delim=".", append=".etal"):
     * Newton.etal
     * Newton.Maxwell
     *
     *  authEtAl give (delim="", append="EtAl"):
     * NewtonEtAl
     * NewtonMaxwell
     *
     * Note that [authEtAl] equals [authors2]
     */
    public static String authEtal(String authorField, String delim,
            String append) {
        String fixedAuthorField = AuthorList.fixAuthorForAlphabetization(authorField);

        String[] tokens = fixedAuthorField.split("\\s*\\band\\b\\s*");
        if (tokens.length == 0) {
            return "";
        }
        StringBuilder author = new StringBuilder();
        author.append((tokens[0].split(","))[0]);
        if (tokens.length == 2) {
            author.append(delim).append((tokens[1].split(","))[0]);
        } else if (tokens.length > 2) {
            author.append(append);
        }

        return author.toString();
    }

    /**
     * The first N characters of the Mth author/editor.
     * M starts counting from 1
     */
    public static String authNofMth(String authorField, int n, int m) {
        // have m counting from 0
        int mminusone = m - 1;

        String fixedAuthorField = AuthorList.fixAuthorForAlphabetization(authorField);

        String[] tokens = fixedAuthorField.split("\\s+\\band\\b\\s+");
        if ((tokens.length <= mminusone) || (n < 0) || (mminusone < 0)) {
            return "";
        }
        String lastName = (tokens[mminusone].split(","))[0];
        if (lastName.length() <= n) {
            return lastName;
        } else {
            return lastName.substring(0, n);
        }
    }

    /**
     * authshort format:
     * added by Kolja Brix, kbx@users.sourceforge.net
     *
     * given author names
     *
     *   Isaac Newton and James Maxwell and Albert Einstein and N. Bohr
     *
     *   Isaac Newton and James Maxwell and Albert Einstein
     *
     *   Isaac Newton and James Maxwell
     *
     *   Isaac Newton
     *
     * yield
     *
     *   NME+
     *
     *   NME
     *
     *   NM
     *
     *   Newton
     */
    public static String authshort(String authorField) {
        String fixedAuthorField = AuthorList.fixAuthorForAlphabetization(authorField);
        StringBuilder author = new StringBuilder();
        String[] tokens = fixedAuthorField.split("\\band\\b");
        int i = 0;

        if (tokens.length == 1) {
            author.append(authNofMth(fixedAuthorField, fixedAuthorField.length(), 1));
        } else if (tokens.length >= 2) {
            while ((tokens.length > i) && (i < 3)) {
                author.append(authNofMth(fixedAuthorField, 1, i + 1));
                i++;
            }
            if (tokens.length > 3) {
                author.append('+');
            }
        }

        return author.toString();
    }

    /**
     * authIniN format:
     *
     * Each author gets (N div #authors) chars, the remaining (N mod #authors)
     * chars are equally distributed to the authors first in the row.
     *
     * If (N < #authors), only the first N authors get mentioned.
     *
     * For example if
     *
     * a) I. Newton and J. Maxwell and A. Einstein and N. Bohr (..)
     *
     * b) I. Newton and J. Maxwell and A. Einstein
     *
     * c) I. Newton and J. Maxwell
     *
     * d) I. Newton
     *
     * authIni4 gives: a) NMEB, b) NeME, c) NeMa, d) Newt
     *
     * @param authorField
     *            The authors to format.
     *
     * @param n
     *            The maximum number of characters this string will be long. A
     *            negative number or zero will lead to "" be returned.
     *
     * @throws NullPointerException
     *             if authorField is null and n > 0
     */
    public static String authIniN(String authorField, int n) {

        if (n <= 0) {
            return "";
        }

        String fixedAuthorField = AuthorList.fixAuthorForAlphabetization(authorField);
        StringBuilder author = new StringBuilder();
        String[] tokens = fixedAuthorField.split("\\band\\b");

        if (tokens.length == 0) {
            return author.toString();
        }

        int i = 0;
        int charsAll = n / tokens.length;
        while (tokens.length > i) {
            if (i < (n % tokens.length)) {
                author.append(authNofMth(fixedAuthorField, charsAll + 1, i + 1));
            } else {
                author.append(authNofMth(fixedAuthorField, charsAll, i + 1));
            }
            i++;
        }

        if (author.length() <= n) {
            return author.toString();
        } else {
            return author.toString().substring(0, n);
        }
    }

    /**
     * Split the pages field into separate numbers and return the lowest
     *
     * @param pages
     *            (may not be null) a pages string such as 42--111 or
     *            7,41,73--97 or 43+
     *
     * @return the first page number or "" if no number is found in the string
     *
     * @throws NullPointerException
     *             if pages is null
     */
    public static String firstPage(String pages) {
        // FIXME: incorrectly exracts the first page when pages are
        // specified with ellipse, e.g. "213-6", which should stand
        // for "213-216". S.G.
        final String[] splitPages = pages.split("\\D+");
        int result = Integer.MAX_VALUE;
        for (String n : splitPages) {
            if (n.matches("\\d+")) {
                result = Math.min(Integer.parseInt(n), result);
            }
        }

        if (result == Integer.MAX_VALUE) {
            return "";
        } else {
            return String.valueOf(result);
        }
    }

    /**
     * Return the non-digit prefix of pages
     *
     * @param pages
     *            a pages string such as L42--111 or L7,41,73--97 or L43+
     *
     * @return the non-digit prefix of pages (like "L" of L7)
     *         or "" if no non-digit prefix is found in the string
     *
     * @throws NullPointerException
     *             if pages is null.
     */
    public static String pagePrefix(String pages) {
        if (pages.matches("^\\D+.*$")) {
            return (pages.split("\\d+"))[0];
        } else {
            return "";
        }
    }

    /**
     * Split the pages field into separate numbers and return the highest
     *
     * @param pages
     *            a pages string such as 42--111 or 7,41,73--97 or 43+
     *
     * @return the first page number or "" if no number is found in the string
     *
     * @throws NullPointerException
     *             if pages is null.
     */
    public static String lastPage(String pages) {
        final String[] splitPages = pages.split("\\D+");
        int result = Integer.MIN_VALUE;
        for (String n : splitPages) {
            if (n.matches("\\d+")) {
                result = Math.max(Integer.parseInt(n), result);
            }
        }

        if (result == Integer.MIN_VALUE) {
            return "";
        } else {
            return String.valueOf(result);
        }
    }

    /**
     * Parse a field marker with modifiers, possibly containing a parenthesised modifier,
     * as well as escaped colons and parentheses.
     * @param arg The argument string.
     * @return An array of strings representing the parts of the marker
     */
    private static List<String> parseFieldMarker(String arg) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        int inParenthesis = 0;
        for (int i = 0; i < arg.length(); i++) {
            char currentChar = arg.charAt(i);
            if ((currentChar == ':') && !escaped && (inParenthesis == 0)) {
                parts.add(current.toString());
                current = new StringBuilder();
            } else if ((currentChar == '(') && !escaped) {
                inParenthesis++;
                current.append(currentChar);
            } else if ((currentChar == ')') && !escaped && (inParenthesis > 0)) {
                inParenthesis--;
                current.append(currentChar);
            } else if (currentChar == '\\') {
                if (escaped) {
                    escaped = false;
                    current.append(currentChar);
                } else {
                    escaped = true;
                }
            } else if (escaped) {
                current.append(currentChar);
                escaped = false;
            } else {
                current.append(currentChar);
            }
        }
        parts.add(current.toString());
        return parts;
    }


    /**
     * This method returns a String similar to the one passed in, except that it is molded into a form that is
     * acceptable for bibtex.
     * <p>
     * Watch-out that the returned string might be of length 0 afterwards.
     *
     * @param key             mayBeNull
     * @param enforceLegalKey make sure that the key is legal in all respects
     */
    public static String checkLegalKey(String key, boolean enforceLegalKey) {
        if (key == null) {
            return null;
        }
        if (!enforceLegalKey) {
            // User doesn't want us to enforce legal characters. We must still look
            // for whitespace and some characters such as commas, since these would
            // interfere with parsing:
            StringBuilder newKey = new StringBuilder();
            for (int i = 0; i < key.length(); i++) {
                char c = key.charAt(i);
                if (!Character.isWhitespace(c) && ("{}(),\\\"".indexOf(c) == -1)) {
                    newKey.append(c);
                }
            }
            return newKey.toString();
        }

        StringBuilder newKey = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (!Character.isWhitespace(c) && ("{}(),\\\"#~^'".indexOf(c) == -1)) {
                newKey.append(c);
            }
        }

        // Replace non-English characters like umlauts etc. with a sensible
        // letter or letter combination that bibtex can accept.

        return StringUtil.replaceSpecialCharacters(newKey.toString());
    }

}
