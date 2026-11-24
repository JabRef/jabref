package org.jabref.logic.lint.rules;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.EntryBracket;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.lint.rule.Finding;
import org.jabref.logic.lint.rule.Rule;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.LintSettings;
import org.jabref.model.metadata.RuleMode;
import org.jabref.model.metadata.RuleSelector;
import org.jabref.model.metadata.RuleSettings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ResourceLock("Localization.lang")
class EntryBracketsRuleTest {

    private static final String IN_PARENTHESES = """
            % (reviewed)
            @Article( key,
              title = {A (small) title},
            )
            """;

    private static final String IN_BRACES = """
            % {reviewed}
            @Article{ key,
              title = {A {small} title},
            }
            """;

    private final EntryBracketsRule rule = new EntryBracketsRule();
    private final EntryBracketsRule inParentheses = new EntryBracketsRule(EntryBracket.PARENTHESES);

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

    @Test
    void anEntryInBracesIsReportedWhereParenthesesAreAskedFor() throws IOException {
        List<Finding> findings = inParentheses.scan(parse(IN_BRACES));

        assertEquals(List.of(Optional.empty()), findings.stream().map(Finding::field).toList());
        assertEquals("entry is enclosed in braces instead of parentheses", findings.getFirst().message());
    }

    @Test
    void anEntryInParenthesesIsLeftAloneWhereTheyAreAskedFor() throws IOException {
        assertEquals(List.of(), inParentheses.scan(parse(IN_PARENTHESES)));
    }

    /// The comment above the entry and the braces inside it are part of what the user wrote.
    @Test
    void theRepairToParenthesesReplacesTheDelimitersAndNothingElse() throws IOException {
        BibEntry entry = parse(IN_BRACES);

        repair(inParentheses, entry);

        assertEquals("""
                % {reviewed}
                @Article( key,
                  title = {A {small} title},
                )
                """, entry.getParsedSerialization());
    }

    @Test
    void anEntryRepairedToParenthesesIsNotReportedAgain() throws IOException {
        BibEntry entry = parse(IN_BRACES);

        repair(inParentheses, entry);

        assertEquals(List.of(), inParentheses.scan(entry));
    }

    @Test
    void aMismatchedPairIsEnclosedInParentheses() throws IOException {
        BibEntry entry = parse("""
                @Article(key,
                  title = {A title},
                }
                """);

        repair(inParentheses, entry);

        assertEquals("""
                @Article(key,
                  title = {A title},
                )
                """, entry.getParsedSerialization());
    }

    @Test
    void anEntryEditedBeforeARepairToParenthesesStaysEdited() throws IOException {
        BibEntry entry = parse(IN_BRACES);
        entry.setField(StandardField.TITLE, "Another title");

        repair(inParentheses, entry);

        assertTrue(entry.hasChanged());
    }

    @Test
    void theValueNamesTheBracket() throws IOException {
        Rule configured = rule.configuredWith(Map.of(EntryBracketsRule.BRACKET, "parentheses")).orElseThrow();

        assertEquals(List.of(), configured.scan(parse(IN_PARENTHESES)));
    }

    @Test
    void theValueCanNameBracesOutLoud() throws IOException {
        Rule configured = inParentheses.configuredWith(Map.of(EntryBracketsRule.BRACKET, "braces")).orElseThrow();

        assertEquals(List.of(), configured.scan(parse(IN_BRACES)));
    }

    @Test
    void withoutAValueTheRuleGoesByBraces() throws IOException {
        Rule configured = rule.configuredWith(Map.of()).orElseThrow();

        assertEquals(List.of(), configured.scan(parse(IN_BRACES)));
    }

    /// It may be a typo, or come from a newer JabFix; going by braces would rewrite a library that
    /// asked for something else.
    @ParameterizedTest
    @MethodSource
    void aValueTheRuleCannotReadPassesItOver(Map<String, String> values) {
        assertEquals(Optional.empty(), rule.configuredWith(values));
    }

    static Stream<Map<String, String>> aValueTheRuleCannotReadPassesItOver() {
        return Stream.of(
                Map.of(EntryBracketsRule.BRACKET, "angle-brackets"),
                Map.of(EntryBracketsRule.BRACKET, "PARENTHESES"),
                Map.of("brackets", "parentheses"),
                Map.of(EntryBracketsRule.BRACKET, "parentheses", "other", "value"));
    }

    /// The bracket reaches a save whichever way the rule goes: an entry written anew has to be
    /// written in something, and only the one asked for is not reported next time.
    @ParameterizedTest
    @EnumSource(value = RuleMode.class, names = {"CHECK", "FIX"})
    void aSaveEnclosesAnEntryInTheBracketOfARuleThatRuns(RuleMode mode) {
        assertEquals(EntryBracket.PARENTHESES, EntryBracketsRule.bracketOf(configuredForParentheses(mode)));
    }

    @Test
    void aSaveEnclosesAnEntryInBracesWhereTheRuleIsSwitchedOff() {
        assertEquals(EntryBracket.BRACES, EntryBracketsRule.bracketOf(configuredForParentheses(RuleMode.OFF)));
    }

    @Test
    void aSaveEnclosesAnEntryInBracesWhereTheRuleIsNotNamed() {
        assertEquals(EntryBracket.BRACES, EntryBracketsRule.bracketOf(LintSettings.NONE));
    }

    @Test
    void aSaveEnclosesAnEntryInBracesWhereTheRuleCannotReadItsValue() {
        LintSettings settings = LintSettings.NONE.and(RuleSelector.of(EntryBracketsRule.ID),
                new RuleSettings(RuleMode.FIX, new LinkedHashMap<>(Map.of(EntryBracketsRule.BRACKET, "angle-brackets"))));

        assertEquals(EntryBracket.BRACES, EntryBracketsRule.bracketOf(settings));
    }

    private static LintSettings configuredForParentheses(RuleMode mode) {
        return LintSettings.NONE.and(RuleSelector.of(EntryBracketsRule.ID),
                new RuleSettings(mode, new LinkedHashMap<>(Map.of(EntryBracketsRule.BRACKET, "parentheses"))));
    }

    private void repair(BibEntry entry) {
        repair(rule, entry);
    }

    private static void repair(EntryBracketsRule rule, BibEntry entry) {
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
