package org.jabref.logic.git.merge;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.merge.MergeOutcome.Merged;
import org.jabref.logic.git.merge.MergeOutcome.Refused;
import org.jabref.logic.git.merge.Refusal.Reason;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BibFileMergerTest {

    private static final String SMITH = """
            @Article{Smith2020,
              author = {Smith, John},
              title  = {Base Title},
              year   = {2020},
            }
            """;

    private static final String DOE = """
            @Article{Doe2021,
              author = {Doe, Jane},
              title  = {Another Paper},
              year   = {2021},
            }
            """;

    private static final String NEWTON = """
            @Book{Newton1999,
              author = {Newton, Isaac},
              title  = {The Principia},
              year   = {1999},
            }
            """;

    private static final String BASE = SMITH + "\n" + DOE;

    private static final BibEntry SMITH_ENTRY = new BibEntry(StandardEntryType.Article)
            .withCitationKey("Smith2020")
            .withField(StandardField.AUTHOR, "Smith, John")
            .withField(StandardField.TITLE, "Base Title")
            .withField(StandardField.YEAR, "2020");

    private static final BibEntry DOE_ENTRY = new BibEntry(StandardEntryType.Article)
            .withCitationKey("Doe2021")
            .withField(StandardField.AUTHOR, "Doe, Jane")
            .withField(StandardField.TITLE, "Another Paper")
            .withField(StandardField.YEAR, "2021");

    private static final BibEntry NEWTON_ENTRY = new BibEntry(StandardEntryType.Book)
            .withCitationKey("Newton1999")
            .withField(StandardField.AUTHOR, "Newton, Isaac")
            .withField(StandardField.TITLE, "The Principia")
            .withField(StandardField.YEAR, "1999");

    @TempDir
    private Path tempDir;

    private final ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private BibFileMerger merger;

    @BeforeEach
    void setUp() {
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        merger = new BibFileMerger(importFormatPreferences);
    }

    @Test
    void appliesNonConflictingChangesOfOtherToCurrent() throws IOException {
        Path current = version("current", BASE.replace("title  = {Base Title},", "title  = {Current Title},\n  journal = {Journal of Tests},"));
        Path other = version("other", BASE.replace("year   = {2020},", "year   = {2020},\n  doi    = {10.1000/xyz123},") + "\n" + NEWTON);

        MergeOutcome outcome = merger.merge(version("base", BASE), current, other);

        assertEquals(new Merged(List.of()), outcome);
        BibEntry smith = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Smith2020")
                .withField(StandardField.AUTHOR, "Smith, John")
                .withField(StandardField.TITLE, "Current Title")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.JOURNAL, "Journal of Tests")
                .withField(StandardField.DOI, "10.1000/xyz123");
        assertEquals(List.of(smith, DOE_ENTRY, NEWTON_ENTRY), entriesOf(current));
    }

    @Test
    void identicalVersionsMergeCleanly() throws IOException {
        Path current = version("current", BASE);

        MergeOutcome outcome = merger.merge(version("base", BASE), current, version("other", BASE));

        assertEquals(new Merged(List.of()), outcome);
        assertEquals(List.of(SMITH_ENTRY, DOE_ENTRY), entriesOf(current));
    }

    @Test
    void conflictingEntryKeepsCurrentVersion() throws IOException {
        Path current = version("current", BASE.replace("Base Title", "Current Title"));
        Path other = version("other", BASE.replace("Base Title", "Other Title") + "\n" + NEWTON);

        MergeOutcome outcome = merger.merge(version("base", BASE), current, other);

        assertEquals(List.of("Smith2020"), conflictingKeys(outcome));
        BibEntry smith = new BibEntry(SMITH_ENTRY).withField(StandardField.TITLE, "Current Title");
        assertEquals(List.of(smith, DOE_ENTRY, NEWTON_ENTRY), entriesOf(current));
    }

    @Test
    void appliesEntryTypeChangedInOther() throws IOException {
        Path current = version("current", BASE);

        MergeOutcome outcome = merger.merge(version("base", BASE), current, version("other", BASE.replace("@Article{Smith2020", "@Book{Smith2020")));

        assertEquals(new Merged(List.of()), outcome);
        assertEquals(List.of(smithAs(StandardEntryType.Book), DOE_ENTRY), entriesOf(current));
    }

    @Test
    void entryTypeChangedDifferentlyOnBothSidesIsConflict() throws IOException {
        Path current = version("current", BASE.replace("@Article{Smith2020", "@Report{Smith2020"));
        Path other = version("other", BASE.replace("@Article{Smith2020", "@Book{Smith2020") + "\n" + NEWTON);

        MergeOutcome outcome = merger.merge(version("base", BASE), current, other);

        assertEquals(List.of("Smith2020"), conflictingKeys(outcome));
        assertEquals(List.of(smithAs(StandardEntryType.Report), DOE_ENTRY, NEWTON_ENTRY), entriesOf(current));
    }

    @Test
    void entryTypeChangedInCurrentAndDeletedInOtherIsConflict() throws IOException {
        Path current = version("current", BASE.replace("@Article{Smith2020", "@Report{Smith2020"));

        MergeOutcome outcome = merger.merge(version("base", BASE), current, version("other", DOE));

        assertEquals(List.of("Smith2020"), conflictingKeys(outcome));
        assertEquals(List.of(smithAs(StandardEntryType.Report), DOE_ENTRY), entriesOf(current));
    }

    @Test
    void appliesEntryCommentChangedInOther() throws IOException {
        Path current = version("current", "% Note about Smith\n" + BASE);

        MergeOutcome outcome = merger.merge(version("base", "% Note about Smith\n" + BASE), current, version("other", "% Changed note about Smith\n" + BASE));

        assertEquals(new Merged(List.of()), outcome);
        assertEquals("% Changed note about Smith\n", entriesOf(current).getFirst().getUserComments());
    }

    @Test
    void entryCommentChangedDifferentlyOnBothSidesIsConflict() throws IOException {
        Path current = version("current", "% Own note about Smith\n" + BASE);

        MergeOutcome outcome = merger.merge(version("base", "% Note about Smith\n" + BASE), current, version("other", "% Changed note about Smith\n" + BASE));

        assertEquals(List.of("Smith2020"), conflictingKeys(outcome));
        assertEquals("% Own note about Smith\n", entriesOf(current).getFirst().getUserComments());
    }

    @Test
    void entryCommentChangedInOtherAndDeletedInCurrentIsConflict() throws IOException {
        Path current = version("current", DOE);

        MergeOutcome outcome = merger.merge(version("base", "% Note about Smith\n" + BASE), current, version("other", "% Changed note about Smith\n" + BASE));

        assertEquals(List.of("Smith2020"), conflictingKeys(outcome));
        assertEquals(List.of(DOE_ENTRY), entriesOf(current));
    }

    @Test
    void entryCommentChangedInCurrentAndDeletedInOtherIsConflict() throws IOException {
        Path current = version("current", "% Own note about Smith\n" + BASE);

        MergeOutcome outcome = merger.merge(version("base", "% Note about Smith\n" + BASE), current, version("other", DOE));

        assertEquals(List.of("Smith2020"), conflictingKeys(outcome));
        assertEquals(List.of(new BibEntry(SMITH_ENTRY).withUserComments("% Own note about Smith\n"), DOE_ENTRY), entriesOf(current));
    }

    @Test
    void keepsCustomEntryTypeDefinition() throws IOException {
        String customType = "@Comment{jabref-entrytype: mytype: req[author;title] opt[year]}\n\n" + """
                @Mytype{Smith2020,
                  author = {Smith, John},
                  title  = {Base Title},
                }
                """;
        Path current = version("current", customType);

        MergeOutcome outcome = merger.merge(version("base", customType), current, version("other", customType.replace("title  = {Base Title},", "title  = {Base Title},\n  year   = {2020},")));

        assertEquals(new Merged(List.of()), outcome);
        String merged = Files.readString(current);
        assertTrue(merged.contains("@Comment{jabref-entrytype: mytype: req[author;title] opt[year]}"), merged);
        assertTrue(merged.contains("year   = {2020},"), merged);
    }

    @Test
    void refusesStringAddedInOther() throws IOException {
        Path current = version("current", BASE);
        Path other = version("other", "@String{jot = {Journal of Tests}}\n\n" + BASE);

        MergeOutcome outcome = merger.merge(version("base", BASE), current, other);

        assertEquals(new Refused(List.of(new Refusal(other, Reason.NON_ENTRY_CONTENT_CHANGED_IN_OTHER))), outcome);
        assertEquals(BASE, Files.readString(current));
    }

    @Test
    void refusesSharedDatabaseIdAddedInOther() throws IOException {
        Path current = version("current", BASE);
        Path other = version("other", "% DBID: 6a7b8c\n\n" + BASE);

        MergeOutcome outcome = merger.merge(version("base", BASE), current, other);

        assertEquals(new Refused(List.of(new Refusal(other, Reason.NON_ENTRY_CONTENT_CHANGED_IN_OTHER))), outcome);
        assertEquals(BASE, Files.readString(current));
    }

    @Test
    void refusesDuplicateCitationKeys() throws IOException {
        String duplicateKeys = SMITH + "\n" + SMITH.replace("Base Title", "Duplicate Key");
        Path current = version("current", duplicateKeys);

        MergeOutcome outcome = merger.merge(version("base", BASE), current, version("other", BASE));

        assertEquals(new Refused(List.of(new Refusal(current, Reason.DUPLICATE_CITATION_KEYS))), outcome);
        assertEquals(duplicateKeys, Files.readString(current));
    }

    @Test
    void refusesFileWithParserWarnings() throws IOException {
        String duplicateStrings = "@String{jot = {Journal of Tests}}\n@String{jot = {Other Journal}}\n\n" + BASE;
        Path current = version("current", duplicateStrings);

        MergeOutcome outcome = merger.merge(version("base", duplicateStrings), current, version("other", duplicateStrings));

        assertEquals(List.of(Reason.PARSER_WARNINGS), reasons(outcome));
        assertEquals(duplicateStrings, Files.readString(current));
    }

    @Test
    void refusesEmptyEntry() throws IOException {
        String withEmptyEntry = BASE + "\n@Misc{,\n}\n";
        Path current = version("current", withEmptyEntry);

        MergeOutcome outcome = merger.merge(version("base", withEmptyEntry), current, version("other", withEmptyEntry));

        assertEquals(List.of(Reason.EMPTY_ENTRY), reasons(outcome));
        assertEquals(withEmptyEntry, Files.readString(current));
    }

    @Test
    void refusesUnusedCustomEntryType() throws IOException {
        String unusedType = "@Comment{jabref-entrytype: unused: req[author] opt[year]}\n\n" + BASE;
        Path current = version("current", unusedType);

        MergeOutcome outcome = merger.merge(version("base", unusedType), current, version("other", unusedType));

        assertEquals(List.of(Reason.UNUSED_CUSTOM_ENTRY_TYPE), reasons(outcome));
        assertEquals(unusedType, Files.readString(current));
    }

    static Stream<Arguments> unattachedComments() {
        return Stream.of(
                Arguments.of("in front of a custom entry type",
                        "% Note about the custom type\n@Comment{jabref-entrytype: mytype: req[author;title] opt[year]}\n\n" + """
                                @Mytype{Smith2020,
                                  author = {Smith, John},
                                  title  = {Base Title},
                                }
                                """),
                Arguments.of("in front of a lowercase @comment",
                        "% note\n@comment{jabref-entrytype: mytype: req[author] opt[year]}\n\n" + """
                                @Mytype{Smith2020,
                                  author = {Smith, John},
                                }
                                """),
                Arguments.of("in front of the preamble",
                        "% Note about the preamble\n@Preamble{\"\\newcommand{\\noopsort}[1]{}\"}\n\n" + BASE));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unattachedComments")
    void refusesUnattachedComment(String description, String content) throws IOException {
        Path current = version("current", content);

        MergeOutcome outcome = merger.merge(version("base", content), current, version("other", content));

        assertEquals(List.of(Reason.UNATTACHED_COMMENT), reasons(outcome));
        assertEquals(content, Files.readString(current));
    }

    @Test
    void refusesUnattachedCommentInNonUtf8File() throws IOException {
        String content = "% Encoding: ISO-8859-1\n\n% Notiz über Müller\n@Comment{jabref-entrytype: mytype: req[author] opt[year]}\n\n" + """
                @Mytype{Mueller2020,
                  author = {Müller, Max},
                }
                """;
        Path current = version("current", content, StandardCharsets.ISO_8859_1);

        MergeOutcome outcome = merger.merge(version("base", content, StandardCharsets.ISO_8859_1), current, version("other", content, StandardCharsets.ISO_8859_1));

        assertEquals(List.of(Reason.UNATTACHED_COMMENT), reasons(outcome));
        assertEquals(content, Files.readString(current, StandardCharsets.ISO_8859_1));
    }

    /// Git hands the driver temporary files without a `.bib` extension
    private Path version(String name, String content) throws IOException {
        return version(name, content, StandardCharsets.UTF_8);
    }

    private Path version(String name, String content, Charset encoding) throws IOException {
        return Files.writeString(tempDir.resolve(".merge_file_" + name), content, encoding);
    }

    private List<BibEntry> entriesOf(Path file) throws IOException {
        return new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor()).importDatabase(file).getDatabase().getEntries();
    }

    private static BibEntry smithAs(StandardEntryType type) {
        BibEntry smith = new BibEntry(SMITH_ENTRY);
        smith.setType(type);
        return smith;
    }

    private static List<String> conflictingKeys(MergeOutcome outcome) {
        return assertInstanceOf(Merged.class, outcome).conflicts().stream().map(ThreeWayEntryConflict::citationKey).toList();
    }

    /// The refusals of the CURRENT version; identical versions are refused three times for the same reason
    private static List<Reason> reasons(MergeOutcome outcome) {
        return assertInstanceOf(Refused.class, outcome).refusals().stream().map(Refusal::reason).distinct().toList();
    }
}
