package org.jabref.logic.lint.rules;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.lint.rule.Finding;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class EntryParenthesesRuleTest {

    private static final String IN_PARENTHESES = """
            % (reviewed)
            @Article( key,
              title = {A (small) title},
            )
            """;

    private final EntryParenthesesRule rule = new EntryParenthesesRule();

    @Test
    void anEntryInParenthesesIsReportedAsAWhole() throws IOException {
        List<Finding> findings = rule.scan(parse(IN_PARENTHESES));

        assertEquals(List.of(Optional.empty()), findings.stream().map(Finding::field).toList());
        assertEquals("entry is enclosed in parentheses instead of braces", findings.getFirst().message());
    }

    @Test
    void anEntryInBracesIsLeftAlone() throws IOException {
        assertEquals(List.of(), rule.scan(parse("""
                @Article{key,
                  title = {A (small) title},
                }
                """)));
    }

    @Test
    void anEntryNotReadFromAFileIsLeftAlone() {
        assertEquals(List.of(), rule.scan(new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, "A title")));
    }

    /// The comment above the entry and the parentheses inside it are part of what the user wrote.
    @Test
    void theRepairReplacesTheDelimitersAndNothingElse() throws IOException {
        BibEntry entry = parse(IN_PARENTHESES);

        repair(entry);

        assertEquals("""
                % (reviewed)
                @Article{ key,
                  title = {A (small) title},
                }
                """, entry.getParsedSerialization());
    }

    @Test
    void theRepairChangesNoField() throws IOException {
        BibEntry entry = parse(IN_PARENTHESES);

        assertEquals(Optional.empty(), rule.scan(entry).getFirst().fix().orElseThrow().applyTo(entry));
    }

    @Test
    void aRepairedEntryIsNotReportedAgain() throws IOException {
        BibEntry entry = parse(IN_PARENTHESES);

        repair(entry);

        assertEquals(List.of(), rule.scan(entry));
    }

    /// The parser takes either delimiter at either end.
    @Test
    void aMismatchedPairIsEnclosedInBraces() throws IOException {
        BibEntry entry = parse("""
                @Article(key,
                  title = {A title},
                }
                """);

        repair(entry);

        assertEquals("""
                @Article{key,
                  title = {A title},
                }
                """, entry.getParsedSerialization());
    }

    /// An entry that is written from what it holds rather than as it was read has to stay that way,
    /// or its edit would be lost.
    @Test
    void anEditedEntryStaysEdited() throws IOException {
        BibEntry entry = parse(IN_PARENTHESES);
        entry.setField(StandardField.TITLE, "Another title");

        repair(entry);

        assertTrue(entry.hasChanged());
    }

    private void repair(BibEntry entry) {
        rule.scan(entry).forEach(finding -> finding.fix().orElseThrow().applyTo(entry));
    }

    private static BibEntry parse(String bibtex) throws IOException {
        return new BibtexParser(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS))
                .parse(Reader.of(bibtex))
                .getDatabase()
                .getEntries()
                .getFirst();
    }
}
