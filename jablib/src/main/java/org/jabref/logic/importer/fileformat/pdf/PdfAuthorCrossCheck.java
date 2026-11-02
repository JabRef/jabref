package org.jabref.logic.importer.fileformat.pdf;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.UnknownEntryType;
import org.jabref.model.strings.LatexToUnicodeAdapter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// The "merged author" is the first author value chosen by [PdfMergeMetadataImporter].
/// Without page text, it is preserved. Otherwise a plausible merged author confirmed by the PDF text is kept;
/// bibliographic evidence also protects an unconfirmed author. Remaining candidates are scored by confirmed
/// family names, and a lone unconfirmed author is removed if no candidate matches.
@NullMarked
class PdfAuthorCrossCheck {

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

    /// Replaces an unconfirmed creator-style author with the best author list confirmed by the PDF's leading
    /// pages. Bibliographic metadata and authors without extractable page text are preserved.
    // [impl->req~import.pdf.author-confirmed-by-text~1]
    static void crossCheckAuthor(BibEntry entry, List<BibEntry> candidates, @Nullable String leadingPagesText) {
        // [impl->req~import.pdf.author-preserved-without-text~1]
        if (StringUtil.isBlank(leadingPagesText)) {
            return;
        }

        DocumentWords documentWords = DocumentWords.of(leadingPagesText);
        if (documentWords.isBlank()) {
            // A text of digits or punctuation confirms nothing
            return;
        }

        entry.getField(StandardField.AUTHOR)
             .ifPresent(mergedAuthor -> crossCheckMergedAuthor(entry, mergedAuthor, candidates, documentWords));
    }

    /// Preserves authors backed by bibliographic metadata or confirmed in the PDF text. Otherwise, selects
    /// the candidate with the most confirmed family names, or removes a lone unconfirmed author.
    /// The first author-bearing candidate identifies the merged author's source because merging keeps its value.
    // [impl->adr~conservative-pdf-author-evidence~1]
    private static void crossCheckMergedAuthor(BibEntry entry, String mergedAuthor, List<BibEntry> candidates,
                                               DocumentWords documentWords) {
        // A sentence mis-parsed as author ("Written by Alice Smith") is not confirmed by one of its names
        if (looksLikeAuthorList(mergedAuthor) && isAuthorConfirmedByText(mergedAuthor, documentWords)) {
            return;
        }

        boolean firstAuthorSourceFound = false;
        Optional<String> bestConfirmedAuthor = Optional.empty();
        int maxConfirmedNames = 0;

        for (BibEntry candidate : candidates) {
            Optional<String> candidateAuthor = candidate.getField(StandardField.AUTHOR);
            if (candidateAuthor.isEmpty()) {
                continue;
            }

            if (!firstAuthorSourceFound) {
                firstAuthorSourceFound = true;
                // The first author-bearing candidate supplied the merged author
                // [impl->req~import.pdf.author-preserved-with-citation-key~1]
                // [impl->req~import.pdf.author-preserved-with-entry-type~1]
                if (candidate.getCitationKey().isPresent() || isKnownNonDefaultType(candidate.getType())) {
                    return;
                }
            }

            // [impl->req~import.pdf.author-best-confirmed-candidate~1]
            String author = candidateAuthor.get();
            if (looksLikeAuthorList(author)) {
                int confirmedNames = countFamilyNamesInText(author, documentWords);
                if (confirmedNames > maxConfirmedNames) {
                    maxConfirmedNames = confirmedNames;
                    bestConfirmedAuthor = Optional.of(author);
                }
            }
        }

        if (firstAuthorSourceFound) {
            if (bestConfirmedAuthor.isPresent()) {
                entry.setField(StandardField.AUTHOR, bestConfirmedAuthor.get());
            } else if (namedAuthors(mergedAuthor).count() == 1) {
                // [impl->req~import.pdf.author-unconfirmed-single-removed~1]
                entry.clearField(StandardField.AUTHOR);
            }
        }

        // [impl->req~import.pdf.author-unconfirmed-multiple-retained~1]
        // If no author was confirmed by text, the unconfirmed multiple authors are retained as is, method does nothing.
    }

    /// Dublin Core metadata of other tools may carry a generic type such as "Text", which says nothing about
    /// who wrote the author field; only a real entry type indicates metadata written by JabRef. The type is
    /// still only a proxy: exact would be tracking the origin of each field in the XMP reader.
    private static boolean isKnownNonDefaultType(EntryType type) {
        return !BibEntry.DEFAULT_TYPE.equals(type) && !(type instanceof UnknownEntryType);
    }

    /// Checks that the value looks like a list of person names rather than prose, so that sentence fragments
    /// mis-parsed as authors (e.g. `Written by Alice Smith`) are not confirmed merely because their words occur
    /// in the document text.
    ///
    /// Every whitespace-separated word, stripped of surrounding non-letters, must be non-empty and either
    /// start with an uppercase letter or be a name particle or `and` (see `NAME_LIST_LOWERCASE_WORDS`).
    /// Caseless scripts such as CJK pass. Example: `Ludwig van Beethoven and others` is accepted;
    /// `Written by Alice Smith`, `Alice Smith 2024` and URLs are rejected.
    // [impl->req~import.pdf.author-candidate-plausible~1]
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

    /// Checks if the author appears in the document text.
    private static boolean isAuthorConfirmedByText(String authorField, DocumentWords documentWords) {
        if (countFamilyNamesInText(authorField, documentWords) > 0) {
            return true;
        }
        // Fallback for author values AuthorList cannot split into proper persons (e.g. exotic separator
        // characters from broken XMP decoding). A single common word such as a given name also occurs in
        // unrelated text, so at least two distinct words of the raw value must occur. Single letters are
        // initials ("A.", "J.") and are found in any text.
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

    /// A trailing "et al." is neither a person that could be confirmed (its family name "others" is an ordinary
    /// word of prose) nor one that makes a creator-only author list look multi-person.
    private static Stream<Author> namedAuthors(String authorField) {
        return AuthorList.parse(authorField).getNamedAuthors().stream();
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

        /// A capitalized name must occur capitalized or in capitals, so that a family name such as "May" or
        /// "Young" is not confirmed by the ordinary word in prose. A name in a caseless script or in lowercase
        /// is matched ignoring case.
        boolean containsName(String name) {
            // Metadata may encode accents as LaTeX ("B{\\\"o}hm"), the PDF text prints them ("Böhm")
            String words = toWordSequence(normalizeForComparison(LatexToUnicodeAdapter.format(name))).strip();
            if (words.isBlank()) {
                return false;
            }
            if (!Character.isUpperCase(words.codePointAt(0))) {
                return lowerCase.contains(" " + words.toLowerCase(Locale.ROOT) + " ");
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
