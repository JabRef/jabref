package org.jabref.logic.bibtexkeypattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.formatter.Formatters;
import org.jabref.logic.formatter.casechanger.Word;
import org.jabref.logic.layout.format.RemoveLatexCommandsFormatter;
import org.jabref.model.cleanup.Formatter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The BracketedExpressionExpander provides methods to expand bracketed expressions,
 * such as [year]_[author]_[firstpage], using information from a provided BibEntry.
 * The above-mentioned expression would yield 2017_Kitsune_123 when expanded using the
 * BibTeX entry "@Article{ authors = {O. Kitsune}, year = {2017}, pages={123-6}}".
 */
public class BracketedPattern {

    private static final Logger LOGGER = LoggerFactory.getLogger(BracketedPattern.class);

    private static final String STARTING_CAPITAL_PATTERN = "[^A-Z]";
    private static final int CHARS_OF_FIRST = 5;
    private static final Pattern REGEX_PATTERN = Pattern.compile(".*\\(\\{([A-Z]+)\\}\\).*");

    private final String pattern;

    public BracketedPattern() {
        this.pattern = null;
    }

    /**
     * @param bibentry
     */
    public BracketedPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[pattern=" + pattern + "]";
    }

    public String expand(BibEntry bibentry) {
        BibDatabase null_database = null;
        return expand(bibentry, null_database);
    }

    /**
     * Expands the current pattern using the given bibentry and database. ";" is used as keyword delimiter.
     *
     * @param bibentry The bibentry to expand.
     * @param database The database to use for string-lookups and cross-refs. May be null.
     *
     * @return The expanded pattern. The empty string is returned, if it could not be expanded.
     */
    public String expand(BibEntry bibentry, BibDatabase database) {
        Objects.requireNonNull(bibentry);
        Character keywordDelimiter = ';';
        return expand(bibentry, keywordDelimiter, database);
    }

    /**
     * Expands the current pattern using the given bibentry, keyword delimiter, and database.
     *
     * @param bibentry The bibentry to expand.
     * @param keywordDelimiter The keyword delimiter to use.
     * @param database The database to use for string-lookups and cross-refs. May be null.
     *
     * @return The expanded pattern. The empty string is returned, if it could not be expanded.
     */
    public String expand(BibEntry bibentry, Character keywordDelimiter, BibDatabase database) {
        Objects.requireNonNull(bibentry);
        return expandBrackets(this.pattern, keywordDelimiter, bibentry, database);
    }

    /**
     * Expands a pattern
     *
     * @param pattern The pattern to expand
     * @param keywordDelimiter The keyword delimiter to use
     * @param entry The bibentry to use for expansion
     * @param database The database for field resolving. May be null.
     * @return The expanded pattern. Not null.
     */
    public static String expandBrackets(String pattern, Character keywordDelimiter, BibEntry entry, BibDatabase database) {
        Objects.requireNonNull(pattern);
        Objects.requireNonNull(entry);
        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(pattern,"\\[]",true);

        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if ("\\".equals(token)) {
                if (st.hasMoreTokens()) {
                    sb.append(st.nextToken());
                }
                // FIXME: else -> raise exception or log? (S.G.)
            } else {
                if ("[".equals(token)) {
                    // Fetch the next token after the '[':
                    token = st.nextToken();
                    List<String> fieldParts = parseFieldMarker(token);
                    // check whether there is a modifier on the end such as
                    // ":lower":
                    if (fieldParts.size() <= 1) {
                        sb.append(getFieldValue(entry, token, keywordDelimiter, database));
                    } else {
                        // apply modifiers:
                        String fieldValue = getFieldValue(entry, fieldParts.get(0), keywordDelimiter, database);
                        sb.append(applyModifiers(fieldValue, fieldParts, 1));
                    }
                    // Fetch and discard the closing ']'
                    if (st.hasMoreTokens()) {
                        token = st.nextToken();
                    } else {
                        token = "";
                    }
                    if (!"]".equals(token)) {
                        LOGGER.warn("Missing closing bracket ']' in '" + pattern + "'");
                    }
                } else {
                    sb.append(token);
                }
            }
        }

        return sb.toString();
    }

    /**
     * Evaluates the given pattern ("value") to the given bibentry and database
     *
     * @param entry The entry to get the field value from
     * @param value A pattern string (such as auth, pureauth, authorLast)
     * @param keywordDelimiter The de
     * @param database The database to use for field resolving. May be null.
     *
     * @return String containing the evaluation result. Empty string if the pattern cannot be resolved.
     */
    public static String getFieldValue(BibEntry entry, String value, Character keywordDelimiter, BibDatabase database) {

        String val = value;
        try {
            if (val.startsWith("auth") || val.startsWith("pureauth")) {
                // result the author
                String authString;
                if (database != null) {
                    authString = entry.getResolvedFieldOrAlias(FieldName.AUTHOR, database)
                            .map(authorString -> normalize(database.resolveForStrings(authorString))).orElse("");
                } else {
                    authString = entry.getResolvedFieldOrAlias(FieldName.AUTHOR, database).orElse("");
                }

                if (val.startsWith("pure")) {
                    // "pure" is used in the context of authors to resolve to authors only and not fallback to editors
                    // The other functionality of the pattern "ForeIni", ... is the same
                    // Thus, remove the "pure" prefix so the remaining code in this section functions correctly
                    //
                    val = val.substring(4);
                } else {
                    // special feature: A pattern starting with "auth" falls back to the editor
                    if (authString.isEmpty()) {
                        if (database != null) {
                            authString = entry.getResolvedFieldOrAlias(FieldName.EDITOR, database)
                                    .map(authorString -> normalize(database.resolveForStrings(authorString))).orElse("");
                        } else {
                            authString = entry.getResolvedFieldOrAlias(FieldName.EDITOR, database).orElse("");
                        }
                    }
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
                    return entry.getResolvedFieldOrAlias(val, database).orElse("");
                }
            } else if (val.startsWith("ed")) {
                // Gather all markers starting with "ed" here, so we
                // don't have to check all the time.
                if ("edtr".equals(val)) {
                    return firstAuthor(entry.getResolvedFieldOrAlias(FieldName.EDITOR, database).orElse(""));
                } else if ("edtrForeIni".equals(val)) {
                    return firstAuthorForenameInitials(entry.getResolvedFieldOrAlias(FieldName.EDITOR, database).orElse(""));
                } else if ("editors".equals(val)) {
                    return allAuthors(entry.getResolvedFieldOrAlias(FieldName.EDITOR, database).orElse(""));
                    // Last author's last name
                } else if ("editorLast".equals(val)) {
                    return lastAuthor(entry.getResolvedFieldOrAlias(FieldName.EDITOR, database).orElse(""));
                } else if ("editorLastForeIni".equals(val)) {
                    return lastAuthorForenameInitials(entry.getResolvedFieldOrAlias(FieldName.EDITOR, database).orElse(""));
                } else if ("editorIni".equals(val)) {
                    return oneAuthorPlusIni(entry.getResolvedFieldOrAlias(FieldName.EDITOR, database).orElse(""));
                } else if (val.matches("edtrIni[\\d]+")) {
                    int num = Integer.parseInt(val.substring(7));
                    return authIniN(entry.getResolvedFieldOrAlias(FieldName.EDITOR, database).orElse(""), num);
                } else if (val.matches("edtr[\\d]+_[\\d]+")) {
                    String[] nums = val.substring(4).split("_");
                    return authNofMth(entry.getResolvedFieldOrAlias(FieldName.EDITOR, database).orElse(""),
                            Integer.parseInt(nums[0]),
                            Integer.parseInt(nums[1]) - 1);
                } else if ("edtr.edtr.ea".equals(val)) {
                    return authAuthEa(entry.getResolvedFieldOrAlias(FieldName.EDITOR, database).orElse(""));
                } else if ("edtrshort".equals(val)) {
                    return authshort(entry.getResolvedFieldOrAlias(FieldName.EDITOR, database).orElse(""));
                }
                // authN. First N chars of the first author's last
                // name.
                else if (val.matches("edtr\\d+")) {
                    String fa = firstAuthor(entry.getResolvedFieldOrAlias(FieldName.EDITOR, database).orElse(""));
                    int num = Integer.parseInt(val.substring(4));
                    if (num > fa.length()) {
                        num = fa.length();
                    }
                    return fa.substring(0, num);
                } else {
                    // This "ed" business was a dead end, so just
                    // use it literally:
                    return entry.getResolvedFieldOrAlias(val, database).orElse("");
                }
            } else if ("firstpage".equals(val)) {
                return firstPage(entry.getResolvedFieldOrAlias(FieldName.PAGES, database).orElse(""));
            } else if ("pageprefix".equals(val)) {
                return pagePrefix(entry.getResolvedFieldOrAlias(FieldName.PAGES, database).orElse(""));
            } else if ("lastpage".equals(val)) {
                return lastPage(entry.getResolvedFieldOrAlias(FieldName.PAGES, database).orElse(""));
            } else if ("title".equals(val)) {
                return camelizeSignificantWordsInTitle(entry.getResolvedFieldOrAlias(FieldName.TITLE, database).orElse(""));
            } else if ("fulltitle".equals(val)) {
                return entry.getResolvedFieldOrAlias(FieldName.TITLE, database).orElse("");
            } else if ("shorttitle".equals(val)) {
                return getTitleWords(3, entry.getResolvedFieldOrAlias(FieldName.TITLE, database).orElse(""));
            } else if ("shorttitleINI".equals(val)) {
                return keepLettersAndDigitsOnly(
                        applyModifiers(getTitleWordsWithSpaces(3, entry.getResolvedFieldOrAlias(FieldName.TITLE, database).orElse("")),
                                Collections.singletonList("abbr"), 0));
            } else if ("veryshorttitle".equals(val)) {
                return getTitleWords(1,
                        removeSmallWords(entry.getResolvedFieldOrAlias(FieldName.TITLE, database).orElse("")));
            } else if ("camel".equals(val)) {
                return getCamelizedTitle(entry.getResolvedFieldOrAlias(FieldName.TITLE, database).orElse(""));
            } else if ("shortyear".equals(val)) {
                String yearString = entry.getResolvedFieldOrAlias(FieldName.YEAR, database).orElse("");
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
            } else if ("entrytype".equals(val)) {
                return entry.getResolvedFieldOrAlias(BibEntry.TYPE_HEADER, database).orElse("");
            } else if (val.matches("keyword\\d+")) {
                // according to LabelPattern.php, it returns keyword number n
                int num = Integer.parseInt(val.substring(7));
                KeywordList separatedKeywords = entry.getResolvedKeywords(keywordDelimiter, database);
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
                KeywordList separatedKeywords = entry.getResolvedKeywords(keywordDelimiter, database);
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
                return entry.getResolvedFieldOrAlias(val, database).orElse("");
            }
        }
        catch (NullPointerException ex) {
            LOGGER.debug("Problem making expanding bracketed expression", ex);
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
    static String applyModifiers(final String label, final List<String> parts, final int offset) {
        String resultingLabel = label;
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
                resultingLabel = abbreviateSB.toString();
            } else {
                Optional<Formatter> formatter = Formatters.getFormatterForModifier(modifier);
                if (formatter.isPresent()) {
                    resultingLabel = formatter.get().format(resultingLabel);
                } else if (!modifier.isEmpty() && (modifier.length() >= 2) && (modifier.charAt(0) == '(') && modifier.endsWith(")")) {
                    // Alternate text modifier in parentheses. Should be inserted if the label is empty
                    if (label.isEmpty() && (modifier.length() > 2)) {
                        resultingLabel = modifier.substring(1, modifier.length() - 1);
                    }
                } else {
                    LOGGER.warn("Key generator warning: unknown modifier '" + modifier + "'.");
                }
            }
        }

        return resultingLabel;
    }

    /**
     * Determines "number" words out of the "title" field in the given BibTeX entry
     */
    public static String getTitleWords(int number, String title) {
        return getTitleWordsWithSpaces(number, title);
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
     *
     * @param arg The argument string.
     * @return An array of strings representing the parts of the marker
     */
    protected static List<String> parseFieldMarker(String arg) {
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

    private static String normalize(String content) {
        List<String> tokens = new ArrayList<>();
        int b = 0;
        StringBuilder and = new StringBuilder();
        StringBuilder token = new StringBuilder();
        for (int p = 0; p < content.length(); p++) {
            if (b == 0) {
                String andString = and.toString(); // Avoid lots of calls
                if (((andString.isEmpty()) && (content.charAt(p) == ' '))
                        || (" ".equals(andString) && (content.charAt(p) == 'a'))
                        || (" a".equals(andString) && (content.charAt(p) == 'n'))
                        || (" an".equals(andString) && (content.charAt(p) == 'd'))) {
                    and.append(content.charAt(p));
                } else if (" and".equals(and.toString()) && (content.charAt(p) == ' ')) {
                    and = new StringBuilder();
                    tokens.add(token.toString().trim());
                    token = new StringBuilder();
                } else {
                    if (content.charAt(p) == '{') {
                        b++;
                    }
                    if (content.charAt(p) == '}') {
                        b--;
                    }
                    token.append(and);
                    and = new StringBuilder();
                    token.append(content.charAt(p));
                }
            } else {
                token.append(content.charAt(p));
            }
        }
        tokens.add(token.toString());
        StringBuilder normalized = new StringBuilder("");

        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) {
                normalized.append(" and ");
            }

            normalized.append(isInstitution(tokens.get(i)) ? generateInstitutionKey(tokens.get(i)) : removeDiacritics(
                    tokens.get(i)));
        }
        return normalized.toString();
    }

    /**
     * Will remove diacritics from the content.
     *
     * Replaces umlaut: \"x with xe, e.g. \"o -> oe, \"u -> ue, etc.
     * Removes all other diacritics: \?x -> x, e.g. \'a -> a, etc.
     *
     * @param content The content.
     * @return The content without diacritics.
     */
    private static String removeDiacritics(String content) {
        if (content.isEmpty()) {
            return content;
        }

        String result = content;
        // Replace umlaut with '?e'
        result = result.replaceAll("\\{\\\\\"([a-zA-Z])\\}", "$1e");
        result = result.replaceAll("\\\\\"\\{([a-zA-Z])\\}", "$1e");
        result = result.replaceAll("\\\\\"([a-zA-Z])", "$1e");
        // Remove diacritics
        result = result.replaceAll("\\{\\\\.([a-zA-Z])\\}", "$1");
        result = result.replaceAll("\\\\.\\{([a-zA-Z])\\}", "$1");
        result = result.replaceAll("\\\\.([a-zA-Z])", "$1");
        return result;
    }

    /**
     * Unifies umlauts.
     *
     * Replaces: $\ddot{\mathrm{X}}$ (an alternative umlaut) with: {\"X}
     * Replaces: \?{X} and \?X with {\?X}, where ? is a diacritic symbol
     *
     * @param content The content.
     * @return The content with unified diacritics.
     */
    private static String unifyDiacritics(String content) {
        return content.replaceAll(
                "\\$\\\\ddot\\{\\\\mathrm\\{([^\\}])\\}\\}\\$",
                "{\\\"$1}").replaceAll(
                        "(\\\\[^\\-a-zA-Z])\\{?([a-zA-Z])\\}?",
                        "{$1$2}");
    }

    /**
     * Check if a value is institution.
     *
     * This is usable for distinguishing between persons and institutions in
     * the author or editor fields.
     *
     * A person:
     *   - "John Doe"
     *   - "Doe, John"
     *
     * An institution:
     *   - "{The Big Company or Institution Inc.}"
     *   - "{The Big Company or Institution Inc. (BCI)}"
     *
     * @param author Author or editor.
     * @return True if the author or editor is an institution.
     */
    private static boolean isInstitution(String author) {
        return StringUtil.isInCurlyBrackets(author);
    }

    /**
     * <p>
     * An author or editor may be and institution not a person. In that case the
     * key generator builds very long keys, e.g.: for &ldquo;The Attributed
     * Graph Grammar System (AGG)&rdquo; ->
     * &ldquo;TheAttributedGraphGrammarSystemAGG&rdquo;.
     * </p>
     *
     * <p>
     * An institution name should be inside <code>{}</code> brackets. If the
     * institution name also includes its abbreviation this abbreviation should
     * be also in <code>{}</code> brackets. For the previous example the value
     * should look like:
     * <code>{The Attributed Graph Grammar System ({AGG})}</code>.
     * </p>
     *
     * <p>
     * If an institution includes its abbreviation, i.e. "...({XYZ})", first
     * such abbreviation should be used as the key value part of such author.
     * </p>
     *
     * <p>
     * If an institution does not include its abbreviation the key should be
     * generated form its name in the following way:
     * </p>
     *
     * <p>
     * The institution value can contain: institution name, part of the
     * institution, address, etc. Those information should be separated by
     * comma. Name of the institution and possible part of the institution
     * should be on the beginning, while address and secondary information
     * should be on the end.
     * </p>
     *
     * Each part is examined separately:
     * <ol>
     * <li>We remove all tokens of a part which are one of the defined ignore
     * words (the, press), which end with a dot (ltd., co., ...) and which first
     * character is lowercase (of, on, di, ...).</li>
     * <li>We detect a type of the part: university, technology institute,
     * department, school, rest
     * <ul>
     * <li>University: <code>"Uni[NameOfTheUniversity]"</code></li>
     * <li>Department: will be an abbreviation of all words beginning with the
     * uppercase letter except of words: <code>d[ei]p.*</code>, school,
     * faculty</li>
     * <li>School: same as department</li>
     * <li>Rest: If there are less than 3 tokens in such part than the result
     * will be by concatenating those tokens, otherwise the result will be build
     * from the first letters of words starting with and uppercase letter.</li>
     * </ul>
     * </ol>
     *
     * Parts are concatenated together in the following way:
     * <ul>
     * <li>If there is a university part use it otherwise use the rest part.</li>
     * <li>If there is a school part append it.</li>
     * <li>If there is a department part and it is not same as school part
     * append it.</li>
     * </ul>
     *
     * Rest part is only the first part which do not match any other type. All
     * other parts (address, ...) are ignored.
     *
     * @param content the institution to generate a Bibtex key for
     * @return <ul>
     *         <li>the institution key</li>
     *         <li>"" in the case of a failure</li>
     *         <li>null if content is null</li>
     *         </ul>
     */
    private static String generateInstitutionKey(String content) {
        if (content.isEmpty()) {
            return content;
        }

        String result = content;
        result = unifyDiacritics(result);
        result = result.replaceAll("^\\{", "").replaceAll("\\}$", "");
        Matcher matcher = REGEX_PATTERN.matcher(result);
        if (matcher.matches()) {
            return matcher.group(1);
        }

        result = removeDiacritics(result);
        String[] parts = result.split(",");

        // Key parts
        String university = null;
        String department = null;
        String school = null;
        String rest = null;

        List<String> ignore = Arrays.asList("press", "the");
        for (int index = 0; index < parts.length; index++) {
            List<String> part = new ArrayList<>();

            // Cleanup: remove unnecessary words.
            for (String k : parts[index].replaceAll("\\{[A-Z]+\\}", "").split("[ \\-_]")) {
                if ((!(k.isEmpty()) // remove empty
                        && !ignore.contains(k.toLowerCase(Locale.ENGLISH)) // remove ignored words
                        && (k.charAt(k.length() - 1) != '.')
                        && (String.valueOf(k.charAt(0))).matches("[A-Z]"))
                        || ((k.length() >= 3) && "uni".equalsIgnoreCase(k.substring(0, 2)))) {
                    part.add(k);
                }
            }

            boolean isUniversity = false; // university
            boolean isTechnology = false; // technology institute
            boolean isDepartment = false; // departments
            boolean isSchool = false; // schools

            // Deciding about a part type...
            for (String k : part) {
                if (k.matches("^[Uu][Nn][Ii].*")) { // Starts with "uni" case and locale independent
                    isUniversity = true;
                }
                if (k.matches("^[Tt][Ee][Cc][Hh].*")) { // Starts with "tech" case and locale independent
                    isTechnology = true;
                }
                if (FieldName.SCHOOL.equalsIgnoreCase(k)) {
                    isSchool = true;
                }
                if (k.matches("^[Dd][EeIi][Pp].*") || k.matches("^[Ll][Aa][Bb].*")) { // Starts with "dep"/"dip"/"lab", case and locale independent
                    isDepartment = true;
                }
            }
            if (isTechnology) {
                isUniversity = false; // technology institute isn't university :-)
            }

            // University part looks like: Uni[NameOfTheUniversity]
            //
            // If university is detected than the previous part is suggested
            // as department
            if (isUniversity) {
                StringBuilder universitySB = new StringBuilder();
                universitySB.append("Uni");
                for (String k : part) {
                    if (!k.matches("^[Uu][Nn][Ii].*")) {
                        universitySB.append(k);
                    }
                }
                university = universitySB.toString();
                if ((index > 0) && (department == null)) {
                    department = parts[index - 1];
                }

                // School is an abbreviation of all the words beginning with a
                // capital letter excluding: department, school and faculty words.
                //
                // Explicitly defined department part is build the same way as
                // school
            } else if (isSchool || isDepartment) {
                StringBuilder schoolSB = new StringBuilder();
                StringBuilder departmentSB = new StringBuilder();
                for (String k : part) {
                    if (!k.matches("^[Dd][EeIi][Pp].*") && !FieldName.SCHOOL.equalsIgnoreCase(k)
                            && !"faculty".equalsIgnoreCase(k)
                            && !(k.replaceAll(STARTING_CAPITAL_PATTERN, "").isEmpty())) {
                        if (isSchool) {
                            schoolSB.append(k.replaceAll(STARTING_CAPITAL_PATTERN, ""));
                        }
                        if (isDepartment) {
                            departmentSB.append(k.replaceAll(STARTING_CAPITAL_PATTERN, ""));
                        }
                    }
                }
                if (isSchool) {
                    school = schoolSB.toString();
                }
                if (isDepartment) {
                    department = departmentSB.toString();
                }
                // A part not matching university, department nor school.
            } else if (rest == null) {
                StringBuilder restSB = new StringBuilder();
                // Less than 3 parts -> concatenate those
                if (part.size() < 3) {
                    for (String k : part) {
                        restSB.append(k);
                        // More than 3 parts -> use 1st letter abbreviation
                    }
                } else {
                    for (String k : part) {
                        k = k.replaceAll(STARTING_CAPITAL_PATTERN, "");
                        if (!(k.isEmpty())) {
                            restSB.append(k);
                        }
                    }
                }
                rest = restSB.toString();
            }
        }

        // Putting parts together.
        return (university == null ? rest : university)
                + (school == null ? "" : school)
                + ((department == null)
                        || ((school != null) && department.equals(school)) ? "" : department);
    }
}
