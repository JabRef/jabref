package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jabref.logic.util.PdfUtils;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.UnknownEntryType;
import org.jabref.model.strings.LatexToUnicodeAdapter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The "merged author" is the first author value chosen by [PdfMergeMetadataImporter].
/// This class looks for its family names on the PDF's leading pages and may replace it with a later candidate
/// whose family names appear there.
@NullMarked
class PdfAuthorCrossCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfAuthorCrossCheck.class);

    private static final Pattern LEADING_AND_TRAILING_NON_LETTERS = Pattern.compile("^\\P{L}+|\\P{L}+$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern NON_LETTERS = Pattern.compile("\\P{L}+");
    private static final Pattern SOFT_LINE_BREAK_HYPHEN = Pattern.compile("-\\r?\\n\\s*");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern APOSTROPHES = Pattern.compile("['\u2019\u02BC]");
    private static final Set<String> NAME_LIST_LOWERCASE_WORDS = Set.of(
            "and", "others", "van", "von", "vom", "zu", "zur", "der", "den", "de", "del", "della", "dei", "des", "du", "dos", "das", "do", "da", "di",
            "la", "le", "ten", "ter", "af", "av", "y", "e", "bin", "binti", "bint", "ibn", "al", "el");

    private PdfAuthorCrossCheck() {
    }

    /// Extracts text from the PDF's first two pages so [PdfMergeMetadataImporter] can check for family names
    /// from entries returned by its PDF importers and online fetchers. Returns an empty string on failure.
    static String extractLeadingPagesText(PDDocument document) {
        try {
            StringJoiner pages = new StringJoiner("\n");
            for (int page = 1; page <= Math.min(2, document.getNumberOfPages()); page++) {
                pages.add(PdfUtils.getPageContents(document, page));
            }
            return pages.toString();
        } catch (IOException e) {
            LOGGER.debug("Could not extract text for the author plausibility check", e);
            return "";
        }
    }

    /// Replaces an unconfirmed creator-style author with the best author list confirmed by the PDF's leading
    /// pages. Bibliographic metadata and authors without extractable page text are preserved.
    ///
    /// [impl->req~import.pdf.author-confirmed-by-text~1]
    static void crossCheckAuthor(BibEntry entry, List<BibEntry> candidates, Set<BibEntry> citedWorks, @Nullable String leadingPagesText) {
        if (StringUtil.isBlank(leadingPagesText)) {
            return;
        }
        DocumentWords documentWords = DocumentWords.of(leadingPagesText);
        if (documentWords.isBlank()) {
            // A text of digits or punctuation confirms nothing
            return;
        }
        entry.getField(StandardField.AUTHOR)
             .ifPresent(mergedAuthor -> crossCheckMergedAuthor(entry, mergedAuthor, candidates, citedWorks, documentWords));
    }

    /// Preserves authors backed by bibliographic metadata or confirmed in the PDF text. Otherwise, selects
    /// the candidate with the most confirmed family names, or removes a lone unconfirmed author.
    /// The first author-bearing candidate identifies the merged author's source because merging keeps its value.
    // [impl->adr~conservative-pdf-author-evidence~1]
    private static void crossCheckMergedAuthor(BibEntry entry, String mergedAuthor, List<BibEntry> candidates,
                                               Set<BibEntry> citedWorks, DocumentWords documentWords) {
        if (isAuthorConfirmedByText(mergedAuthor, documentWords)) {
            return;
        }

        boolean sourceFound = false;
        Optional<ScoredAuthor> bestConfirmed = Optional.empty();
        for (BibEntry candidate : candidates) {
            Optional<String> candidateAuthor = candidate.getField(StandardField.AUTHOR);
            if (candidateAuthor.isEmpty()) {
                continue;
            }
            if (!sourceFound) {
                sourceFound = true;
                // The first author-bearing candidate supplied the merged author. Identity distinguishes a
                // reference-list entry from another candidate with identical fields.
                if (!citedWorks.contains(candidate)
                        && (candidate.getCitationKey().isPresent() || isKnownNonDefaultType(candidate.getType()))) {
                    return;
                }
            }

            String author = candidateAuthor.orElseThrow();
            if (looksLikeAuthorList(author)) {
                int confirmedNames = countFamilyNamesInText(author, documentWords);
                if (confirmedNames > bestConfirmed.map(ScoredAuthor::confirmedNames).orElse(0)) {
                    bestConfirmed = Optional.of(new ScoredAuthor(author, confirmedNames));
                }
            }
        }

        if (sourceFound) {
            bestConfirmed.ifPresentOrElse(
                    scored -> entry.setField(StandardField.AUTHOR, scored.value()),
                    () -> {
                        if (namedAuthors(mergedAuthor).count() == 1) {
                            entry.clearField(StandardField.AUTHOR);
                        }
                    });
        }
    }

    /// Dublin Core metadata of other tools may carry a generic type such as "Text", which says nothing about
    /// who wrote the author field; only a real entry type indicates metadata written by JabRef. The type is
    /// still only a proxy: exact would be tracking the origin of each field in the XMP reader.
    private static boolean isKnownNonDefaultType(EntryType type) {
        return !BibEntry.DEFAULT_TYPE.equals(type) && !(type instanceof UnknownEntryType);
    }

    private record ScoredAuthor(String value, int confirmedNames) {
    }

    /// Keeps sentence fragments mis-parsed as author lists (e.g. by this importer's first-page parsing) from
    /// being promoted by the cross-check: their words trivially occur in the document text. Real name lists
    /// consist of capitalized words plus name particles and the "and" separator, while prose contains
    /// other lowercase words and non-letter tokens such as URLs.
    private static boolean looksLikeAuthorList(String authorField) {
        for (String word : WHITESPACE.split(authorField)) {
            String stripped = LEADING_AND_TRAILING_NON_LETTERS.matcher(word).replaceAll("");
            if (stripped.isEmpty()
                    // The allowlist applies only to lowercase letters; caseless scripts such as CJK and Arabic pass.
                    || (Character.isLowerCase(stripped.codePointAt(0)) && !NAME_LIST_LOWERCASE_WORDS.contains(stripped))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAuthorConfirmedByText(String authorField, DocumentWords documentWords) {
        if (countFamilyNamesInText(authorField, documentWords) > 0) {
            return true;
        }
        // Fallback for author values AuthorList cannot split into proper persons (e.g. exotic separator
        // characters from broken XMP decoding). A single common word such as a given name also occurs in
        // unrelated text, so at least two distinct words of the raw value must occur.
        long confirmedWords = Arrays.stream(NON_LETTERS.split(normalizeForComparison(LatexToUnicodeAdapter.format(authorField))))
                                    .filter(word -> word.length() >= 2 && !NAME_LIST_LOWERCASE_WORDS.contains(word.toLowerCase(Locale.ROOT)))
                                    .distinct()
                                    .filter(documentWords::containsName)
                                    .count();
        return confirmedWords >= 2;
    }

    private static int countFamilyNamesInText(String authorField, DocumentWords documentWords) {
        return (int) namedAuthors(authorField)
                .map(Author::getFamilyName)
                .flatMap(Optional::stream)
                .filter(documentWords::containsName)
                .count();
    }

    /// A trailing "et al." parses to [Author#OTHERS]. It is neither a person that could be confirmed (its family
    /// name "others" is an ordinary word of prose) nor one that makes a creator-only author list look multi-person.
    private static Stream<Author> namedAuthors(String authorField) {
        return AuthorList.parse(authorField).getAuthors().stream()
                         .filter(author -> !Author.OTHERS.equals(author));
    }

    /// The leading-page text as normalized words separated and enclosed by single spaces, so that a whole-word
    /// match is a plain substring search, also for multi-word names such as "van der Berg".
    private record DocumentWords(String lowerCase, String caseSensitive) {
        static DocumentWords of(String text) {
            return new DocumentWords(toWordSequence(normalizeForComparison(text).toLowerCase(Locale.ROOT)),
                    toWordSequence(normalizeForComparison(text)));
        }

        boolean isBlank() {
            return lowerCase.isBlank();
        }

        boolean containsWords(String phrase) {
            String words = toWordSequence(normalizeForComparison(LatexToUnicodeAdapter.format(phrase)).toLowerCase(Locale.ROOT)).strip();
            return !words.isBlank() && lowerCase.contains(" " + words + " ");
        }

        /// A capitalized name must occur capitalized or in capitals, so that a family name such as "May" or
        /// "Young" is not confirmed by the ordinary word in prose.
        boolean containsName(String name) {
            // Metadata may encode accents as LaTeX ("B{\\\"o}hm"), the PDF text prints them ("Böhm")
            String words = toWordSequence(normalizeForComparison(LatexToUnicodeAdapter.format(name))).strip();
            if (words.isBlank() || !Character.isUpperCase(words.codePointAt(0))) {
                return containsWords(name);
            }
            int firstLength = Character.charCount(words.codePointAt(0));
            String capitalized = words.substring(0, firstLength) + words.substring(firstLength).toLowerCase(Locale.ROOT);
            return Stream.of(words, words.toUpperCase(Locale.ROOT), capitalized)
                         .anyMatch(variant -> caseSensitive.contains(" " + variant + " "));
        }

        private static String toWordSequence(String normalizedText) {
            return " " + String.join(" ", NON_LETTERS.split(normalizedText)).strip() + " ";
        }
    }

    /// Diacritic-, hyphen- and apostrophe-insensitive comparison form. Hyphens are removed on both sides because
    /// text extraction may break a name at the end of a justified line ("Breitenbü-\ncher"), where the
    /// hyphen is a soft line-break hyphen for one name but a genuine part of another (e.g. "Kylo-Ren").
    /// Apostrophes are removed instead of splitting words, so that "Connor" does not match "O'Connor".
    private static String normalizeForComparison(String text) {
        String joined = APOSTROPHES.matcher(SOFT_LINE_BREAK_HYPHEN.matcher(text).replaceAll("").replace("-", "")).replaceAll("");
        return COMBINING_MARKS.matcher(Normalizer.normalize(joined, Normalizer.Form.NFKD)).replaceAll("");
    }
}
