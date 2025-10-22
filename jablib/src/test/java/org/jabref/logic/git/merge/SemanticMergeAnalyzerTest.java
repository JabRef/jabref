package org.jabref.logic.git.merge;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jabref.logic.git.merge.planning.SemanticMergeAnalyzer;
import org.jabref.logic.git.model.MergeAnalysis;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SemanticMergeAnalyzerTest {
    @ParameterizedTest
    @MethodSource
    void semanticEntryLevelConflicts(String description,
                                     BibDatabaseContext baseDb,
                                     BibDatabaseContext localDb,
                                     BibDatabaseContext remoteDb,
                                     Consumer<MergeAnalysis> verify) {
        MergeAnalysis analysis = SemanticMergeAnalyzer.analyze(baseDb, localDb, remoteDb);
        verify.accept(analysis);
    }

    @ParameterizedTest
    @MethodSource
    void semanticFieldLevelConflicts(String description,
                                     BibDatabaseContext baseDb,
                                     BibDatabaseContext localDb,
                                     BibDatabaseContext remoteDb,
                                     Consumer<MergeAnalysis> verify) {
        MergeAnalysis analysis = SemanticMergeAnalyzer.analyze(baseDb, localDb, remoteDb);
        verify.accept(analysis);
    }

    static Stream<Arguments> semanticEntryLevelConflicts() {
        BibEntry a_base_A = entryAWithAuthorAndTitle("base", "A");
        BibEntry a_local_A = entryAWithAuthorAndTitle("local", "A");
        BibEntry a_remote_A = entryAWithAuthorAndTitle("remote", "A");
        BibEntry a_same_A = entryAWithAuthorAndTitle("same", "A");
        BibEntry a_local_only = entryAWithAuthor("local");
        BibEntry a_remote_journal = entryAWithJournal("Remote Journal");
        BibEntry a_year_2025 = entryAWithAuthorTitleYear("base", "A", "2025");
        BibEntry a_title_B = entryAWithAuthorAndTitle("base", "B");
        BibEntry a_common = entryAWithAuthor("common");

        return Stream.of(
                Arguments.of("E01 - entry a does not exist anywhere",
                        emptyDb(),
                        emptyDb(),
                        emptyDb(),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertPlanEmpty(analysis);
                        }),

                Arguments.of("E02 - entry a added remotely only",
                        emptyDb(),
                        emptyDb(),
                        dbOf(a_remote_A),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertHasNewEntry(analysis, a_remote_A);
                        }),

                Arguments.of("E03 - entry a added locally only",
                        emptyDb(),
                        dbOf(a_local_A),
                        emptyDb(),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertPlanEmpty(analysis);
                        }),

                Arguments.of("E04a - both sides added entry a with identical content",
                        emptyDb(),
                        dbOf(a_same_A),
                        dbOf(a_same_A),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                        }),

                // local: author = {local},, remote: journal = {Remote Journal},
                Arguments.of("E04b - both added entry a but changed different fields",
                        emptyDb(),
                        dbOf(a_local_only),
                        dbOf(a_remote_journal),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertPatchEquals(analysis, "a", StandardField.JOURNAL, "Remote Journal");
                        }),

                Arguments.of("E04c - both added entry a with conflicting field values",
                        emptyDb(),
                        dbOf(a_local_A),
                        dbOf(a_remote_A),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertHasConflicts(analysis);
                        }),

                Arguments.of("E05 - entry a was deleted by both",
                        dbOf(a_base_A),
                        emptyDb(),
                        emptyDb(),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertDeletes(analysis, Set.of("a"));
                        }),

                Arguments.of("E06 - local deleted entry a, remote kept it unchanged",
                        dbOf(a_base_A),
                        emptyDb(),
                        dbOf(a_base_A),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertPlanEmpty(analysis);
                        }),

                Arguments.of("E07 - local deleted entry a, remote modified it",
                        dbOf(a_base_A),
                        emptyDb(),
                        dbOf(entryAWithAuthorAndTitle("remote", "A")),
                        (Consumer<MergeAnalysis>) analysis -> assertHasConflicts(analysis)),

                Arguments.of("E08 - remote deleted entry a, local kept it unchanged",
                        dbOf(a_base_A),
                        dbOf(a_base_A),
                        emptyDb(),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertDeletes(analysis, Set.of("a"));
                        }),

                Arguments.of("E09 - entry a unchanged in all three",
                        dbOf(a_base_A),
                        dbOf(a_base_A),
                        dbOf(a_base_A),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertPlanEmpty(analysis);
                        }),

                Arguments.of("E10a - remote modified a different field, local unchanged",
                        dbOf(a_base_A),
                        dbOf(a_base_A),
                        dbOf(a_year_2025),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertPatchEquals(analysis, "a", StandardField.YEAR, "2025");
                        }),

                Arguments.of("E10b - remote modified same field, local unchanged",
                        dbOf(a_base_A),
                        dbOf(a_base_A),
                        dbOf(a_remote_A),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertPatchEquals(analysis, "a", StandardField.AUTHOR, "remote");
                        }),

                Arguments.of("E11 - remote deleted entry a, local modified it",
                        dbOf(a_base_A),
                        dbOf(a_local_A),
                        emptyDb(),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertHasConflicts(analysis);
                            assertFalse(analysis.autoPlan().deletedEntryKeys().contains("a"),
                                    "Unsafe delete must not appear in auto plan when local modified");
                        }),

                Arguments.of("E12 - local modified entry a, remote unchanged",
                        dbOf(a_base_A),
                        dbOf(a_local_A),
                        dbOf(a_base_A),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertPlanEmpty(analysis);
                        }),

                Arguments.of("E13a - both modified entry a but changed different fields",
                        dbOf(a_base_A),
                        dbOf(a_local_A),
                        dbOf(a_title_B),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertPatchEquals(analysis, "a", StandardField.TITLE, "B");
                        }),

                Arguments.of("E13b - both changed same field to same value",
                        dbOf(entryAWithAuthor("base")),
                        dbOf(a_common),
                        dbOf(a_common),
                        (Consumer<MergeAnalysis>) analysis -> assertNoConflicts(analysis)),

                Arguments.of("E13c - both changed same field differently",
                        dbOf(entryAWithAuthor("base")),
                        dbOf(entryAWithAuthor("local")),
                        dbOf(entryAWithAuthor("remote")),
                        (Consumer<MergeAnalysis>) analysis -> assertHasConflicts(analysis)),

                Arguments.of("E14a - citationKey changed in local",
                        dbOf(entryAWithAuthor("base")),
                        dbOf(new BibEntry(StandardEntryType.Article)
                                .withCitationKey("b")
                                .withField(StandardField.AUTHOR, "base")),
                        dbOf(entryAWithAuthor("base")),
                        (Consumer<MergeAnalysis>) analysis -> assertNoConflicts(analysis)),

                Arguments.of("E14b - citationKey changed in remote",
                        dbOf(entryAWithAuthor("base")),
                        dbOf(entryAWithAuthor("base")),
                        dbOf(new BibEntry(StandardEntryType.Article)
                                .withCitationKey("b")
                                .withField(StandardField.AUTHOR, "base")),
                        (Consumer<MergeAnalysis>) analysis -> assertNoConflicts(analysis)),

                Arguments.of("E14c - citationKey renamed differently both sides",
                        dbOf(entryAWithAuthor("base")),
                        dbOf(new BibEntry(StandardEntryType.Article)
                                .withCitationKey("b")
                                .withField(StandardField.AUTHOR, "base")),
                        dbOf(new BibEntry(StandardEntryType.Article)
                                .withCitationKey("c")
                                .withField(StandardField.AUTHOR, "base")),
                        (Consumer<MergeAnalysis>) analysis -> assertNoConflicts(analysis)),

                Arguments.of("E15 - both added same citationKey with different content",
                        emptyDb(),
                        dbOf(entryAWithAuthor("local")),
                        dbOf(entryAWithAuthor("remote")),
                        (Consumer<MergeAnalysis>) analysis -> assertHasConflicts(analysis))
        );
    }

    static Stream<Arguments> semanticFieldLevelConflicts() {
        BibEntry a_base_author = entryAWithAuthor("base");
        BibEntry a_local_author = entryAWithAuthor("local");
        BibEntry a_remote_author = entryAWithAuthor("remote");
        BibEntry a_same_author = entryAWithAuthor("same");
        BibEntry a_title_hello_author_alice = entryAWithAuthorAndTitle("Alice", "Hello");
        BibEntry a_title_hello_author_alice_swap = entryAWithTitleThenAuthor("Hello", "Alice");

        return Stream.of(
                Arguments.of("F01 - identical field value on all sides",
                        dbOf(a_same_author),
                        dbOf(a_same_author),
                        dbOf(a_same_author),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertNoAutoChanges(analysis);
                        }),

                Arguments.of("F02 - remote changed, local same as base",
                        dbOf(a_base_author),
                        dbOf(a_base_author),
                        dbOf(a_remote_author),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertPatchEquals(analysis, "a", StandardField.AUTHOR, "remote");
                        }),

                Arguments.of("F03 - local changed, remote same as base",
                        dbOf(a_base_author),
                        dbOf(a_local_author),
                        dbOf(a_base_author),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertNoAutoChanges(analysis);
                        }),

                Arguments.of("F04 - both changed to same value",
                        dbOf(a_base_author),
                        dbOf(a_same_author),
                        dbOf(a_same_author),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertNoAutoChanges(analysis);
                        }),

                Arguments.of("F05 - both changed same field differently",
                        dbOf(a_base_author),
                        dbOf(a_local_author),
                        dbOf(a_remote_author),
                        (Consumer<MergeAnalysis>) SemanticMergeAnalyzerTest::assertHasConflicts),

                Arguments.of("F06 - Local deleted, remote unchanged",
                        dbOf(a_base_author),
                        dbOf(entryAEmpty()),
                        dbOf(a_base_author),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertNoAutoChanges(analysis);
                        }),

                Arguments.of("F07 - Remote deleted, local unchanged",
                        dbOf(a_base_author),
                        dbOf(a_base_author),
                        dbOf(entryAEmpty()),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertPatchDeletesField(analysis, "a", StandardField.AUTHOR);
                        }),

                Arguments.of("F08 - Both deleted",
                        dbOf(a_base_author),
                        dbOf(entryAEmpty()),
                        dbOf(entryAEmpty()),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertNoAutoChanges(analysis);
                        }),

                Arguments.of("F09 - Local changed, remote deleted",
                        dbOf(a_base_author),
                        dbOf(a_local_author),
                        dbOf(entryAEmpty()),
                        (Consumer<MergeAnalysis>) SemanticMergeAnalyzerTest::assertHasConflicts),

                Arguments.of("F10 - Local deleted, remote changed",
                        dbOf(a_base_author),
                        dbOf(entryAEmpty()),
                        dbOf(a_remote_author),
                        (Consumer<MergeAnalysis>) SemanticMergeAnalyzerTest::assertHasConflicts),

                Arguments.of("F11 - All missing",
                        dbOf(entryAEmpty()),
                        dbOf(entryAEmpty()),
                        dbOf(entryAEmpty()),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertNoAutoChanges(analysis);
                        }),

                Arguments.of("F12 - Local added",
                        dbOf(entryAEmpty()),
                        dbOf(a_local_author),
                        dbOf(entryAEmpty()),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertNoAutoChanges(analysis);
                        }),

                Arguments.of("F13 - Remote added",
                        dbOf(entryAEmpty()),
                        dbOf(entryAEmpty()),
                        dbOf(a_remote_author),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertPatchEquals(analysis, "a", StandardField.AUTHOR, "remote");
                        }),

                Arguments.of("F14 - Both added same value",
                        dbOf(entryAEmpty()),
                        dbOf(a_same_author),
                        dbOf(a_same_author),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertNoAutoChanges(analysis);
                        }),

                Arguments.of("F15 - Both added different values",
                        dbOf(entryAEmpty()),
                        dbOf(a_local_author),
                        dbOf(a_remote_author),
                        (Consumer<MergeAnalysis>) SemanticMergeAnalyzerTest::assertHasConflicts),

                Arguments.of("F16 - Local modified, remote deleted (conflict)",
                        dbOf(a_base_author),
                        dbOf(a_local_author),
                        dbOf(entryAEmpty()),
                        (Consumer<MergeAnalysis>) SemanticMergeAnalyzerTest::assertHasConflicts),

                Arguments.of("F17 - Local deleted and remote modified (conflict)",
                        dbOf(a_base_author),
                        dbOf(entryAEmpty()),
                        dbOf(a_remote_author),
                        (Consumer<MergeAnalysis>) SemanticMergeAnalyzerTest::assertHasConflicts),

                Arguments.of("F18 - No base (field), both added different",
                        dbOf(entryAEmpty()),
                        dbOf(a_local_author),
                        dbOf(a_remote_author),
                        (Consumer<MergeAnalysis>) SemanticMergeAnalyzerTest::assertHasConflicts),

                Arguments.of("F19 - field order changed, same content",
                        dbOf(a_title_hello_author_alice),
                        dbOf(a_title_hello_author_alice_swap),
                        dbOf(a_title_hello_author_alice),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertNoAutoChanges(analysis);
                        }),

                Arguments.of("F20 - entryType changed in local",
                        dbOf(new BibEntry(StandardEntryType.Article, "a").withField(StandardField.AUTHOR, "base")),
                        dbOf(new BibEntry(StandardEntryType.Book, "a").withField(StandardField.AUTHOR, "base")),
                        dbOf(new BibEntry(StandardEntryType.Article, "a").withField(StandardField.AUTHOR, "base")),
                        (Consumer<MergeAnalysis>) analysis -> {
                            assertNoConflicts(analysis);
                            assertNoAutoChanges(analysis);
                        }),

                Arguments.of("F21 - entryType changed differently on both sides",
                        emptyDb(),
                        dbOf(new BibEntry(StandardEntryType.Book, "a").withField(StandardField.AUTHOR, "base")),
                        dbOf(new BibEntry(StandardEntryType.InProceedings, "a").withField(StandardField.AUTHOR, "base")),
                        (Consumer<MergeAnalysis>) SemanticMergeAnalyzerTest::assertHasConflicts)
        );
    }

    private static void assertPatchDeletesField(MergeAnalysis analysis, String citationKey, Field field) {
        Map<Field, String> patch = analysis.autoPlan().fieldPatches().get(citationKey);
        assertNotNull(patch, "Expected a field patch for entry '" + citationKey + "'");
        assertTrue(patch.containsKey(field), "Expected deletion patch for field " + field.getName());
        String patchedValue = patch.get(field);
        assertNull(patchedValue, "Expected field " + field.getName() + " to be deleted (null)");
    }

    private static void assertPlanEmpty(MergeAnalysis analysis) {
        assertTrue(analysis.autoPlan().newEntries().isEmpty(), "newEntries should be empty");
        assertTrue(analysis.autoPlan().fieldPatches().isEmpty(), "fieldPatches should be empty");
        assertTrue(analysis.autoPlan().deletedEntryKeys().isEmpty(), "deletedEntryKeys should be empty");
    }

    private static void assertHasNewEntry(MergeAnalysis analysis, BibEntry expected) {
        String expectedKey = expected.getCitationKey()
                                     .orElseThrow(() -> new AssertionError("Expected entry must have a citation key"));

        List<BibEntry> matches = analysis.autoPlan().newEntries().stream()
                                         .filter(e -> expectedKey.equals(e.getCitationKey().orElse(null)))
                                         .toList();

        assertEquals(1, matches.size(),
                "Expected exactly one new entry with key '" + expectedKey + "', but found " + matches.size());

        BibEntry actual = matches.getFirst();
        assertEntryEqualsIgnoringId(expected, actual);
    }

    private static void assertPatchEquals(MergeAnalysis analysis, String citationKey, Field field, String expectedValue) {
        Map<Field, String> patch = analysis.autoPlan().fieldPatches().get(citationKey);
        assertNotNull(patch, "Expected a field patch for entry '" + citationKey + "'");
        assertEquals(expectedValue, patch.get(field), "Field patch mismatch for " + field.getName());
    }

    private static void assertDeletes(MergeAnalysis analysis, Set<String> expectedKeys) {
        Set<String> actual = new LinkedHashSet<>(analysis.autoPlan().deletedEntryKeys());
        assertEquals(actual, expectedKeys, "Deleted keys mismatch. expected=" + expectedKeys + " actual=" + actual);
    }

    private static void assertEntryEqualsIgnoringId(BibEntry expected, BibEntry actual) {
        assertEquals(expected.getType(), actual.getType(), "Entry type mismatch");
        assertEquals(expected.getCitationKey(), actual.getCitationKey(), "Citation key mismatch");

        Map<Field, String> expectedFields = fieldMap(expected);
        Map<Field, String> actualFields = fieldMap(actual);
        assertEquals(expectedFields, actualFields, "Field map mismatch. expected=" + expectedFields + " actual=" + actualFields);
    }

    private static void assertNoConflicts(MergeAnalysis analysis) {
        assertTrue(analysis.conflicts().isEmpty(),
                "Expected no conflicts, but got: " + analysis.conflicts());
    }

    private static void assertHasConflicts(MergeAnalysis analysis) {
        assertFalse(analysis.conflicts().isEmpty(), "Expected conflicts, but none found.");
    }

    private static void assertNoAutoChanges(MergeAnalysis analysis) {
        boolean empty = analysis.autoPlan().newEntries().isEmpty()
                && analysis.autoPlan().fieldPatches().isEmpty()
                && analysis.autoPlan().deletedEntryKeys().isEmpty();
        assertTrue(empty, "Expected no auto changes, but got: "
                + "newEntries=" + analysis.autoPlan().newEntries()
                + ", fieldPatches=" + analysis.autoPlan().fieldPatches()
                + ", deleted=" + analysis.autoPlan().deletedEntryKeys());
    }

    private static Map<Field, String> fieldMap(BibEntry entry) {
        Map<Field, String> map = new LinkedHashMap<>();
        for (Field field : entry.getFields()) {
            map.put(field, entry.getField(field).orElse(""));
        }
        return map;
    }

    // ------- helpers: build entries and DBs -------

    private static BibDatabaseContext emptyDb() {
        return new BibDatabaseContext.Builder().build();
    }

    private static BibDatabaseContext dbOf(BibEntry... entries) {
        BibDatabaseContext context = new BibDatabaseContext.Builder().build();
        for (BibEntry entry : entries) {
            context.getDatabase().insertEntry(new BibEntry(entry));
        }
        return context;
    }

    private static BibEntry entryAWithAuthor(String author) {
        BibEntry entry = new BibEntry(StandardEntryType.Article, "a");
        entry.setField(StandardField.AUTHOR, author);
        return entry;
    }

    private static BibEntry entryAWithAuthorAndTitle(String author, String title) {
        BibEntry entry = new BibEntry(StandardEntryType.Article, "a")
                .withField(StandardField.AUTHOR, author)
                .withField(StandardField.TITLE, title);
        return entry;
    }

    private static BibEntry entryAWithJournal(String journal) {
        BibEntry entry = new BibEntry(StandardEntryType.Article, "a");
        entry.setField(StandardField.JOURNAL, journal);
        return entry;
    }

    private static BibEntry entryAWithAuthorTitleYear(String author, String title, String year) {
        BibEntry entry = new BibEntry(StandardEntryType.Article, "a");
        entry.setField(StandardField.AUTHOR, author);
        entry.setField(StandardField.TITLE, title);
        entry.setField(StandardField.YEAR, year);
        return entry;
    }

    private static BibEntry entryAEmpty() {
        return new BibEntry(StandardEntryType.Article, "a");
    }

    private static BibEntry entryAWithTitleThenAuthor(String title, String author) {
        BibEntry entry = new BibEntry(StandardEntryType.Article, "a");
        entry.setField(StandardField.TITLE, title);
        entry.setField(StandardField.AUTHOR, author);
        return entry;
    }
}
