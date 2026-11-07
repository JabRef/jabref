package org.jabref.logic.importer.fileformat.pdf;

import java.util.List;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.jspecify.annotations.NullMarked;

/// Finds the citation of a book on its bibliographic page and reads publisher and year from it.
///
/// Supported is the Ukrainian cataloguing-in-publication record printed on the imprint page:
///
/// ```
/// Коваль О. П.
/// К56 Збірник задач з фізики для 8 класу / О. П. Коваль, Н. І. Ткач. — 2-ге вид., перероб. — Харків : Ранок, 2010. — 176 с.
/// ```
///
/// The record is anchored on its publication area, "Place : Publisher, Year", which follows a full stop and a dash.
/// Dates elsewhere on the page (approval of the textbook, copyright, print date) are never used.
/// A page without a publication area, or with more than one, yields nothing:
/// a missing field is better than a wrong one.
@NullMarked
final class BookCitationBlock {

    /// How far above the publication area the title line is looked for,
    /// and how far above the title line the author heading is looked for.
    private static final int MAX_LINES_ABOVE = 3;

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    // PDF text often has no-break spaces, which \s does not match
    private static final String SPACE = "[\\s\\h]";

    // Full stop and dash between two areas of the citation, e.g. ". — ". The dash is an em dash, an en dash or a hyphen.
    private static final String AREA_SEPARATOR = "\\." + SPACE + "+[\\u2014\\u2013-]" + SPACE + "+";

    // At most three words, so that a sentence after an area separator is not taken for a place.
    // A word starts with a letter, so the edition area ("2-ге вид.") is not taken for a place either.
    private static final String PLACE_WORD = "\\p{L}[\\p{L}'\\u2019\\u02BC.-]*";
    private static final String PLACE = PLACE_WORD + "(?:" + SPACE + "+" + PLACE_WORD + "){0,2}";

    // The publisher has no digit, dash or copyright sign, so it cannot run on into the pages area or other lines.
    // The optional pages area ("— 176 с.") is only matched to know where the citation ends.
    private static final Pattern PUBLICATION_AREA = Pattern.compile(
            AREA_SEPARATOR + PLACE + SPACE + "+:" + SPACE + "+(?<publisher>[^\\d\\u2014\\u2013©]+?)," + SPACE + "*(?<year>\\d{4})(?!\\d)"
                    + "(?:" + AREA_SEPARATOR + "\\d+" + SPACE + "*с\\.)?",
            FLAGS);

    private static final Pattern EDITION = Pattern.compile(AREA_SEPARATOR + "(?<edition>\\d+)-\\p{L}{1,3}" + SPACE + "+вид\\.", FLAGS);

    private static final Pattern TITLE_SEPARATOR = Pattern.compile("\\h/(?:\\h|$)");

    // Without case-insensitive flags, because \p{Lu} would then match lowercase letters, too
    private static final Pattern AUTHOR_HEADING = Pattern.compile("\\h*\\p{Lu}[\\p{L}'\\u2019\\u02BC-]+,?\\h+\\p{Lu}\\.(?:\\h*\\p{Lu}\\.)?\\h*");

    // Lines before the citation: classification, approval, reviewers, copyright and print details
    private static final Pattern STOP_LINE = Pattern.compile(
            "©|(?<!\\p{L})(?:УДК|ББК|Рекомендовано|Схвалено|Рецензент|наказ|ISBN|Підписано)", FLAGS);

    // ISBD puts a colon or semicolon before a second place or publisher
    private static final Pattern SEVERAL_PUBLISHERS = Pattern.compile("[:;]");

    private static final Pattern WHITESPACE = Pattern.compile(SPACE + "+");
    private static final Pattern LINE_BREAK = Pattern.compile("\\R");

    private BookCitationBlock() {
    }

    /// Returns the lines of the citation, from the author heading ("Surname I. I.") or the title line
    /// to the end of the publication area, which is the pages area if one follows it.
    ///
    /// The title line is the nearest line with a " / " between title and authors, looked for on the line of the
    /// publication area and up to three lines above it. The author heading is looked for up to three lines above
    /// the title line. Both searches stop at a line that cannot belong to a citation, such as a reviewer list,
    /// and leave it out. Without a title line, the citation is the line of the publication area.
    ///
    /// @param bibliographicPageText the text of the page selected by [BibliographicPageDetector]
    /// @return the lines joined with `\n`, or an empty Optional if the page does not hold exactly one publication area
    static Optional<String> find(String bibliographicPageText) {
        String text = LINE_BREAK.matcher(bibliographicPageText).replaceAll("\n");
        return findSingle(PUBLICATION_AREA, text).map(area -> {
            List<String> lines = text.lines().toList();
            int firstLine = findFirstLine(lines, lineNumber(text, area.start()));
            int lastLine = lineNumber(text, area.end());
            return String.join("\n", lines.subList(firstLine, lastLine + 1));
        });
    }

    /// Returns the publisher of the publication area, with its quotes and without place and year.
    /// A line break inside the publisher counts as one space.
    ///
    /// @param citationBlock the citation found by [#find], or a whole page
    /// @return the publisher, or an empty Optional if there is no single publication area or it names several publishers
    // [impl->req~import.pdf.book-citation-publication~1]
    static Optional<String> findPublisher(String citationBlock) {
        return findSingle(PUBLICATION_AREA, citationBlock)
                .map(area -> WHITESPACE.matcher(area.group("publisher")).replaceAll(" ").strip())
                .filter(publisher -> !publisher.isEmpty())
                .filter(publisher -> !SEVERAL_PUBLISHERS.matcher(publisher).find());
    }

    /// Returns the year of the publication area. Other years of the page are not considered.
    ///
    /// @param citationBlock the citation found by [#find], or a whole page
    /// @return the four-digit year, or an empty Optional if there is no single publication area
    // [impl->req~import.pdf.book-citation-publication~1]
    static Optional<String> findYear(String citationBlock) {
        return findSingle(PUBLICATION_AREA, citationBlock)
                .map(area -> area.group("year"));
    }

    /// Returns the number of the edition area, such as "2" for "2-ге вид., перероб.".
    ///
    /// @param citationBlock the citation found by [#find]
    /// @return the edition number, or an empty Optional if there is no single edition area
    static Optional<String> findEdition(String citationBlock) {
        return findSingle(EDITION, citationBlock)
                .map(edition -> edition.group("edition"));
    }

    private static Optional<MatchResult> findSingle(Pattern pattern, String text) {
        List<MatchResult> matches = pattern.matcher(text).results().limit(2).toList();
        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }

    private static int findFirstLine(List<String> lines, int areaLine) {
        for (int line = areaLine; line >= Math.max(0, areaLine - MAX_LINES_ABOVE); line--) {
            if (line < areaLine && STOP_LINE.matcher(lines.get(line)).find()) {
                break;
            }
            if (TITLE_SEPARATOR.matcher(lines.get(line)).find()) {
                return findHeadingLine(lines, line);
            }
        }
        return areaLine;
    }

    private static int findHeadingLine(List<String> lines, int titleLine) {
        for (int line = titleLine - 1; line >= Math.max(0, titleLine - MAX_LINES_ABOVE); line--) {
            String text = lines.get(line);
            if (STOP_LINE.matcher(text).find()) {
                break;
            }
            if (AUTHOR_HEADING.matcher(text).matches()) {
                return line;
            }
        }
        return titleLine;
    }

    private static int lineNumber(String text, int offset) {
        return (int) text.substring(0, offset).chars().filter(character -> character == '\n').count();
    }
}
