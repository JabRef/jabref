package org.jabref.logic.importer.fileformat.pdf;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jspecify.annotations.NullMarked;

/// Picks the page of a book that carries its bibliographic block: the imprint page behind the title page,
/// or, in Ukrainian and other post-Soviet books, the imprint data printed on one of the last pages.
///
/// A page is scored by how many different kinds of imprint information it shows (see [#SIGNALS]).
/// One kind alone is common on ordinary pages (a preface mentions copyright, a back cover prints the ISBN),
/// so a page needs at least [#MIN_SIGNALS] different kinds to be accepted.
///
/// Reference lists and publisher catalogues show several signals too, but they describe other books.
/// They are recognised by a reference-list heading or by printing more ISBNs than one book has editions.
@NullMarked
final class BibliographicPageDetector {

    private static final int MIN_SIGNALS = 2;

    /// One ISBN per edition: print, hardcover, ebook, PDF.
    private static final int MAX_ISBNS = 4;

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    private static final Pattern ISBN = Pattern.compile("ISBN", FLAGS);

    // Classification codes printed on Ukrainian and Russian imprint pages, e.g. "УДК 371.388:53", "ББК 22.3я72"
    private static final Pattern UDC = Pattern.compile("(?<!\\p{L})(UDC|УДК)\\s*\\d", FLAGS);
    private static final Pattern BBK = Pattern.compile("(?<!\\p{L})(BBK|ББК)\\s*\\d", FLAGS);

    private static final Pattern COPYRIGHT = Pattern.compile("©|\\(c\\)\\s*\\d{4}|copyright", FLAGS);
    private static final Pattern RIGHTS_RESERVED = Pattern.compile("all rights reserved|усі права захищені|все права защищены|alle rechte vorbehalten", FLAGS);
    private static final Pattern PUBLISHER = Pattern.compile("publish(ed|er|ing)|видавництв|издательств|verlag", FLAGS);
    private static final Pattern PRINT_DETAILS = Pattern.compile("printed in|підписано до друку|подписано в печать|наклад\\s*\\d|тираж\\s*\\d", FLAGS);

    private static final List<Pattern> SIGNALS = List.of(ISBN, UDC, BBK, COPYRIGHT, RIGHTS_RESERVED, PUBLISHER, PRINT_DETAILS);

    private static final Pattern REFERENCE_LIST_HEADING = Pattern.compile(
            "\\A\\s*(references|bibliography|literature|works cited|література|литература|список .*(джерел|літератури|литературы))\\s*$",
            FLAGS | Pattern.MULTILINE);

    private BibliographicPageDetector() {
    }

    /// Returns the text of the page that is most likely the book's bibliographic page.
    ///
    /// @param pageTexts the text of each candidate page; on equal scores the earlier page wins
    /// @return the accepted page text, or an empty Optional if no page qualifies
    static Optional<String> findBibliographicPage(List<String> pageTexts) {
        return pageTexts.stream()
                        .filter(pageText -> score(pageText) >= MIN_SIGNALS)
                        .reduce((best, pageText) -> score(pageText) > score(best) ? pageText : best);
    }

    private static int score(String pageText) {
        if (REFERENCE_LIST_HEADING.matcher(pageText).find() || ISBN.matcher(pageText).results().count() > MAX_ISBNS) {
            return 0;
        }
        return (int) SIGNALS.stream()
                            .filter(signal -> signal.matcher(pageText).find())
                            .count();
    }
}
