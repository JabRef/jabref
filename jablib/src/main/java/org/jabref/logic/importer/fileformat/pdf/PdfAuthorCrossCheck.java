package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Guesses which of several author candidates is the real author list of a PDF by checking the candidates
/// against the text of the document's leading pages. Used by [PdfMergeMetadataImporter] after merging
/// the candidates of the individual [PdfImporter]s.
@NullMarked
class PdfAuthorCrossCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfAuthorCrossCheck.class);

    private static final Pattern LEADING_AND_TRAILING_NON_LETTERS = Pattern.compile("^\\P{L}+|\\P{L}+$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern NON_LETTERS = Pattern.compile("\\P{L}+");
    private static final Pattern SOFT_LINE_BREAK_HYPHEN = Pattern.compile("-\\r?\\n\\s*");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Set<String> NAME_LIST_LOWERCASE_WORDS = Set.of(
            "and", "van", "von", "der", "den", "de", "del", "dos", "da", "di", "la", "le", "ten", "ter", "y", "e");

    private PdfAuthorCrossCheck() {
    }

    /// Extracts the plain text of a PDF's leading pages. Used by [PdfMergeMetadataImporter] to cross-check
    /// author candidates against what the document itself prints. Returns an empty string on failure.
    static String extractLeadingPagesText(PDDocument document) {
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setEndPage(Math.min(2, document.getNumberOfPages()));
            return stripper.getText(document);
        } catch (IOException e) {
            LOGGER.debug("Could not extract text for the author plausibility check", e);
            return "";
        }
    }

    /// Office suites store the account name of whoever produced the file as "Author" in the document
    /// information dictionary. Via the XMP/docinfo candidate, this person — usually not an author of the
    /// work at all — would win the merge in [PdfMergeMetadataImporter] against author lists extracted from
    /// the document itself.
    ///
    /// Since scholarly works print their authors on the leading pages, an author list is trusted only if at
    /// least one of its family names occurs in the text of those pages. An unconfirmed merged value is
    /// replaced by the best-confirmed candidate value. If no candidate is confirmed, a single person coming
    /// from a non-bibliographic candidate (no citation key, no explicit entry type) is dropped entirely: a
    /// wrong author is worse than none. Entries with a citation key or explicit type are left untouched so
    /// that metadata previously written by JabRef survives re-import even when the PDF text does not
    /// contain the author (e.g. slides or reports).
    static void crossCheckAuthor(BibEntry entry, List<BibEntry> candidates, @Nullable String leadingPagesText) {
        if (StringUtil.isBlank(leadingPagesText)) {
            return;
        }
        String normalizedText = normalizeForComparison(leadingPagesText);
        if (normalizedText.isBlank()) {
            // Normalization strips everything but letters, so a text of digits or punctuation confirms nothing
            return;
        }
        entry.getField(StandardField.AUTHOR).ifPresent(mergedAuthor -> {
            if (isAuthorConfirmedByText(mergedAuthor, normalizedText)) {
                return;
            }

            candidates.stream()
                      .flatMap(candidate -> candidate.getField(StandardField.AUTHOR).stream())
                      .filter(PdfAuthorCrossCheck::looksLikeAuthorList)
                      .map(author -> new ScoredAuthor(author, countFamilyNamesInText(author, normalizedText)))
                      .filter(scored -> scored.confirmedNames() > 0)
                      // keeps the earlier (= higher-priority) candidate unless a later one is strictly better
                      .reduce((first, second) -> second.confirmedNames() > first.confirmedNames() ? second : first)
                      .ifPresentOrElse(
                              scored -> entry.setField(StandardField.AUTHOR, scored.value()),
                              () -> dropCreatorOnlyAuthor(entry, candidates, mergedAuthor));
        });
    }

    private static void dropCreatorOnlyAuthor(BibEntry entry, List<BibEntry> candidates, String mergedAuthor) {
        // The merge is first-wins, thus the merged value stems from the first candidate carrying an author
        boolean sourceLooksBibliographic = candidates.stream()
                                                     .filter(candidate -> candidate.hasField(StandardField.AUTHOR))
                                                     .findFirst()
                                                     .map(source -> source.getCitationKey().isPresent()
                                                             || !BibEntry.DEFAULT_TYPE.equals(source.getType()))
                                                     .orElse(true);
        if (!sourceLooksBibliographic && (namedAuthors(mergedAuthor).count() == 1)) {
            entry.clearField(StandardField.AUTHOR);
        }
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
                    || (!Character.isUpperCase(stripped.codePointAt(0)) && !NAME_LIST_LOWERCASE_WORDS.contains(stripped))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAuthorConfirmedByText(String authorField, String normalizedText) {
        if (countFamilyNamesInText(authorField, normalizedText) > 0) {
            return true;
        }
        // Fallback for author values AuthorList cannot split into proper persons (e.g. exotic separator
        // characters from broken XMP decoding): any word of the raw value found in the text confirms it.
        return Arrays.stream(NON_LETTERS.split(authorField))
                     .filter(word -> word.length() >= 2)
                     .map(PdfAuthorCrossCheck::normalizeForComparison)
                     .anyMatch(word -> !word.isBlank() && containsWord(normalizedText, word));
    }

    private static int countFamilyNamesInText(String authorField, String normalizedText) {
        return (int) namedAuthors(authorField)
                .map(Author::getFamilyName)
                .flatMap(Optional::stream)
                .map(PdfAuthorCrossCheck::normalizeForComparison)
                .filter(familyName -> !familyName.isBlank() && containsWord(normalizedText, familyName))
                .count();
    }

    /// A trailing "et al." parses to [Author#OTHERS]. It is neither a person that could be confirmed (its family
    /// name "others" is an ordinary word of prose) nor one that makes a creator-only author list look multi-person.
    private static Stream<Author> namedAuthors(String authorField) {
        return AuthorList.parse(authorField).getAuthors().stream()
                         .filter(author -> !Author.OTHERS.equals(author));
    }

    /// Whole-word check: the occurrence must not be preceded or followed by another letter
    private static boolean containsWord(String normalizedText, String word) {
        int index = normalizedText.indexOf(word);
        while (index >= 0) {
            int end = index + word.length();
            if ((index == 0 || !Character.isLetter(normalizedText.codePointBefore(index)))
                    && (end >= normalizedText.length() || !Character.isLetter(normalizedText.codePointAt(end)))) {
                return true;
            }
            index = normalizedText.indexOf(word, index + 1);
        }
        return false;
    }

    /// Case-, diacritic- and hyphen-insensitive comparison form. Hyphens are removed on both sides because
    /// text extraction may break a name at the end of a justified line ("Breitenbü-\ncher"), where the
    /// hyphen is a soft line-break hyphen for one name but a genuine part of another (e.g. "Kylo-Ren").
    private static String normalizeForComparison(String text) {
        String dehyphenated = SOFT_LINE_BREAK_HYPHEN.matcher(text).replaceAll("").replace("-", "");
        return COMBINING_MARKS.matcher(Normalizer.normalize(dehyphenated, Normalizer.Form.NFKD))
                              .replaceAll("")
                              .toLowerCase(Locale.ROOT);
    }
}
