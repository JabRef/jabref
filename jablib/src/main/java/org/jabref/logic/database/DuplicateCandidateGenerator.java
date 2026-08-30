package org.jabref.logic.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Generates the candidate pairs that are afterwards verified by [DuplicateCheck#isDuplicate].
///
/// Small libraries are searched exhaustively, i.e., every pair of entries is a candidate.
/// For larger libraries, comparing all `n * (n - 1) / 2` pairs is too slow (see
/// [issue 16579](https://github.com/JabRef/jabref/issues/16579)). There, a blocking stage
/// (see [doi:10.1126/sciadv.abi8021](https://doi.org/10.1126/sciadv.abi8021)) assigns each entry a set
/// of redundant blocking keys derived from its identifiers, title, and author/editor names.
/// Only pairs sharing at least one key become candidates.
/// See [ADR-0070](https://github.com/JabRef/jabref/blob/main/docs/decisions/0070-use-deterministic-blocking-for-duplicate-candidate-generation.md) for the rationale.
///
/// The keys are chosen to mirror the signals [DuplicateCheck] relies on, so that hardly any pair
/// that would be classified as a duplicate is skipped:
///
///   - [DuplicateCheck] treats two field values as equal when
///     [org.jabref.logic.util.strings.StringSimilarity#correlateByWords] exceeds `0.8`. That measure
///     compares words position by position, so similar titles agree on their leading words unless the
///     titles are very long. Keying on each of the leading title words (and analogously on the leading
///     author/editor last names) therefore blocks such pairs together even when single words differ.
///   - The entry type is never used for blocking, because different sources may use different types
///     (e.g., `InCollection` vs. `InProceedings`) for the same publication.
///   - Entries without any key (no identifier, title, author, or editor) are compared with all other
///     entries.
///
/// [impl->req~logic.duplicates.candidate-blocking~1]
@NullMarked
public class DuplicateCandidateGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicateCandidateGenerator.class);

    /// Libraries with at most this many entries are searched exhaustively.
    /// 1000 entries correspond to 499500 pairs, which [DuplicateCheck] handles quickly.
    private static final int EXHAUSTIVE_SEARCH_LIMIT = 1000;

    /// A title pair with more than 20 words is the only one that could evade all four keys
    /// while still being considered similar by DuplicateCheck.
    private static final int TITLE_KEY_WORDS = 4;

    /// Analogous bound for last names: only pairs with more than 10 persons could evade both keys.
    private static final int NAME_KEY_WORDS = 2;

    /// Marks keys whose equality alone already decides the duplicate question (identifiers, ISBN).
    /// [DuplicateCheck#isDuplicate] accepts any pair with a matching identifier, so blocks of such
    /// keys must never be dropped, no matter how large they are.
    private static final String EXACT_KEY_PREFIX = "id:";

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern NOT_LETTER_OR_DIGIT = Pattern.compile("[^\\p{L}\\p{N}]");

    private DuplicateCandidateGenerator() {
    }

    /// A pair of entries that potentially are duplicates of each other.
    /// The order of `first` and `second` follows the order of the entries in the searched list.
    public record CandidatePair(BibEntry first, BibEntry second) {
    }

    /// Determines the pairs of entries that need to be checked by [DuplicateCheck#isDuplicate].
    /// The returned stream is partially lazy, so callers can begin verifying (and cancel) without
    /// all candidates having been materialized.
    ///
    /// @param entries the entries to search, typically a snapshot of a library
    /// @return all pairs that potentially are duplicates
    public static Stream<CandidatePair> getCandidatePairs(List<BibEntry> entries) {
        return getCandidatePairs(entries, EXHAUSTIVE_SEARCH_LIMIT);
    }

    /// Visible for tests to force the blocking stage with small entry lists.
    static Stream<CandidatePair> getCandidatePairs(List<BibEntry> entries, int exhaustiveSearchLimit) {
        if (entries.size() <= exhaustiveSearchLimit) {
            return getAllPairs(entries);
        }
        return getBlockedPairs(entries);
    }

    private static Stream<CandidatePair> getAllPairs(List<BibEntry> entries) {
        return IntStream.range(0, entries.size() - 1)
                        .boxed()
                        .flatMap(first -> IntStream.range(first + 1, entries.size())
                                                   .mapToObj(second -> new CandidatePair(entries.get(first), entries.get(second))));
    }

    private static Stream<CandidatePair> getBlockedPairs(List<BibEntry> entries) {
        Map<String, List<Integer>> blocks = new HashMap<>();
        Set<Integer> entriesWithoutKeys = new HashSet<>();
        for (int i = 0; i < entries.size(); i++) {
            Set<String> keys = getBlockingKeys(entries.get(i));
            if (keys.isEmpty()) {
                entriesWithoutKeys.add(i);
            } else {
                for (String key : keys) {
                    blocks.computeIfAbsent(key, unused -> new ArrayList<>()).add(i);
                }
            }
        }

        // Safeguard against unusually large blocks: a fuzzy key shared by a large part of the library
        // (e.g., all titles starting with "The") carries no identifying information, but would emit
        // quadratically many pairs. True duplicates share several aligned words, so the pair still
        // meets through its other, more discriminative keys. Blocks of exact identifier keys are
        // never dropped, because a shared identifier alone already makes a pair a duplicate.
        int maximumBlockSize = Math.max(100, entries.size() / 50);

        List<CandidatePair> blockPairs = new ArrayList<>();
        Set<Long> seenPairs = new HashSet<>();
        for (Map.Entry<String, List<Integer>> blockEntry : blocks.entrySet()) {
            if (Thread.currentThread().isInterrupted()) {
                return blockPairs.stream();
            }
            List<Integer> block = blockEntry.getValue();
            if ((block.size() > maximumBlockSize) && !blockEntry.getKey().startsWith(EXACT_KEY_PREFIX)) {
                LOGGER.debug("Skipping block '{}' with {} entries", blockEntry.getKey(), block.size());
                continue;
            }
            for (int first = 0; first < (block.size() - 1); first++) {
                for (int second = first + 1; second < block.size(); second++) {
                    int lower = Math.min(block.get(first), block.get(second));
                    int higher = Math.max(block.get(first), block.get(second));
                    if (seenPairs.add(((long) lower << 32) | higher)) {
                        blockPairs.add(new CandidatePair(entries.get(lower), entries.get(higher)));
                    }
                }
            }
        }
        LOGGER.debug("Blocking reduced {} entries to {} block-based candidate pairs plus {} keyless entries compared exhaustively (an exhaustive search would check {} pairs)",
                entries.size(), blockPairs.size(), entriesWithoutKeys.size(), ((long) entries.size() * (entries.size() - 1)) / 2);

        // An entry without any blocking key can still be a duplicate (e.g., two entries only
        // containing the same journal and year), so it is compared with every other entry.
        // These pairs are streamed lazily: a keyless entry occurs in no block, so its pairs cannot
        // collide with the block pairs, and materializing them (worst case quadratic when most
        // entries are keyless) is not needed for de-duplication.
        Stream<CandidatePair> keylessPairs = entriesWithoutKeys.stream()
                .flatMap(withoutKeys -> IntStream.range(0, entries.size())
                        .filter(other -> (other > withoutKeys) || ((other != withoutKeys) && !entriesWithoutKeys.contains(other)))
                        .mapToObj(other -> new CandidatePair(entries.get(Math.min(withoutKeys, other)), entries.get(Math.max(withoutKeys, other)))));
        return Stream.concat(blockPairs.stream(), keylessPairs);
    }

    private static Set<String> getBlockingKeys(BibEntry entry) {
        Set<String> keys = new HashSet<>();
        for (Field field : entry.getFields()) {
            if (field.getProperties().contains(FieldProperty.IDENTIFIER)) {
                entry.getField(field).ifPresent(value -> {
                    if (field == StandardField.DOI) {
                        // DOI values may be given as plain DOI, URL, or contain LaTeX escapes - canonicalize them
                        keys.add(EXACT_KEY_PREFIX + "doi:" + DOI.parse(value).map(DOI::asString).orElse(value).toLowerCase(Locale.ROOT));
                    } else {
                        keys.add(EXACT_KEY_PREFIX + field.getName() + ":" + value.trim());
                    }
                });
            }
        }
        // Lower-cased because ISBN equality ignores the case of the ISBN-10 check digit "X"
        entry.getISBN().ifPresent(isbn -> keys.add(EXACT_KEY_PREFIX + "isbn:" + isbn.asString().toLowerCase(Locale.ROOT)));
        addWordKeys(keys, "title", entry.getFieldLatexFree(StandardField.TITLE), TITLE_KEY_WORDS);
        addNameKeys(keys, "author", entry.getFieldLatexFree(StandardField.AUTHOR));
        addNameKeys(keys, "editor", entry.getFieldLatexFree(StandardField.EDITOR));
        return keys;
    }

    private static void addNameKeys(Set<String> keys, String keyPrefix, Optional<String> value) {
        // Same normalization as DuplicateCheck#compareAuthorField, so that the keys align with its word positions
        addWordKeys(keys, keyPrefix, value.map(names -> AuthorList.fixAuthorLastNameOnlyCommas(names, false).replace(" and ", " ")), NAME_KEY_WORDS);
    }

    /// Adds one key per leading word of `value`. The word position is part of the key, because
    /// [org.jabref.logic.util.strings.StringSimilarity#correlateByWords] also compares words position by position.
    private static void addWordKeys(Set<String> keys, String keyPrefix, Optional<String> value, int wordCount) {
        if (value.isEmpty()) {
            return;
        }
        String[] words = WHITESPACE.split(value.get().trim());
        for (int position = 0; (position < wordCount) && (position < words.length); position++) {
            String normalized = NOT_LETTER_OR_DIGIT.matcher(words[position].toLowerCase(Locale.ROOT)).replaceAll("");
            if (!normalized.isEmpty()) {
                keys.add(keyPrefix + position + ":" + normalized);
            }
        }
    }
}
