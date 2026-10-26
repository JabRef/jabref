package org.jabref.logic.importer.plaincitation;

import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.entry.BibEntry;
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

    private static final String AUTHORS_GROUP = "AUTHORS";
    private static final String ET_AL_GROUP = "ETAL";
    private static final String FIRSTNAME_GROUP = "FIRSTNAME";
    private static final String FROM_GROUP = "FROM";
    private static final String INITIALS_GROUP = "INITIALS";
    private static final String LASTNAME_GROUP = "LASTNAME";
    private static final String PAGE_GROUP = "PAGE";
    private static final String SUFFIX_GROUP = "SUFFIX";
    private static final String TITLE_GROUP = "TITLE";
    private static final String TO_GROUP = "TO";
    private static final String YEAR_GROUP = "YEAR";

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern REFERENCE_LABEL_PATTERN = Pattern.compile("\\s*\\[\\d+]\\s*");

    /// `doi:`, `urn:doi:` or a `doi.org` link directly in front of a DOI
    private static final Pattern DOI_PREFIX_AT_END_PATTERN = Pattern.compile(
            "(?:(?:https?://(?:(?:dx|www)\\.)?|www\\.)doi\\.org/|urn:doi:\\s*|doi:\\s*)$",
            Pattern.CASE_INSENSITIVE);

    /// An unfinished URL at the end of a text: a DOI directly behind it is part of that URL's path
    /// (`https://publisher.example.com/10.1000/xyz123`) and not a standalone DOI.
    private static final Pattern URL_BEFORE_DOI_PATTERN = Pattern.compile(
            "(?:(?:https?|ftps?)://|www\\.)\\S*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)" +
                    "(([\\w\\-]+\\.)+?([\\w\\-.~]+\\/?)*" +
                    "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

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

    private static final String NAME_PARTICLE_REGEX = "(?:de|der|den|van|von|di|da|del|dos|du|la|le|ten|ter)";

    private static final String NAME_SUFFIX_REGEX = "(?:Jr|Sr|II|III|IV)";

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

    /// `Lastname, I. J.`. It is applied with [Matcher#lookingAt()] at the end of the previous author (or at the start
    /// of the input). Thus, only the authors block at the very beginning of the citation is matched,
    /// and never something in the title, journal, or publisher.
    private static final Pattern LASTNAME_FIRST_AUTHOR_PATTERN = Pattern.compile(
            "\\s*" + LASTNAME_REGEX + ",\\s*" + INITIALS_REGEX + AUTHOR_SEPARATOR_REGEX);

    /// `I. J. Lastname`. Applied with [Matcher#lookingAt()] in the same way. Both author patterns are tried at every
    /// position of the authors block, so the two name orders can be mixed (`Smith, J. and A. Doe`).
    private static final Pattern INITIALS_FIRST_AUTHOR_PATTERN = Pattern.compile(
            "\\s*" + INITIALS_REGEX + LASTNAME_REGEX + AUTHOR_SEPARATOR_REGEX);

    /// `et al.` (the period is optional) directly behind the last author. BibTeX writes it as `and others`.
    private static final Pattern ET_AL_PATTERN = Pattern.compile(
            "^\\s*et\\s+al\\b\\.?\\s*",
            Pattern.CASE_INSENSITIVE);

    /// `Firstname [Middle] Lastname`, as in `Marc Shapiro and Susan Horwitz.`: the complete authors block, which has to
    /// end with a period. This pattern is the most ambiguous one, because a title such as `Machine Learning.` looks
    /// the same. Thus, it is only used if neither of the patterns above found an author and a separate title
    /// boundary follows the authors block.
    private static final String FULL_NAME_REGEX =
            NAME_WORD_REGEX + "(?:\\s+(?:\\p{Lu}\\.|" + NAME_PARTICLE_REGEX + "|" + NAME_WORD_REGEX + ")){1,3}?" +
                    "(?:\\s+" + NAME_SUFFIX_REGEX + "\\.?)?";

    private static final String FULL_NAME_SEPARATOR_REGEX = "(?:\\s*(?:,|&|\\band\\b))+\\s*";

    private static final Pattern FULL_NAME_AUTHORS_PATTERN = Pattern.compile(
            "^\\s*(?<" + AUTHORS_GROUP + ">" + FULL_NAME_REGEX +
                    "(?:" + FULL_NAME_SEPARATOR_REGEX + FULL_NAME_REGEX + ")*)" +
                    "(?:,?\\s+(?<" + ET_AL_GROUP + ">(?i:et\\s+al)))?\\.(?=\\s|$)");

    private static final Pattern FULL_NAME_SEPARATOR_PATTERN = Pattern.compile(FULL_NAME_SEPARATOR_REGEX);

    /// Splits one full name into first name(s) and family name. The first name is as short as possible.
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile(
            "^(?<" + FIRSTNAME_GROUP + ">.+?)\\s+" + LASTNAME_REGEX +
                    "(?:\\s+(?<" + SUFFIX_GROUP + ">" + NAME_SUFFIX_REGEX + ")\\.?)?$");

    /// Initials without periods (`Smith JK.`) look like a family name in capitals
    private static final Pattern INITIALS_WITHOUT_PERIODS_PATTERN = Pattern.compile("\\p{Lu}{1,3}");

    /// A hyphen with surrounding whitespace, as in `W. -P.`
    private static final Pattern SPACED_HYPHEN_PATTERN = Pattern.compile("\\s*-\\s*");

    /// A page range such as `127-136`, `90–97` (en dash) or `pp. 62-68`. The range must not be glued to a word
    /// or to another number, so that report numbers such as `rr-02-92` are not taken as pages.
    private static final Pattern PAGES_RANGE_PATTERN = Pattern.compile(
            "(?:\\b(?:pages?|pp?)\\.?\\s*)?(?<![\\p{L}\\d\\-])(?<" + FROM_GROUP + ">\\d+)\\s*[-‐‑‒–—]+\\s*(?<" + TO_GROUP + ">\\d+)",
            Pattern.CASE_INSENSITIVE);

    /// A single page that is explicitly marked as such: `p. 5`, `page 5`
    private static final Pattern PAGES_SINGLE_PATTERN = Pattern.compile(
            "\\b(?:pages?|pp?)\\.?\\s*(?<" + PAGE_GROUP + ">\\d+)",
            Pattern.CASE_INSENSITIVE);

    /// End of a sentence: a period (or `?`/`!`) after a word of at least three letters or digits (`... of 2020.`)
    /// or after an acronym of two capitals (`... for AI.`), followed by a letter, optionally after the
    /// extracted year and its punctuation. Single letters (initials, `U.S.`) do not end the title.
    private static final Pattern TITLE_END_PATTERN = Pattern.compile(
            "(?<=[\\p{L}\\d]{3}|(?<![\\p{L}\\d])\\p{Lu}{2})[.?!]\\s+(?:" + Pattern.quote(YEAR_TAG) + "[\\s.,;:]*)?(?=\\p{L})");

    private static final Pattern QUOTED_TITLE_PATTERN = Pattern.compile("^[\"“](?<" + TITLE_GROUP + ">[^\"”]+)[\"”]");

    private static final Pattern LEADING_FIELD_PUNCTUATION_PATTERN = Pattern.compile("^[\\s,;:.\"“”]+");

    private static final Pattern TRAILING_FIELD_PUNCTUATION_PATTERN = Pattern.compile("[\\s,;:.\"“”]+$");

    /// Same as [#TRAILING_FIELD_PUNCTUATION_PATTERN], but keeps a trailing period
    private static final Pattern TRAILING_FIELD_PUNCTUATION_WITHOUT_PERIOD_PATTERN = Pattern.compile("[\\s,;:\"“”]+$");

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

    /// Builds the entry from the parsed fields. `originalCitation` is the citation as entered by the user. It is
    /// stored in the `comment` field so that nothing is lost, and so that no internal placeholder token ends up there.
    private Optional<BibEntry> generateEntity(String originalCitation) {
        EntryType type = isArticle ? StandardEntryType.Article : StandardEntryType.Book;
        // An empty value does not create the field (see BibEntry#setField), so missing data yields no empty field
        return Optional.of(new BibEntry(type)
                .withField(StandardField.AUTHOR, String.join(" and ", authors))
                .withField(StandardField.URL, String.join(", ", urls))
                .withField(StandardField.YEAR, year)
                .withField(StandardField.PAGES, pages)
                .withField(StandardField.TITLE, title)
                .withField(StandardField.DOI, doi)
                .withField(isArticle ? StandardField.JOURNAL : StandardField.PUBLISHER, journalOrPublisher)
                .withField(StandardField.COMMENT, originalCitation));
    }

    private String findUrls(String input) {
        Matcher matcher = URL_PATTERN.matcher(input);
        while (matcher.find()) {
            String url = input.substring(matcher.start(1), matcher.end());
            urls.add(stripTrailingUrlPunctuation(url));
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
    /// If no DOI is found, `input` is returned unchanged.
    private String findDoi(String input) {
        // [impl->req~import.plain-citation.doi~1]
        return DOI.findInText(input)
                  .map(found -> {
                      doi = found.asString();
                      return removeDoi(input);
                  })
                  .orElse(input);
    }

    /// Removes exactly one occurrence of [#doi] and its prefix. `DOI.replaceInText` would remove a neighbouring URL, too.
    ///
    /// The occurrences are searched case-insensitively in `input` itself, not in a lowercased copy: lowercasing can
    /// change the length of a text (`İ` becomes two chars), which would shift all offsets behind it.
    ///
    /// An occurrence that is part of another URL (`https://publisher.example.com/10.1000/xyz123`) is skipped, because
    /// removing it would truncate that URL. A DOI with a prefix (`doi:`, `doi.org/`) or standing alone is removed.
    ///
    /// Only whitespace is collapsed here. [#fixSpaces] would insert spaces into a URL that is still in the text
    /// (`https://x.org` -> `https: //x. org`), so no URL could be found afterwards.
    private String removeDoi(String input) {
        boolean found = false;
        int index = indexOfIgnoreCase(input, doi, 0);
        while (index != -1) {
            found = true;
            String beforeDoi = input.substring(0, index);
            String afterDoi = input.substring(index + doi.length());

            // Checked first: a doi.org link also looks like the beginning of a URL
            Matcher prefixMatcher = DOI_PREFIX_AT_END_PATTERN.matcher(beforeDoi);
            if (prefixMatcher.find()) {
                return collapseWhitespace(prefixMatcher.replaceFirst("") + " " + afterDoi);
            }
            if (!URL_BEFORE_DOI_PATTERN.matcher(beforeDoi).find()) {
                return collapseWhitespace(beforeDoi + " " + afterDoi);
            }
            index = indexOfIgnoreCase(input, doi, index + doi.length());
        }
        if (found) {
            // The DOI only occurs inside other URLs. They stay whole, and findUrls takes them.
            return input;
        }
        // The DOI is written differently than its canonical form (for example percent-encoded)
        return collapseWhitespace(DOI.replaceInText(input, ""));
    }

    /// Finds `needle` in `text` ignoring case, starting at `fromIndex`. The result is an offset in `text` itself
    /// (no lowercased copy is made, so offsets cannot shift). Returns -1 if there is no match.
    private static int indexOfIgnoreCase(String text, String needle, int fromIndex) {
        int lastStart = text.length() - needle.length();
        for (int i = fromIndex; i <= lastStart; i++) {
            if (text.regionMatches(true, i, needle, 0, needle.length())) {
                return i;
            }
        }
        return -1;
    }

    private static String collapseWhitespace(String input) {
        return WHITESPACE_PATTERN.matcher(input).replaceAll(" ").strip();
    }

    /// Removes punctuation at the end of `url` that belongs to the sentence around it. A closing bracket is only
    /// removed if the URL has no opening partner for it: `https://en.wikipedia.org/wiki/Foo_(bar)` keeps its `)`,
    /// while `(see https://example.com/page)` loses it.
    private static String stripTrailingUrlPunctuation(String url) {
        String result = url;
        while (!result.isEmpty()) {
            char last = result.charAt(result.length() - 1);
            int bracketIndex = ")]}".indexOf(last);
            if (bracketIndex >= 0) {
                char opening = "([{".charAt(bracketIndex);
                if (count(result, opening) >= count(result, last)) {
                    break;
                }
            } else if (".,;:!?'\"".indexOf(last) < 0) {
                break;
            }
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static long count(String text, char character) {
        return text.chars().filter(c -> c == character).count();
    }

    /// Finds the publication year. A year in parentheses (`(1993)`, `(1991a)`) is preferred. Otherwise, the last
    /// valid year wins, because the publication year usually stands behind the title, which may mention a year, too.
    /// A year range (`2019–2021`) counts as its first year, and a number behind `p.`/`pp.` is a page, not a year.
    /// A range behind an already found year (`2020, 1725-1730`) is a page range and is left for [#findPages].
    ///
    /// Only the chosen occurrence is replaced by [#YEAR_TAG], so other occurrences (in the title) stay untouched.
    private String findYear(String input) {
        // [impl->req~import.plain-citation.year-and-pages~1]
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
            boolean isRange = matcher.end(YEAR_GROUP) < matcher.end();
            if (isRange && (start != -1)) {
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
        return (value > 1700) && (value <= Year.now().getValue());
    }

    /// Replaces the authors block at the beginning of `input` by [#AUTHOR_TAG]. Both name orders may be mixed
    /// (`Smith, J. and A. Doe`); `et al.` becomes `others`.
    private String findAuthors(String input) {
        // [impl->req~import.plain-citation.authors~1]
        Matcher labelMatcher = REFERENCE_LABEL_PATTERN.matcher(input);
        int labelEnd = labelMatcher.lookingAt() ? labelMatcher.end() : 0;

        Matcher lastNameFirstMatcher = LASTNAME_FIRST_AUTHOR_PATTERN.matcher(input);
        Matcher initialsFirstMatcher = INITIALS_FIRST_AUTHOR_PATTERN.matcher(input);

        int authorsEnd = labelEnd;
        while (true) {
            lastNameFirstMatcher.region(authorsEnd, input.length());
            initialsFirstMatcher.region(authorsEnd, input.length());

            Matcher authorMatcher;
            if (lastNameFirstMatcher.lookingAt()) {
                authorMatcher = lastNameFirstMatcher;
            } else if (initialsFirstMatcher.lookingAt()) {
                authorMatcher = initialsFirstMatcher;
            } else {
                break;
            }
            authors.add(generateAuthor(authorMatcher.group(LASTNAME_GROUP), authorMatcher.group(INITIALS_GROUP)));
            authorsEnd = authorMatcher.end();
        }

        if (authors.isEmpty()) {
            return findFullNameAuthors(input.substring(labelEnd));
        }

        String rest = input.substring(authorsEnd);
        Matcher etAlMatcher = ET_AL_PATTERN.matcher(rest);
        if (etAlMatcher.find()) {
            authors.add("others");
            rest = rest.substring(etAlMatcher.end());
        }
        return fixSpaces(AUTHOR_TAG + " " + rest);
    }

    /// Finds authors with full first names (`Marc Shapiro and Susan Horwitz.`) at the beginning of `input`.
    /// Returns `input` unchanged if the beginning is not such an authors block.
    private String findFullNameAuthors(String input) {
        Matcher blockMatcher = FULL_NAME_AUTHORS_PATTERN.matcher(input);
        if (!blockMatcher.find()) {
            return input;
        }

        String remainder = input.substring(blockMatcher.end()).stripLeading();
        if (!TITLE_END_PATTERN.matcher(remainder).find()
                && !QUOTED_TITLE_PATTERN.matcher(remainder).find()
                && !remainder.contains("//")) {
            return input;
        }

        List<String> fullNameAuthors = new ArrayList<>();
        for (String name : FULL_NAME_SEPARATOR_PATTERN.split(blockMatcher.group(AUTHORS_GROUP))) {
            Matcher nameMatcher = FULL_NAME_PATTERN.matcher(name.strip());
            if (!nameMatcher.matches()
                    || INITIALS_WITHOUT_PERIODS_PATTERN.matcher(nameMatcher.group(LASTNAME_GROUP)).matches()) {
                return input;
            }
            String familyName = collapseWhitespace(nameMatcher.group(LASTNAME_GROUP));
            if (nameMatcher.start(SUFFIX_GROUP) >= 0) {
                String suffix = nameMatcher.group(SUFFIX_GROUP);
                familyName += ", " + switch (suffix) {
                    case "Jr",
                         "Sr" ->
                            suffix + ".";
                    default ->
                            suffix;
                };
            }
            fullNameAuthors.add(familyName + ", " + collapseWhitespace(nameMatcher.group(FIRSTNAME_GROUP)));
        }

        if (blockMatcher.start(ET_AL_GROUP) >= 0) {
            fullNameAuthors.add("others");
        }
        authors.addAll(fullNameAuthors);
        return fixSpaces(AUTHOR_TAG + " " + remainder);
    }

    private String generateAuthor(String lastName, String initials) {
        // fixSpaces turns "W.-P." into "W. -P." - undo that
        String normalizedInitials = collapseWhitespace(SPACED_HYPHEN_PATTERN.matcher(initials).replaceAll("-"));
        return collapseWhitespace(lastName) + ", " + normalizedInitials;
    }

    /// Finds the pages. A range (`127-136`, `90–97`, `pp. 62-68`) is preferred, the last one wins because volume
    /// and issue numbers usually stand in front of the pages. A single number is only taken if it is marked as
    /// page (`p. 5`), otherwise the volume number would be mistaken as page.
    ///
    /// Pages are stored with the BibTeX range separator `--`.
    private String findPages(String input) {
        // [impl->req~import.plain-citation.year-and-pages~1]
        int start = -1;
        int end = -1;
        String foundPages = "";

        Matcher rangeMatcher = PAGES_RANGE_PATTERN.matcher(input);
        while (rangeMatcher.find()) {
            start = rangeMatcher.start();
            end = rangeMatcher.end();
            foundPages = rangeMatcher.group(FROM_GROUP) + "--" + rangeMatcher.group(TO_GROUP);
        }

        if (start == -1) {
            Matcher singleMatcher = PAGES_SINGLE_PATTERN.matcher(input);
            if (singleMatcher.find()) {
                start = singleMatcher.start();
                end = singleMatcher.end();
                foundPages = singleMatcher.group(PAGE_GROUP);
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
            titlePart = quotedTitleMatcher.group(TITLE_GROUP);
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
        String cleaned = collapseWhitespace(TAG_PATTERN.matcher(value).replaceAll(" "));
        String withoutLeading = LEADING_FIELD_PUNCTUATION_PATTERN.matcher(cleaned).replaceFirst("");
        Pattern trailing = stripTrailingPeriod
                           ? TRAILING_FIELD_PUNCTUATION_PATTERN
                           : TRAILING_FIELD_PUNCTUATION_WITHOUT_PERIOD_PATTERN;
        return trailing.matcher(withoutLeading).replaceFirst("");
    }

    private static boolean containsLetterOrDigit(String input) {
        return input.codePoints().anyMatch(Character::isLetterOrDigit);
    }
}
