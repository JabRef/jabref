package org.jabref.logic.importer.plaincitation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

/// Parse a plain citation using regex rules.
///
/// TODO: This class is similar to [org.jabref.logic.importer.fileformat.pdf.RuleBasedBibliographyPdfImporter], we need to unify them.
public class RuleBasedPlainCitationParser implements PlainCitationParser {

    private static final String AUTHOR_TAG = "[author_tag]";
    private static final String URL_TAG = "[url_tag]";
    private static final String YEAR_TAG = "[year_tag]";
    private static final String PAGES_TAG = "[pages_tag]";

    private static final String INITIALS_GROUP = "INITIALS";
    private static final String LASTNAME_GROUP = "LASTNAME";
    private static final String YEAR_GROUP = "YEAR";

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)" +
                    "(([\\w\\-]+\\.)+?([\\w\\-.~]+\\/?)*" +
                    "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    /// Punctuation that ends a sentence or a bracket, but is not part of the URL in front of it.
    private static final Pattern TRAILING_URL_PUNCTUATION_PATTERN = Pattern.compile("[.,;:!?'\")\\]}]+$");

    /// A four-digit number that is not part of a longer number, optionally followed by a second one to form a
    /// range (`2019–2021`). Only the first number is the year.
    private static final Pattern YEAR_PATTERN = Pattern.compile(
            "(?<!\\d)(?<" + YEAR_GROUP + ">\\d{4})(?:\\s*[-‐‑‒–—]\\s*\\d{4})?(?!\\d)");

    /// A year in parentheses, optionally with a disambiguation letter: `(1991a)`. This is the APA style and the
    /// most reliable place for the publication year.
    private static final Pattern YEAR_IN_PARENTHESES_CANDIDATE_PATTERN = Pattern.compile(
            "\\(\\s*(?<" + YEAR_GROUP + ">\\d{4})\\s*[a-z]?\\s*\\)");

    /// `p.`, `pp.` or `pages` at the end of a text: the number behind it is a page, not a year.
    private static final Pattern PAGES_PREFIX_AT_END_PATTERN = Pattern.compile(
            "(?:\\bpages?|\\bpp?)\\.?\\s*$",
            Pattern.CASE_INSENSITIVE);

    /// Matches any of the internal placeholder tokens.
    private static final Pattern TAG_PATTERN = Pattern.compile("\\[(?:author|url|year|pages)_tag\\]");

    /// A year in parentheses, optionally with a disambiguation letter, as in `(1991a)`. Used for the APA style
    /// "Author (year). Title."
    private static final Pattern YEAR_IN_PARENTHESES_PATTERN = Pattern.compile(
            "\\(\\s*" + Pattern.quote(YEAR_TAG) + "\\s*[a-z]?\\s*\\)");

    /// Punctuation and year tokens in front of the title (for example the `. ` in `(1993). Title`).
    private static final Pattern LEADING_NOISE_PATTERN = Pattern.compile(
            "^(?:[\\s.,;:&()\\-]|" + Pattern.quote(YEAR_TAG) + ")+");

    private static final String FIRSTNAME_GROUP = "FIRSTNAME";
    private static final String AUTHORS_GROUP = "AUTHORS";

    private static final String NAME_PARTICLE_REGEX = "(?:de|der|den|van|von|di|da|del|dos|du|la|le|ten|ter)";

    /// A capitalized word of a name, with hyphens or apostrophes (`Garcia-Molina`, `O'Brien`).
    /// Unicode aware, in contrast to `\w`.
    private static final String NAME_WORD_REGEX = "\\p{Lu}[\\p{L}'’\\-]+";

    /// Family name, optionally with lowercase particles (`de Roever`)
    private static final String LASTNAME_REGEX =
            "(?<" + LASTNAME_GROUP + ">(?:" + NAME_PARTICLE_REGEX + "\\s+)*" + NAME_WORD_REGEX + ")";

    /// One to three initials, optionally hyphenated (`W.-P.`). Note that the pattern is case-sensitive on purpose,
    /// otherwise `\p{Lu}` would also match lowercase letters.
    private static final String INITIALS_REGEX =
            "(?<" + INITIALS_GROUP + ">(?:\\p{Lu}\\.\\s*-?\\s*){1,3})";

    /// Separators between two authors: `,`, `.`, `&` and `and`
    private static final String AUTHOR_SEPARATOR_REGEX = "(?:\\s*(?:and\\b|&|,|\\.))*\\s*";

    /// `Lastname, I. J.`. `\G` anchors every match to the end of the previous one (or to the start of the input).
    /// Thus, only the authors block at the very beginning of the citation is matched,
    /// and never something in the title, journal, or publisher.
    private static final Pattern LASTNAME_FIRST_AUTHOR_PATTERN = Pattern.compile(
            "\\G\\s*" + LASTNAME_REGEX + ",\\s*" + INITIALS_REGEX + AUTHOR_SEPARATOR_REGEX);

    /// `I. J. Lastname`. Anchored with `\G` in the same way.
    private static final Pattern INITIALS_FIRST_AUTHOR_PATTERN = Pattern.compile(
            "\\G\\s*" + INITIALS_REGEX + LASTNAME_REGEX + AUTHOR_SEPARATOR_REGEX);

    /// `et al.` (the period is optional) directly behind the last author. BibTeX writes it as `and others`.
    private static final Pattern ET_AL_PATTERN = Pattern.compile(
            "^\\s*et\\s+al\\b\\.?\\s*",
            Pattern.CASE_INSENSITIVE);

    /// `Firstname [Middle] Lastname`, as in `Marc Shapiro and Susan Horwitz.`: the complete authors block, which has to
    /// end with a period. This pattern is the most ambiguous one, because a title such as `Machine Learning.` looks
    /// the same. Thus, it is only used if neither of the patterns above found an author.
    private static final String FULL_NAME_REGEX =
            NAME_WORD_REGEX + "(?:\\s+(?:\\p{Lu}\\.|" + NAME_PARTICLE_REGEX + "|" + NAME_WORD_REGEX + ")){1,3}";

    private static final String FULL_NAME_SEPARATOR_REGEX = "(?:\\s*(?:,|&|\\band\\b))+\\s*";

    private static final Pattern FULL_NAME_AUTHORS_PATTERN = Pattern.compile(
            "^\\s*(?<" + AUTHORS_GROUP + ">" + FULL_NAME_REGEX +
                    "(?:" + FULL_NAME_SEPARATOR_REGEX + FULL_NAME_REGEX + ")*)\\.(?=\\s|$)");

    private static final Pattern FULL_NAME_SEPARATOR_PATTERN = Pattern.compile(FULL_NAME_SEPARATOR_REGEX);

    /// Splits one full name into first name(s) and family name. The first name is as short as possible.
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile(
            "^(?<" + FIRSTNAME_GROUP + ">.+?)\\s+" + LASTNAME_REGEX + "$");

    /// A page range such as `127-136`, `90–97` (en dash) or `pp. 62-68`. The range must not be glued to a word
    /// or to another number, so that report numbers such as `rr-02-92` are not taken as pages.
    private static final Pattern PAGES_RANGE_PATTERN = Pattern.compile(
            "(?:\\b(?:pages?|pp?)\\.?\\s*)?(?<![\\p{L}\\d\\-])(?<FROM>\\d+)\\s*[-‐‑‒–—]+\\s*(?<TO>\\d+)",
            Pattern.CASE_INSENSITIVE);

    /// A single page that is explicitly marked as such: `p. 5`, `page 5`
    private static final Pattern PAGES_SINGLE_PATTERN = Pattern.compile(
            "\\b(?:pages?|pp?)\\.?\\s*(?<PAGE>\\d+)",
            Pattern.CASE_INSENSITIVE);

    /// End of a sentence: a period (or `?`/`!`) after a word of at least three letters or digits (`... of 2020.`),
    /// followed by a capital letter. Single letters (initials, `U.S.`) do not end the title.
    private static final Pattern TITLE_END_PATTERN = Pattern.compile("(?<=[\\p{L}\\d]{3})[.?!]\\s+(?=\\p{Lu})");

    private static final Pattern QUOTED_TITLE_PATTERN = Pattern.compile("^[\"“](?<TITLE>[^\"”]+)[\"”]");

    private static final Pattern LEADING_VENUE_NOISE_PATTERN = Pattern.compile("^[\\s,.;:]*(?:In\\s+)?");

    /// A comma-separated part that only consists of volume/issue/number information, such as `41`, `4(2)` or `vol. 3`
    private static final Pattern NUMERIC_PART_PATTERN = Pattern.compile(
            "^(?:(?:vol(?:ume)?|no|nr|issue|pp?|pages?)\\.?\\s*)?[\\d\\s().:/\\-]*\\d[\\d\\s().:/\\-]*$",
            Pattern.CASE_INSENSITIVE);

    private List<String> urls;
    private List<String> authors;
    private String year;
    private String pages;
    private String title;
    private String doi;
    private boolean isArticle;
    private String journalOrPublisher;

    @Override
    public Optional<BibEntry> parsePlainCitation(String text) {
        urls = new ArrayList<>();
        authors = new ArrayList<>();
        year = "";
        pages = "";
        title = "";
        doi = "";
        isArticle = true;
        journalOrPublisher = "";

        String inputWithoutDoi = findDoi(text);
        String inputWithoutUrls = findUrls(inputWithoutDoi);
        String inputWithoutAuthors = findAuthors(inputWithoutUrls);
        String inputWithoutYear = findYear(inputWithoutAuthors);
        String inputWithoutPages = findPages(inputWithoutYear);
        findParts(inputWithoutPages);

        if (nothingFound()) {
            return Optional.empty();
        }
        return generateEntity(text.strip());
    }

    /// Text without any citation data must not create an empty `@article` entry.
    private boolean nothingFound() {
        return authors.isEmpty()
                && urls.isEmpty()
                && year.isEmpty()
                && pages.isEmpty()
                && title.isEmpty()
                && doi.isEmpty();
    }

    /// @param originalCitation the citation as entered by the user. It is stored in the `comment` field so that
    ///                         nothing is lost, and so that no internal placeholder token ends up there.
    private Optional<BibEntry> generateEntity(String originalCitation) {
        EntryType type = isArticle ? StandardEntryType.Article : StandardEntryType.Book;
        BibEntry extractedEntity = new BibEntry(type);
        setIfNotEmpty(extractedEntity, StandardField.AUTHOR, String.join(" and ", authors));
        setIfNotEmpty(extractedEntity, StandardField.URL, String.join(", ", urls));
        setIfNotEmpty(extractedEntity, StandardField.YEAR, year);
        setIfNotEmpty(extractedEntity, StandardField.PAGES, pages);
        setIfNotEmpty(extractedEntity, StandardField.TITLE, title);
        setIfNotEmpty(extractedEntity, StandardField.DOI, doi);
        setIfNotEmpty(extractedEntity, isArticle ? StandardField.JOURNAL : StandardField.PUBLISHER, journalOrPublisher);
        extractedEntity.setField(StandardField.COMMENT, originalCitation);
        return Optional.of(extractedEntity);
    }

    private static void setIfNotEmpty(BibEntry entry, Field field, String value) {
        if (!value.isEmpty()) {
            entry.setField(field, value);
        }
    }

    private String findUrls(String input) {
        Matcher matcher = URL_PATTERN.matcher(input);
        while (matcher.find()) {
            String url = input.substring(matcher.start(1), matcher.end());
            urls.add(TRAILING_URL_PUNCTUATION_PATTERN.matcher(url).replaceFirst(""));
        }
        return fixSpaces(matcher.replaceAll(URL_TAG));
    }

    /// Extracts the first DOI found in `input`, stores its canonical form in [#doi],
    /// and returns `input` with the matched DOI (including any leading `doi:` or
    /// `https://doi.org/` prefix) removed so the remaining rules do not re-parse it as a
    /// URL or title.
    ///
    /// The DOI is replaced with an empty string rather than a placeholder token: DOIs
    /// typically contain digits, and a token would change how the remaining text is interpreted.
    ///
    /// Called before [#findUrls] so a `doi.org` link is recorded as a DOI rather
    /// than being swallowed by the generic URL rule.
    ///
    /// @param input raw citation text
    /// @return the input with the DOI stripped, or the original input if no DOI was found
    private String findDoi(String input) {
        Optional<DOI> found = DOI.findInText(input);
        if (found.isEmpty()) {
            return input;
        }
        doi = found.get().asString();

        // Remove exactly the DOI and its prefix. DOI.replaceInText removed a neighbouring URL, too.
        Matcher matcher = Pattern.compile(
                "(?:https?://(?:dx\\.)?doi\\.org/|urn:doi:\\s*|doi:\\s*)?" + Pattern.quote(doi),
                Pattern.CASE_INSENSITIVE).matcher(input);
        // Only whitespace is collapsed here. fixSpaces would insert spaces into a URL that is still in the text
        // ("https://x.org" -> "https: //x. org"), so no URL could be found afterwards.
        if (matcher.find()) {
            return collapseWhitespace(input.substring(0, matcher.start()) + " " + input.substring(matcher.end()));
        }
        // The DOI is written differently than its canonical form (for example percent-encoded)
        return collapseWhitespace(DOI.replaceInText(input, ""));
    }

    private static String collapseWhitespace(String input) {
        return input.replaceAll("\\s+", " ").strip();
    }

    /// Finds the publication year. A year in parentheses (`(1993)`, `(1991a)`) is preferred. Otherwise, the last
    /// valid year wins, because the publication year usually stands behind the title, which may mention a year, too.
    /// A year range (`2019–2021`) counts as its first year, and a number behind `p.`/`pp.` is a page, not a year.
    ///
    /// Only the chosen occurrence is replaced by [#YEAR_TAG], so other occurrences (in the title) stay untouched.
    private String findYear(String input) {
        Matcher parenthesesMatcher = YEAR_IN_PARENTHESES_CANDIDATE_PATTERN.matcher(input);
        while (parenthesesMatcher.find()) {
            if (isValidYear(parenthesesMatcher.group(YEAR_GROUP))) {
                year = parenthesesMatcher.group(YEAR_GROUP);
                return fixSpaces(input.substring(0, parenthesesMatcher.start(YEAR_GROUP))
                        + YEAR_TAG
                        + input.substring(parenthesesMatcher.end(YEAR_GROUP)));
            }
        }

        int start = -1;
        int end = -1;
        Matcher matcher = YEAR_PATTERN.matcher(input);
        while (matcher.find()) {
            if (!isValidYear(matcher.group(YEAR_GROUP))
                    || PAGES_PREFIX_AT_END_PATTERN.matcher(input.substring(0, matcher.start())).find()) {
                continue;
            }
            year = matcher.group(YEAR_GROUP);
            start = matcher.start();
            end = matcher.end();
        }
        if (start == -1) {
            return input;
        }
        return fixSpaces(input.substring(0, start) + YEAR_TAG + input.substring(end));
    }

    private static boolean isValidYear(String candidate) {
        int value = Integer.parseInt(candidate);
        return (value > 1700) && (value <= Calendar.getInstance().get(Calendar.YEAR));
    }

    private String findAuthors(String input) {
        String result = findAuthorsByPattern(input, LASTNAME_FIRST_AUTHOR_PATTERN);
        if (authors.isEmpty()) {
            result = findAuthorsByPattern(input, INITIALS_FIRST_AUTHOR_PATTERN);
        }
        if (authors.isEmpty()) {
            result = findFullNameAuthors(input);
        }
        return result;
    }

    /// Finds authors with full first names (`Marc Shapiro and Susan Horwitz.`) at the beginning of `input`.
    /// Returns `input` unchanged if the beginning is not such an authors block.
    private String findFullNameAuthors(String input) {
        Matcher blockMatcher = FULL_NAME_AUTHORS_PATTERN.matcher(input);
        if (!blockMatcher.find()) {
            return input;
        }

        List<String> fullNameAuthors = new ArrayList<>();
        for (String name : FULL_NAME_SEPARATOR_PATTERN.split(blockMatcher.group(AUTHORS_GROUP))) {
            Matcher nameMatcher = FULL_NAME_PATTERN.matcher(name.strip());
            // Initials without periods ("Smith JK.") look like a last name in capitals: this is not a full name
            if (!nameMatcher.matches() || nameMatcher.group(LASTNAME_GROUP).matches("\\p{Lu}{1,3}")) {
                return input;
            }
            fullNameAuthors.add(nameMatcher.group(LASTNAME_GROUP).replaceAll("\\s+", " ")
                    + ", " + nameMatcher.group(FIRSTNAME_GROUP).replaceAll("\\s+", " "));
        }

        authors.addAll(fullNameAuthors);
        return fixSpaces(AUTHOR_TAG + " " + input.substring(blockMatcher.end()));
    }

    /// Collects the consecutive authors at the beginning of `input` and replaces the whole authors block by one
    /// [#AUTHOR_TAG]. An `et al.` behind the last author becomes the BibTeX author `others`.
    /// Returns `input` unchanged if `pattern` does not match at the beginning.
    private String findAuthorsByPattern(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        int authorsEnd = 0;
        while (matcher.find()) {
            authors.add(generateAuthor(matcher.group(LASTNAME_GROUP), matcher.group(INITIALS_GROUP)));
            authorsEnd = matcher.end();
        }
        if (authors.isEmpty()) {
            return input;
        }

        String rest = input.substring(authorsEnd);
        Matcher etAlMatcher = ET_AL_PATTERN.matcher(rest);
        if (etAlMatcher.find()) {
            authors.add("others");
            rest = rest.substring(etAlMatcher.end());
        }
        return fixSpaces(AUTHOR_TAG + " " + rest);
    }

    private String generateAuthor(String lastName, String initials) {
        String normalizedLastName = lastName.replaceAll("\\s+", " ");
        // fixSpaces turns "W.-P." into "W. -P." - undo that
        String normalizedInitials = initials.replaceAll("\\s*-\\s*", "-").replaceAll("\\s+", " ").trim();
        return normalizedLastName + ", " + normalizedInitials;
    }

    /// Finds the pages. A range (`127-136`, `90–97`, `pp. 62-68`) is preferred, the last one wins because volume
    /// and issue numbers usually stand in front of the pages. A single number is only taken if it is marked as
    /// page (`p. 5`), otherwise the volume number would be mistaken as page.
    ///
    /// Pages are stored with the BibTeX range separator `--`.
    private String findPages(String input) {
        int start = -1;
        int end = -1;
        String foundPages = "";

        Matcher rangeMatcher = PAGES_RANGE_PATTERN.matcher(input);
        while (rangeMatcher.find()) {
            start = rangeMatcher.start();
            end = rangeMatcher.end();
            foundPages = rangeMatcher.group("FROM") + "--" + rangeMatcher.group("TO");
        }

        if (start == -1) {
            Matcher singleMatcher = PAGES_SINGLE_PATTERN.matcher(input);
            if (singleMatcher.find()) {
                start = singleMatcher.start();
                end = singleMatcher.end();
                foundPages = singleMatcher.group("PAGE");
            }
        }

        if (start == -1) {
            return input;
        }
        pages = foundPages;
        return fixSpaces(input.substring(0, start) + PAGES_TAG + input.substring(end));
    }

    private String fixSpaces(String input) {
        return input.replaceAll("[,.!?;:]", "$0 ")
                    .replaceAll("\\p{Lt}", " $0")
                    .replaceAll("\\s+", " ").trim();
    }

    /// Infers title and journal/publisher from the text behind the authors.
    ///
    /// The title ends at the first sentence end (see [#TITLE_END_PATTERN]), at a closing quotation mark if the title
    /// is quoted, or at `//`. If there is none of them, the first comma ends the title. A `?` or `!` that ends the
    /// title belongs to it.
    ///
    /// The journal/publisher is the first comma-separated part after the title. The text behind the first remaining
    /// placeholder token (pages or year) is ignored, because it is usually location or date information.
    /// If it is the only part (no comma), its trailing period ends the sentence and is removed; otherwise the period
    /// is kept, because it belongs to an abbreviation (`ACM Trans. Math. Softw.,`).
    /// Placeholder tokens never end up in the title or journal/publisher.
    private void findParts(String input) {
        String afterAuthors = input.startsWith(AUTHOR_TAG) ? input.substring(AUTHOR_TAG.length()) : input;
        String remainder = YEAR_IN_PARENTHESES_PATTERN.matcher(afterAuthors).replaceAll(" ");
        remainder = LEADING_NOISE_PATTERN.matcher(remainder).replaceFirst("");

        String titlePart;
        String venuePart = "";

        Matcher quotedTitleMatcher = QUOTED_TITLE_PATTERN.matcher(remainder);
        Matcher titleEndMatcher = TITLE_END_PATTERN.matcher(remainder);
        int delimiterIndex = remainder.lastIndexOf("//");
        if (quotedTitleMatcher.find()) {
            titlePart = quotedTitleMatcher.group("TITLE");
            venuePart = remainder.substring(quotedTitleMatcher.end());
        } else if (delimiterIndex != -1) {
            titlePart = remainder.substring(0, delimiterIndex);
            venuePart = remainder.substring(delimiterIndex + 2);
        } else if (titleEndMatcher.find()) {
            int titleEnd = titleEndMatcher.start();
            if (remainder.charAt(titleEnd) != '.') {
                // "Is AI safe?" keeps its question mark
                titleEnd++;
            }
            titlePart = remainder.substring(0, titleEnd);
            venuePart = remainder.substring(titleEndMatcher.end());
        } else {
            int commaIndex = remainder.indexOf(',');
            if (commaIndex == -1) {
                titlePart = remainder;
            } else {
                titlePart = remainder.substring(0, commaIndex);
                venuePart = remainder.substring(commaIndex + 1);
            }
        }

        title = cleanField(titlePart, true);
        if (!containsLetterOrDigit(title)) {
            // Only symbols (for example emojis) are no title
            title = "";
        }

        Matcher tagMatcher = TAG_PATTERN.matcher(venuePart);
        if (tagMatcher.find()) {
            venuePart = venuePart.substring(0, tagMatcher.start());
        }
        venuePart = LEADING_VENUE_NOISE_PATTERN.matcher(venuePart).replaceFirst("");

        List<String> venueParts = Arrays.stream(venuePart.split(","))
                                        .map(String::strip)
                                        .filter(RuleBasedPlainCitationParser::containsLetterOrDigit)
                                        .toList();

        int textParts = 0;
        for (String part : venueParts) {
            if (NUMERIC_PART_PATTERN.matcher(part).matches()) {
                break;
            }
            textParts++;
        }

        if (textParts > 0) {
            boolean periodEndsSentence = !venuePart.contains(",");
            journalOrPublisher = cleanField(venueParts.getFirst(), periodEndsSentence);
        }
        if (textParts > 1) {
            isArticle = false;
        }
    }

    /// Removes placeholder tokens, surplus whitespace, and punctuation at the borders of a title or venue.
    private static String cleanField(String value, boolean stripTrailingPeriod) {
        String cleaned = TAG_PATTERN.matcher(value).replaceAll(" ")
                                    .replaceAll("\\s+", " ")
                                    .strip();
        String trailing = stripTrailingPeriod ? "[\\s,;:.\"“”]+$" : "[\\s,;:\"“”]+$";
        return cleaned.replaceAll("^[\\s,;:.\"“”]+", "").replaceAll(trailing, "");
    }

    private static boolean containsLetterOrDigit(String input) {
        return input.codePoints().anyMatch(Character::isLetterOrDigit);
    }
}