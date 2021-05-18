package org.jabref.logic.citationkeypattern;

import java.math.BigInteger;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.formatter.Formatters;
import org.jabref.logic.formatter.casechanger.Word;
import org.jabref.logic.layout.format.RemoveLatexCommandsFormatter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.LatexToUnicodeAdapter;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The BracketedExpressionExpander provides methods to expand bracketed expressions, such as
 * [year]_[author]_[firstpage], using information from a provided BibEntry. The above-mentioned expression would yield
 * 2017_Kitsune_123 when expanded using the BibTeX entry "@Article{ authors = {O. Kitsune}, year = {2017},
 * pages={123-6}}".
 */
public class BracketedPattern {
    private static final Logger LOGGER = LoggerFactory.getLogger(BracketedPattern.class);

    /**
     * The maximum number of characters in the first author's last name.
     */
    private static final int CHARS_OF_FIRST = 5;
    /**
     * The maximum number of name abbreviations that can be used. If there are more authors, {@code MAX_ALPHA_AUTHORS -
     * 1} name abbreviations will be displayed, and a + sign will be appended at the end.
     */
    private static final int MAX_ALPHA_AUTHORS = 4;

    /**
     * Matches everything that is not a unicode decimal digit.
     */
    private static final Pattern NOT_DECIMAL_DIGIT = Pattern.compile("\\P{Nd}");
    /**
     * Matches everything that is not an uppercase ASCII letter. The intended use is to remove all lowercase letters
     */
    private static final Pattern NOT_CAPITAL_CHARACTER = Pattern.compile("[^A-Z]");
    /**
     * Matches uppercase english letters between "({" and "})", which should be used to abbreviate the name of an institution
     */
    private static final Pattern INLINE_ABBREVIATION = Pattern.compile("(?<=\\(\\{)[A-Z]+(?=}\\))");
    /**
     * Matches with "dep"/"dip", case insensitive
     */
    private static final Pattern DEPARTMENTS = Pattern.compile("^d[ei]p.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHITESPACE = Pattern.compile("\\p{javaWhitespace}");

    private enum Institution {
        SCHOOL,
        DEPARTMENT,
        UNIVERSITY,
        TECHNOLOGY;

        /**
         * Matches "uni" followed by "v" or "b", at the start of a string or after a space, case insensitive
         */
        private static final Pattern UNIVERSITIES = Pattern.compile("^uni(v|b|$).*", Pattern.CASE_INSENSITIVE);
        /**
         * Matches with "tech", case insensitive
         */
        private static final Pattern TECHNOLOGICAL_INSTITUTES = Pattern.compile("^tech.*", Pattern.CASE_INSENSITIVE);
        /**
         * Matches with "dep"/"dip"/"lab", case insensitive
         */
        private static final Pattern DEPARTMENTS_OR_LABS = Pattern.compile("^(d[ei]p|lab).*", Pattern.CASE_INSENSITIVE);

        /**
         * Find which types of institutions have words in common with the given name parts.
         *
         * @param nameParts a list of words that constitute parts of an institution's name.
         * @return set containing all types that matches
         */
        public static EnumSet<Institution> findTypes(List<String> nameParts) {
            EnumSet<Institution> parts = EnumSet.noneOf(Institution.class);
            // Deciding about a part type…
            for (String namePart : nameParts) {
                if (UNIVERSITIES.matcher(namePart).matches()) {
                    parts.add(Institution.UNIVERSITY);
                } else if (TECHNOLOGICAL_INSTITUTES.matcher(namePart).matches()) {
                    parts.add(Institution.TECHNOLOGY);
                } else if (StandardField.SCHOOL.getName().equalsIgnoreCase(namePart)) {
                    parts.add(Institution.SCHOOL);
                } else if (DEPARTMENTS_OR_LABS.matcher(namePart).matches()) {
                    parts.add(Institution.DEPARTMENT);
                }
            }

            if (parts.contains(Institution.TECHNOLOGY)) {
                parts.remove(Institution.UNIVERSITY); // technology institute isn't university :-)
            }

            return parts;
        }
    }

    private final String pattern;

    public BracketedPattern() {
        this.pattern = null;
    }

    public BracketedPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[pattern=" + pattern + "]";
    }

    public String expand(BibEntry bibentry) {
        return expand(bibentry, null);
    }

    /**
     * Expands the current pattern using the given bibentry and database. ";" is used as keyword delimiter.
     *
     * @param bibentry The bibentry to expand.
     * @param database The database to use for string-lookups and cross-refs. May be null.
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
     * @param bibentry         The bibentry to expand.
     * @param keywordDelimiter The keyword delimiter to use.
     * @param database         The database to use for string-lookups and cross-refs. May be null.
     * @return The expanded pattern. The empty string is returned, if it could not be expanded.
     */
    public String expand(BibEntry bibentry, Character keywordDelimiter, BibDatabase database) {
        Objects.requireNonNull(bibentry);
        return expandBrackets(this.pattern, keywordDelimiter, bibentry, database);
    }

    /**
     * Expands a pattern
     *
     * @param pattern          The pattern to expand
     * @param keywordDelimiter The keyword delimiter to use
     * @param entry            The bibentry to use for expansion
     * @param database         The database for field resolving. May be null.
     * @return The expanded pattern. Not null.
     */
    public static String expandBrackets(String pattern, Character keywordDelimiter, BibEntry entry, BibDatabase database) {
        Objects.requireNonNull(pattern);
        Objects.requireNonNull(entry);
        return expandBrackets(pattern, expandBracketContent(keywordDelimiter, entry, database));
    }

    /**
     * Utility method creating a function taking the string representation of the content of a bracketed expression and
     * expanding it.
     *
     * @param keywordDelimiter The keyword delimiter to use
     * @param entry            The {@link BibEntry} to use for expansion
     * @param database         The {@link BibDatabase} for field resolving. May be null.
     * @return a function accepting a bracketed expression and returning the result of expanding it
     */
    public static Function<String, String> expandBracketContent(Character keywordDelimiter, BibEntry entry, BibDatabase database) {
        return (String bracket) -> {
            String expandedPattern;
            List<String> fieldParts = parseFieldAndModifiers(bracket);
            // check whether there is a modifier on the end such as
            // ":lower":
            expandedPattern = getFieldValue(entry, fieldParts.get(0), keywordDelimiter, database);
            if (fieldParts.size() > 1) {
                // apply modifiers:
                expandedPattern = applyModifiers(expandedPattern, fieldParts, 1, expandBracketContent(keywordDelimiter, entry, database));
            }
            return expandedPattern;
        };
    }

    /**
     * Expands a pattern.
     *
     * @param pattern               The pattern to expand
     * @param bracketContentHandler A function taking the string representation of the content of a bracketed pattern
     *                              and expanding it
     * @return The expanded pattern. Not null.
     */
    public static String expandBrackets(String pattern, Function<String, String> bracketContentHandler) {
        Objects.requireNonNull(pattern);
        StringBuilder expandedPattern = new StringBuilder();
        StringTokenizer parsedPattern = new StringTokenizer(pattern, "\\[]\"", true);

        while (parsedPattern.hasMoreTokens()) {
            String token = parsedPattern.nextToken();
            switch (token) {
                case "\"" -> appendQuote(expandedPattern, parsedPattern);
                case "[" -> {
                    String fieldMarker = contentBetweenBrackets(parsedPattern, pattern);
                    expandedPattern.append(bracketContentHandler.apply(fieldMarker));
                }
                case "\\" -> {
                    if (parsedPattern.hasMoreTokens()) {
                        expandedPattern.append(parsedPattern.nextToken());
                    } else {
                        LOGGER.warn("Found a \"\\\" that is not part of an escape sequence");
                    }
                }
                default -> expandedPattern.append(token);
            }
        }

        return expandedPattern.toString();
    }

    /**
     * Returns the content enclosed between brackets, including enclosed quotes, and excluding the paired enclosing brackets.
     * There may be brackets in it.
     * Intended to be used by {@link BracketedPattern#expandBrackets(String, Character, BibEntry, BibDatabase)} when a [
     * is encountered, and has been consumed, by the {@code StringTokenizer}.
     *
     * @param pattern   pattern used by {@code expandBrackets}, used for logging
     * @param tokenizer the tokenizer producing the tokens
     * @return the content enclosed by brackets
     */
    private static String contentBetweenBrackets(StringTokenizer tokenizer, final String pattern) {
        StringBuilder bracketContent = new StringBuilder();
        boolean foundClosingBracket = false;
        int subBrackets = 0;
        // make sure to read until the paired ']'
        while (tokenizer.hasMoreTokens() && !foundClosingBracket) {
            String token = tokenizer.nextToken();
            // If the beginning of a quote is found, append the content
            switch (token) {
                case "\"" -> appendQuote(bracketContent, tokenizer);
                case "]" -> {
                    if (subBrackets == 0) {
                        foundClosingBracket = true;
                    } else {
                        subBrackets--;
                        bracketContent.append(token);
                    }
                }
                case "[" -> {
                    subBrackets++;
                    bracketContent.append(token);
                }
                default -> bracketContent.append(token);
            }
        }

        if (!foundClosingBracket) {
            LOGGER.warn("Missing closing bracket ']' in '{}'", pattern);
        } else if (bracketContent.length() == 0) {
            LOGGER.warn("Found empty brackets \"[]\" in '{}'", pattern);
        }
        return bracketContent.toString();
    }

    /**
     * Appends the content between, and including, two \" to the provided <code>StringBuilder</code>. Intended to be
     * used by {@link BracketedPattern#expandBrackets(String, Character, BibEntry, BibDatabase)} when a \" is
     * encountered by the StringTokenizer.
     *
     * @param stringBuilder the <code>StringBuilder</code> to which tokens will be appended
     * @param tokenizer     the tokenizer producing the tokens
     */
    private static void appendQuote(StringBuilder stringBuilder, StringTokenizer tokenizer) {
        stringBuilder.append("\"");  // We know that the previous token was \"
        String token = "";
        while (tokenizer.hasMoreTokens() && !"\"".equals(token)) {
            token = tokenizer.nextToken();
            stringBuilder.append(token);
        }
    }

    /**
     * Evaluates the given pattern to the given bibentry and database
     *
     * @param entry            The entry to get the field value from
     * @param pattern          A pattern string (such as auth, pureauth, authorLast)
     * @param keywordDelimiter The de
     * @param database         The database to use for field resolving. May be null.
     * @return String containing the evaluation result. Empty string if the pattern cannot be resolved.
     */
    public static String getFieldValue(BibEntry entry, String pattern, Character keywordDelimiter, BibDatabase database) {
        try {
            if (pattern.startsWith("auth") || pattern.startsWith("pureauth")) {
                // result the author
                String unparsedAuthors = entry.getResolvedFieldOrAlias(StandardField.AUTHOR, database).orElse("");

                if (pattern.startsWith("pure")) {
                    // "pure" is used in the context of authors to resolve to authors only and not fallback to editors
                    // The other functionality of the pattern "ForeIni", ... is the same
                    // Thus, remove the "pure" prefix so the remaining code in this section functions correctly
                    //
                    pattern = pattern.substring(4);
                } else if (unparsedAuthors.isEmpty()) {
                    // special feature: A pattern starting with "auth" falls back to the editor
                    unparsedAuthors = entry.getResolvedFieldOrAlias(StandardField.EDITOR, database).orElse("");
                }

                AuthorList authorList = createAuthorList(unparsedAuthors);

                // Gather all author-related checks, so we don't
                // have to check all the time.
                switch (pattern) {
                    case "auth":
                        return firstAuthor(authorList);
                    case "authForeIni":
                        return firstAuthorForenameInitials(authorList);
                    case "authFirstFull":
                        return firstAuthorVonAndLast(authorList);
                    case "authors":
                        return allAuthors(authorList);
                    case "authorsAlpha":
                        return authorsAlpha(authorList);
                    case "authorLast":
                        return lastAuthor(authorList);
                    case "authorLastForeIni":
                        return lastAuthorForenameInitials(authorList);
                    case "authorIni":
                        return oneAuthorPlusInitials(authorList);
                    case "auth.auth.ea":
                        return authAuthEa(authorList);
                    case "auth.etal":
                        return authEtal(authorList, ".", ".etal");
                    case "authEtAl":
                        return authEtal(authorList, "", "EtAl");
                    case "authshort":
                        return authshort(authorList);
                }

                if (pattern.matches("authIni[\\d]+")) {
                    int num = Integer.parseInt(pattern.substring(7));
                    return authIniN(authorList, num);
                } else if (pattern.matches("auth[\\d]+_[\\d]+")) {
                    String[] nums = pattern.substring(4).split("_");
                    return authNofMth(authorList, Integer.parseInt(nums[0]),
                            Integer.parseInt(nums[1]));
                } else if (pattern.matches("auth\\d+")) {
                    // authN. First N chars of the first author's last name.
                    int num = Integer.parseInt(pattern.substring(4));
                    return authN(authorList, num);
                } else if (pattern.matches("authors\\d+")) {
                    return nAuthors(authorList, Integer.parseInt(pattern.substring(7)));
                } else {
                    // This "auth" business was a dead end, so just
                    // use it literally:
                    return entry.getResolvedFieldOrAlias(FieldFactory.parseField(pattern), database).orElse("");
                }
            } else if (pattern.startsWith("ed")) {
                // Gather all markers starting with "ed" here, so we
                // don't have to check all the time.
                String unparsedEditors = entry.getResolvedFieldOrAlias(StandardField.EDITOR, database).orElse("");
                AuthorList editorList = createAuthorList(unparsedEditors);

                switch (pattern) {
                    case "edtr":
                        return firstAuthor(editorList);
                    case "edtrForeIni":
                        return firstAuthorForenameInitials(editorList);
                    case "editors":
                        return allAuthors(editorList);
                    case "editorLast":
                        return lastAuthor(editorList); // Last author's last name
                    case "editorLastForeIni":
                        return lastAuthorForenameInitials(editorList);
                    case "editorIni":
                        return oneAuthorPlusInitials(editorList);
                    case "edtr.edtr.ea":
                        return authAuthEa(editorList);
                    case "edtrshort":
                        return authshort(editorList);
                }

                if (pattern.matches("edtrIni[\\d]+")) {
                    int num = Integer.parseInt(pattern.substring(7));
                    return authIniN(editorList, num);
                } else if (pattern.matches("edtr[\\d]+_[\\d]+")) {
                    String[] nums = pattern.substring(4).split("_");
                    return authNofMth(editorList,
                            Integer.parseInt(nums[0]),
                            Integer.parseInt(nums[1]) - 1);
                } else if (pattern.matches("edtr\\d+")) {
                    String fa = firstAuthor(editorList);
                    int num = Integer.parseInt(pattern.substring(4));
                    if (num > fa.length()) {
                        num = fa.length();
                    }
                    return fa.substring(0, num);
                } else {
                    // This "ed" business was a dead end, so just
                    // use it literally:
                    return entry.getResolvedFieldOrAlias(FieldFactory.parseField(pattern), database).orElse("");
                }
            } else if ("firstpage".equals(pattern)) {
                return firstPage(entry.getResolvedFieldOrAlias(StandardField.PAGES, database).orElse(""));
            } else if ("pageprefix".equals(pattern)) {
                return pagePrefix(entry.getResolvedFieldOrAlias(StandardField.PAGES, database).orElse(""));
            } else if ("lastpage".equals(pattern)) {
                return lastPage(entry.getResolvedFieldOrAlias(StandardField.PAGES, database).orElse(""));
            } else if ("title".equals(pattern)) {
                return camelizeSignificantWordsInTitle(entry.getResolvedFieldOrAlias(StandardField.TITLE, database).orElse(""));
            } else if ("fulltitle".equals(pattern)) {
                return entry.getResolvedFieldOrAlias(StandardField.TITLE, database).orElse("");
            } else if ("shorttitle".equals(pattern)) {
                return getTitleWords(3,
                        removeSmallWords(entry.getResolvedFieldOrAlias(StandardField.TITLE, database).orElse("")));
            } else if ("shorttitleINI".equals(pattern)) {
                return keepLettersAndDigitsOnly(
                        applyModifiers(getTitleWordsWithSpaces(3, entry.getResolvedFieldOrAlias(StandardField.TITLE, database).orElse("")),
                                Collections.singletonList("abbr"), 0, Function.identity()));
            } else if ("veryshorttitle".equals(pattern)) {
                return getTitleWords(1,
                        removeSmallWords(entry.getResolvedFieldOrAlias(StandardField.TITLE, database).orElse("")));
            } else if ("camel".equals(pattern)) {
                return getCamelizedTitle(entry.getResolvedFieldOrAlias(StandardField.TITLE, database).orElse(""));
            } else if ("shortyear".equals(pattern)) {
                String yearString = entry.getResolvedFieldOrAlias(StandardField.YEAR, database).orElse("");
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
            } else if ("entrytype".equals(pattern)) {
                return entry.getResolvedFieldOrAlias(InternalField.TYPE_HEADER, database).orElse("");
            } else if (pattern.matches("keyword\\d+")) {
                // according to LabelPattern.php, it returns keyword number n
                int num = Integer.parseInt(pattern.substring(7));
                KeywordList separatedKeywords = entry.getResolvedKeywords(keywordDelimiter, database);
                if (separatedKeywords.size() < num) {
                    // not enough keywords
                    return "";
                } else {
                    // num counts from 1 to n, but index in arrayList count from 0 to n-1
                    return separatedKeywords.get(num - 1).toString();
                }
            } else if (pattern.matches("keywords\\d*")) {
                // return all keywords, not separated
                int num;
                if (pattern.length() > 8) {
                    num = Integer.parseInt(pattern.substring(8));
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
                return entry.getResolvedFieldOrAlias(FieldFactory.parseField(pattern), database).orElse("");
            }
        } catch (NullPointerException ex) {
            LOGGER.debug("Problem making expanding bracketed expression", ex);
            return "";
        }
    }

    /**
     * Parses the provided string to an {@link AuthorList}, which are then formatted by {@link LatexToUnicodeAdapter}.
     * Afterward, any institutions are formatted into an institution key.
     *
     * @param unparsedAuthors a string representation of authors or editors
     * @return an {@link AuthorList} consisting of authors and institution keys with resolved latex.
     */
    private static AuthorList createAuthorList(String unparsedAuthors) {
        return AuthorList.parse(unparsedAuthors).getAuthors().stream()
                         .map((author) -> {
                             // If the author is an institution, use an institution key instead of the full name
                             String lastName = author.getLast()
                                                     .map(lastPart -> isInstitution(author) ?
                                                             generateInstitutionKey(lastPart) :
                                                             LatexToUnicodeAdapter.format(lastPart))
                                                     .orElse(null);
                             return new Author(
                                     author.getFirst().map(LatexToUnicodeAdapter::format).orElse(null),
                                     author.getFirstAbbr().map(LatexToUnicodeAdapter::format).orElse(null),
                                     author.getVon().map(LatexToUnicodeAdapter::format).orElse(null),
                                     lastName,
                                     author.getJr().map(LatexToUnicodeAdapter::format).orElse(null));
                         })
                         .collect(AuthorList.collect());
    }

    /**
     * Checks if an author is an institution which can get a citation key from {@link #generateInstitutionKey(String)}.
     *
     * @param author the checked author
     * @return true if only the last name is present and it contains at least one whitespace character.
     */
    private static boolean isInstitution(Author author) {
        return author.getFirst().isEmpty() && author.getFirstAbbr().isEmpty() && author.getJr().isEmpty()
                && author.getVon().isEmpty() && author.getLast().isPresent()
                && WHITESPACE.matcher(author.getLast().get()).find();
    }

    /**
     * Applies modifiers to a label generated based on a field marker.
     *
     * @param label  The generated label.
     * @param parts  String array containing the modifiers.
     * @param offset The number of initial items in the modifiers array to skip.
     * @param expandBracketContent a function to expand the content in the parentheses.
     * @return The modified label.
     */
    static String applyModifiers(final String label, final List<String> parts, final int offset, Function<String, String> expandBracketContent) {
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
                        resultingLabel = expandBrackets(modifier.substring(1, modifier.length() - 1), expandBracketContent);
                    }
                } else {
                    LOGGER.warn("Key generator warning: unknown modifier '{}'.", modifier);
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

        try (Scanner titleScanner = new Scanner(formattedTitle)) {
            while (titleScanner.hasNext()) {
                String word = titleScanner.next();

                // Camelize the word if it is significant
                boolean camelize = !Word.SMALLER_WORDS.contains(word.toLowerCase(Locale.ROOT));

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
        String formattedTitle = formatTitle(title);

        try (Scanner titleScanner = new Scanner(formattedTitle)) {
            return titleScanner.tokens()
                               .filter(Predicate.not(
                                       Word::isSmallerWord))
                               .collect(Collectors.joining(" "));
        }
    }

    private static String getTitleWordsWithSpaces(int number, String title) {
        String formattedTitle = formatTitle(title);

        try (Scanner titleScanner = new Scanner(formattedTitle)) {
            return titleScanner.tokens()
                               .limit(number)
                               .collect(Collectors.joining(" "));
        }
    }

    private static String keepLettersAndDigitsOnly(String in) {
        return in.codePoints()
                 .filter(Character::isLetterOrDigit)
                 .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                 .toString();
    }

    /**
     * Gets the last name of the first author/editor
     *
     * @param authorList an {@link AuthorList}
     * @return the surname of an author/editor or "" if no author was found This method is guaranteed to never return
     * null.
     */
    private static String firstAuthor(AuthorList authorList) {
        return authorList.getAuthors().stream()
                         .findFirst()
                         .flatMap(Author::getLast).orElse("");
    }

    /**
     * Gets the first name initials of the first author/editor
     *
     * @param authorList an {@link AuthorList}
     * @return the first name initial of an author/editor or "" if no author was found This method is guaranteed to
     * never return null.
     */
    private static String firstAuthorForenameInitials(AuthorList authorList) {
        return authorList.getAuthors().stream()
                         .findFirst()
                         .flatMap(Author::getFirstAbbr)
                         .map(s -> s.substring(0, 1))
                         .orElse("");
    }

    /**
     * Gets the von part and the last name of the first author/editor. No spaces are returned.
     *
     * @param authorList an {@link AuthorList}
     * @return the von part and surname of an author/editor or "" if no author was found. This method is guaranteed to
     * never return null.
     */
    private static String firstAuthorVonAndLast(AuthorList authorList) {
        return authorList.isEmpty() ? "" :
                authorList.getAuthor(0).getLastOnly().replaceAll(" ", "");
    }

    /**
     * Gets the last name of the last author/editor
     *
     * @param authorList an {@link AuthorList}
     * @return the surname of an author/editor
     */
    private static String lastAuthor(AuthorList authorList) {
        if (authorList.isEmpty()) {
            return "";
        }
        return authorList.getAuthors().get(authorList.getNumberOfAuthors() - 1).getLast().orElse("");
    }

    /**
     * Gets the forename initials of the last author/editor
     *
     * @param authorList an {@link AuthorList}
     * @return the forename initial of an author/editor or "" if no author was found This method is guaranteed to never
     * return null.
     */
    private static String lastAuthorForenameInitials(AuthorList authorList) {
        if (authorList.isEmpty()) {
            return "";
        }
        return authorList.getAuthor(authorList.getNumberOfAuthors() - 1).getFirstAbbr().map(s -> s.substring(0, 1))
                         .orElse("");
    }

    /**
     * Gets the last name of all authors/editors
     *
     * @param authorList an {@link AuthorList}
     * @return the sur name of all authors/editors
     */
    private static String allAuthors(AuthorList authorList) {
        return joinAuthorsOnLastName(authorList, authorList.getNumberOfAuthors(), "", "");
    }

    /**
     * Returns the authors according to the BibTeX-alpha-Style
     *
     * @param authorList an {@link AuthorList}
     * @return the initials of all authors' names
     */
    private static String authorsAlpha(AuthorList authorList) {
        StringBuilder alphaStyle = new StringBuilder();
        int maxAuthors = authorList.getNumberOfAuthors() <= MAX_ALPHA_AUTHORS ?
                authorList.getNumberOfAuthors() : (MAX_ALPHA_AUTHORS - 1);

        if (authorList.getNumberOfAuthors() == 1) {
            String[] firstAuthor = authorList.getAuthor(0).getLastOnly()
                                             .replaceAll("\\s+", " ").trim().split(" ");
            // take first letter of any "prefixes" (e.g. van der Aalst -> vd)
            for (int j = 0; j < (firstAuthor.length - 1); j++) {
                alphaStyle.append(firstAuthor[j], 0, 1);
            }
            // append last part of last name completely
            alphaStyle.append(firstAuthor[firstAuthor.length - 1], 0,
                    Math.min(3, firstAuthor[firstAuthor.length - 1].length()));
        } else {
            List<String> vonAndLastNames = authorList.getAuthors().stream()
                                                     .limit(maxAuthors).map(Author::getLastOnly)
                                                     .collect(Collectors.toList());
            for (String vonAndLast : vonAndLastNames) {
                // replace all whitespaces by " "
                // split the lastname at " "
                String[] nameParts = vonAndLast.replaceAll("\\s+", " ").trim().split(" ");
                for (String part : nameParts) {
                    // use first character of each part of lastname
                    alphaStyle.append(part, 0, 1);
                }
            }
            if (authorList.getNumberOfAuthors() > MAX_ALPHA_AUTHORS) {
                alphaStyle.append("+");
            }
        }
        return alphaStyle.toString();
    }

    /**
     * Creates a string with all last names separated by a `delimiter`. If the number of authors are larger than
     * `maxAuthors`, replace all excess authors with `suffix`.
     *
     * @param authorList the list of authors
     * @param maxAuthors the maximum number of authors in the string
     * @param delimiter  delimiter separating the last names of the authors
     * @param suffix     to replace excess authors with
     * @return a string consisting of authors' last names separated by a `delimiter` and with any authors excess of
     * `maxAuthors` replaced with `suffix`
     */
    private static String joinAuthorsOnLastName(AuthorList authorList, int maxAuthors, String delimiter, String suffix) {
        suffix = authorList.getNumberOfAuthors() > maxAuthors ? suffix : "";
        return authorList.getAuthors().stream()
                         .map(Author::getLast).flatMap(Optional::stream)
                         .limit(maxAuthors).collect(Collectors.joining(delimiter, "", suffix));
    }

    /**
     * Gets the surnames of the first N authors and appends EtAl if there are more than N authors
     *
     * @param authorList an {@link AuthorList}
     * @param n          the number of desired authors
     * @return Gets the surnames of the first N authors and appends EtAl if there are more than N authors
     */
    private static String nAuthors(AuthorList authorList, int n) {
        return joinAuthorsOnLastName(authorList, n, "", "EtAl");
    }

    /**
     * Gets the first part of the last name of the first author/editor, and appends the last name initial of the
     * remaining authors/editors. Maximum 5 characters
     *
     * @param authorList an <{@link AuthorList}
     * @return the surname of all authors/editors
     */
    private static String oneAuthorPlusInitials(AuthorList authorList) {
        if (authorList.isEmpty()) {
            return "";
        }

        StringBuilder authorSB = new StringBuilder();
        // authNofMth start index at 1 instead of 0
        authorSB.append(authNofMth(authorList, CHARS_OF_FIRST, 1));
        for (int i = 2; i <= authorList.getNumberOfAuthors(); i++) {
            authorSB.append(authNofMth(authorList, 1, i));
        }
        return authorSB.toString();
    }

    /**
     * auth.auth.ea format:
     * <ol>
     * <li>Isaac Newton and James Maxwell and Albert Einstein (1960)</li>
     * <li>Isaac Newton and James Maxwell (1960)</li>
     * </ol>
     * give:
     * <ol>
     * <li>Newton.Maxwell.ea</li>
     * <li>Newton.Maxwell</li>
     * </ol>
     */
    private static String authAuthEa(AuthorList authorList) {
        return joinAuthorsOnLastName(authorList, 2, ".", ".ea");
    }

    /**
     * auth.etal, authEtAl, ... format:
     * <ol>
     * <li>Isaac Newton and James Maxwell and Albert Einstein (1960)</li>
     * <li>Isaac Newton and James Maxwell (1960)</li>
     * </ol>
     * <p>
     * auth.etal give (delim=".", append=".etal"):
     * <ol>
     * <li>Newton.etal</li>
     * <li>Newton.Maxwell</li>
     * </ol>
     * </p>
     * <p>
     * authEtAl give (delim="", append="EtAl"):
     * <ol>
     * <li>NewtonEtAl</li>
     * <li>NewtonMaxwell</li>
     * </ol>
     * </p>
     * Note that [authEtAl] equals [authors2]
     */
    private static String authEtal(AuthorList authorList, String delim, String append) {
        if (authorList.getNumberOfAuthors() <= 2) {
            return joinAuthorsOnLastName(authorList, 2, delim, "");
        } else {
            return authorList.getAuthor(0).getLast().orElse("") + append;
        }
    }

    /**
     * The first N characters of the Mth author's or editor's last name. M starts counting from 1
     */
    private static String authNofMth(AuthorList authorList, int n, int m) {
        // have m counting from 0
        int mminusone = m - 1;

        if ((authorList.getNumberOfAuthors() <= mminusone) || (n < 0) || (mminusone < 0)) {
            return "";
        }

        String lastName = authorList.getAuthor(mminusone).getLast()
                                    .map(CitationKeyGenerator::removeDefaultUnwantedCharacters).orElse("");
        return lastName.length() > n ? lastName.substring(0, n) : lastName;
    }

    /**
     * First N chars of the first author's last name.
     */
    private static String authN(AuthorList authorList, int num) {
        return authNofMth(authorList, num, 1);
    }

    /**
     * authshort format:
     * <p>
     * given author names
     * <ol><li>Isaac Newton and James Maxwell and Albert Einstein and N. Bohr</li>
     * <li>Isaac Newton and James Maxwell and Albert Einstein</li>
     * <li>Isaac Newton and James Maxwell</li>
     * <li>Isaac Newton</li></ol>
     * yield
     * <ol><li>NME+</li>
     * <li>NME</li>
     * <li>NM</li>
     * <li>Newton</li></ol></p>
     * {@author added by Kolja Brix, kbx@users.sourceforge.net}
     */
    private static String authshort(AuthorList authorList) {
        StringBuilder author = new StringBuilder();
        final int numberOfAuthors = authorList.getNumberOfAuthors();

        if (numberOfAuthors == 1) {
            author.append(authorList.getAuthor(0).getLast().orElse(""));
        } else if (numberOfAuthors >= 2) {
            for (int i = 0; i < numberOfAuthors && i < 3; i++) {
                author.append(authNofMth(authorList, 1, i + 1));
            }
            if (numberOfAuthors > 3) {
                author.append('+');
            }
        }

        return author.toString();
    }

    /**
     * authIniN format:
     * <p>
     * Each author gets (N div #authors) chars, the remaining (N mod #authors) chars are equally distributed to the
     * authors first in the row. If (N < #authors), only the first N authors get mentioned.
     * <p>
     * For example if
     * <ol>
     * <li> I. Newton and J. Maxwell and A. Einstein and N. Bohr (..) </li>
     * <li> I. Newton and J. Maxwell and A. Einstein </li>
     * <li> I. Newton and J. Maxwell </li>
     * <li> I. Newton </li>
     * </ol>
     * authIni4 gives:
     * <ol>
     * <li> NMEB </li>
     * <li> NeME </li>
     * <li> NeMa </li>
     * <li> Newt </li>
     * </ol>
     *
     * @param authorList The authors to format.
     * @param n          The maximum number of characters this string will be long. A negative number or zero will lead
     *                   to "" be returned.
     */
    private static String authIniN(AuthorList authorList, int n) {
        if (n <= 0 || authorList.isEmpty()) {
            return "";
        }

        StringBuilder author = new StringBuilder();
        final int numberOfAuthors = authorList.getNumberOfAuthors();

        int charsAll = n / numberOfAuthors;
        for (int i = 0; i < numberOfAuthors; i++) {
            if (i < (n % numberOfAuthors)) {
                author.append(authNofMth(authorList, charsAll + 1, i + 1));
            } else {
                author.append(authNofMth(authorList, charsAll, i + 1));
            }
        }

        if (author.length() <= n) {
            return author.toString();
        } else {
            return author.substring(0, n);
        }
    }

    /**
     * Split the pages field into separate numbers and return the lowest
     *
     * @param pages (may not be null) a pages string such as 42--111 or 7,41,73--97 or 43+
     * @return the first page number or "" if no number is found in the string
     * @throws NullPointerException if pages is null
     */
    public static String firstPage(String pages) {
        // FIXME: incorrectly exracts the first page when pages are
        // specified with ellipse, e.g. "213-6", which should stand
        // for "213-216". S.G.
        return NOT_DECIMAL_DIGIT.splitAsStream(pages)
                                .filter(Predicate.not(String::isBlank))
                                .map(BigInteger::new)
                                .min(BigInteger::compareTo)
                                .map(BigInteger::toString)
                                .orElse("");
    }

    /**
     * Return the non-digit prefix of pages
     *
     * @param pages a pages string such as L42--111 or L7,41,73--97 or L43+
     * @return the non-digit prefix of pages (like "L" of L7) or "" if no non-digit prefix is found in the string
     * @throws NullPointerException if pages is null.
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
     * @param pages a pages string such as 42--111 or 7,41,73--97 or 43+
     * @return the first page number or "" if no number is found in the string
     * @throws NullPointerException if pages is null.
     */
    public static String lastPage(String pages) {
        return NOT_DECIMAL_DIGIT.splitAsStream(pages)
                                .filter(Predicate.not(String::isBlank))
                                .map(BigInteger::new)
                                .max(BigInteger::compareTo)
                                .map(BigInteger::toString)
                                .orElse("");
    }

    /**
     * Parse a field marker with modifiers, possibly containing a parenthesised modifier, as well as escaped colons and
     * parentheses.
     *
     * @param arg The argument string.
     * @return An array of strings representing the parts of the marker
     */
    protected static List<String> parseFieldAndModifiers(String arg) {
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
     * <p>
     * An author or editor may be and institution not a person. In that case the key generator builds very long keys,
     * e.g.: for &ldquo;The Attributed Graph Grammar System (AGG)&rdquo; -> &ldquo;TheAttributedGraphGrammarSystemAGG&rdquo;.
     * </p>
     *
     * <p>
     * An institution name should be inside <code>{}</code> brackets. If the institution name includes its abbreviation
     * this abbreviation should be in <code>{}</code> brackets. For the previous example the value should look like:
     * <code>{The Attributed Graph Grammar System ({AGG})}</code>.
     * </p>
     *
     * <p>
     * If an institution includes its abbreviation, i.e. "...({XYZ})", first such abbreviation should be used as the key
     * value part of such author.
     * </p>
     *
     * <p>
     * If an institution does not include its abbreviation the key should be generated from its name in the following
     * way:
     * </p>
     *
     * <p>
     * The institution value can contain: institution name, part of the institution, address, etc. These values should
     * be comma separated. Institution name and possible part of the institution should be in the beginning, while
     * address and secondary information should be in the end.
     * </p>
     * <p>
     * Each part is examined separately:
     * <ol>
     * <li>We remove all tokens of a part which are one of the defined ignore words (the, press), which end with a dot
     * (ltd., co., ...) and which first character is lowercase (of, on, di, ...).</li>
     * <li>We detect the types of the part: university, technology institute,
     * department, school, rest
     * <ul>
     * <li>University: <code>"Uni[NameOfTheUniversity]"</code></li>
     * <li>Department: If the institution value contains more than one comma separated part, the department will be an
     * abbreviation of all words beginning with the uppercase letter except of words:
     * <code>d[ei]p.*</code>, school, faculty</li>
     * <li>School: same as department</li>
     * <li>Rest: If there are less than 3 tokens in such part than the result
     * is a concatenation of those tokens. Otherwise, the result will be built
     * from the first letter in each token.</li>
     * </ul>
     * </ol>
     * <p>
     * Parts are concatenated together in the following way:
     * <ul>
     * <li>If there is a university part use it otherwise use the rest part.</li>
     * <li>If there is a school part append it.</li>
     * <li>If there is a department part and it is not same as school part
     * append it.</li>
     * </ul>
     * <p>
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
        if (content == null) {
            return null;
        }
        if (content.isBlank()) {
            return "";
        }

        Matcher matcher = INLINE_ABBREVIATION.matcher(content);
        if (matcher.find()) {
            return LatexToUnicodeAdapter.format(matcher.group());
        }

        Optional<String> unicodeFormattedName = LatexToUnicodeAdapter.parse(content);
        if (unicodeFormattedName.isEmpty()) {
            LOGGER.warn("{} could not be converted to unicode. This can result in an incorrect or missing institute citation key", content);
        }
        String result = unicodeFormattedName.orElse(Normalizer.normalize(content, Normalizer.Form.NFC));

        // Special characters can't be allowed past this point because the citation key generator might replace them with multiple mixed-case characters
        result = StringUtil.replaceSpecialCharacters(result);

        String[] institutionNameTokens = result.split(",");

        // Key parts
        String university = null;
        String department = null;
        String school = null;
        String rest = null;

        for (int index = 0; index < institutionNameTokens.length; index++) {
            List<String> tokenParts = getValidInstitutionNameParts(institutionNameTokens[index]);
            EnumSet<Institution> tokenTypes = Institution.findTypes(tokenParts);

            if (tokenTypes.contains(Institution.UNIVERSITY)) {
                StringBuilder universitySB = new StringBuilder();
                // University part looks like: Uni[NameOfTheUniversity]
                universitySB.append("Uni");
                for (String k : tokenParts) {
                    if (!"uni".regionMatches(true, 0, k, 0, 3)) {
                        universitySB.append(k);
                    }
                }
                university = universitySB.toString();
                // If university is detected than the previous part is suggested
                // as department
                if ((index > 0) && (department == null)) {
                    department = institutionNameTokens[index - 1];
                }
            } else if ((tokenTypes.contains(Institution.SCHOOL)
                    || tokenTypes.contains(Institution.DEPARTMENT))
                    && institutionNameTokens.length > 1) {
                // School is an abbreviation of all the words beginning with a
                // capital letter excluding: department, school and faculty words.
                StringBuilder schoolSB = new StringBuilder();
                StringBuilder departmentSB = new StringBuilder();
                for (String k : tokenParts) {
                    if (noOtherInstitutionKeyWord(k)) {
                        if (tokenTypes.contains(Institution.SCHOOL)) {
                            schoolSB.append(NOT_CAPITAL_CHARACTER.matcher(k).replaceAll(""));
                        }
                        // Explicitly defined department part is build the same way as school
                        if (tokenTypes.contains(Institution.DEPARTMENT)) {
                            departmentSB.append(NOT_CAPITAL_CHARACTER.matcher(k).replaceAll(""));
                        }
                    }
                }
                if (tokenTypes.contains(Institution.SCHOOL)) {
                    school = schoolSB.toString();
                }
                if (tokenTypes.contains(Institution.DEPARTMENT)) {
                    department = departmentSB.toString();
                }
            } else if (rest == null) {
                // A part not matching university, department nor school
                if (tokenParts.size() >= 3) {
                    // If there are more than 3 parts, only keep the first character of each word
                    final int[] codePoints = tokenParts.stream()
                                                       .filter(Predicate.not(String::isBlank))
                                                       .mapToInt((s) -> s.codePointAt(0))
                                                       .toArray();
                    rest = new String(codePoints, 0, codePoints.length);
                } else {
                    rest = String.join("", tokenParts);
                }
            }
        }

        // Putting parts together.
        return (university == null ? Objects.toString(rest, "") : university)
                + (school == null ? "" : school)
                + ((department == null)
                || ((school != null) && department.equals(school)) ? "" : department);
    }

    /**
     * Helper method for {@link BracketedPattern#generateInstitutionKey(String)}. Checks that the word is not an
     * institution keyword and has an uppercase first letter, except univ/tech key word.
     *
     * @param word to check
     */
    private static boolean noOtherInstitutionKeyWord(String word) {
        return !DEPARTMENTS.matcher(word).matches()
                && !StandardField.SCHOOL.getName().equalsIgnoreCase(word)
                && !"faculty".equalsIgnoreCase(word)
                && !NOT_CAPITAL_CHARACTER.matcher(word).replaceAll("").isEmpty();
    }

    private static List<String> getValidInstitutionNameParts(String name) {
        List<String> nameParts = new ArrayList<>();
        List<String> ignore = Arrays.asList("press", "the");

        // Cleanup: remove unnecessary words.
        for (String part : name.replaceAll("\\{[A-Z]+}", "").split("[ \\-_]")) {
            if ((!(part.isEmpty()) // remove empty
                    && !ignore.contains(part.toLowerCase(Locale.ENGLISH)) // remove ignored words
                    && (part.charAt(part.length() - 1) != '.')
                    && Character.isUpperCase(part.charAt(0)))
                    || ((part.length() >= 3) && "uni".equalsIgnoreCase(part.substring(0, 3)))) {
                nameParts.add(part);
            }
        }
        return nameParts;
    }
}
