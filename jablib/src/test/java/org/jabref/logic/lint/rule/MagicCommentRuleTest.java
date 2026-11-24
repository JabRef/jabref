package org.jabref.logic.lint.rule;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MagicCommentRuleTest {

    private final MagicCommentRule rule = new MagicCommentRule(Set.of("surrounding-whitespace"));

    @Test
    void aKnownIdIsNotReported() {
        assertEquals(List.of(), rule.scan(entryWithComment("% jabref-format-ignore surrounding-whitespace")));
    }

    @Test
    void anIdOfNoRuleOfTheRunIsReported() {
        List<Finding> findings = rule.scan(entryWithComment("% jabref-format-ignore surounding-whitespace"));

        assertEquals(1, findings.size());
        assertEquals(Optional.empty(), findings.getFirst().field());
        assertEquals("the magic comment switches off \"surounding-whitespace\", which is no rule of this run",
                findings.getFirst().message());
    }

    /// The person who wrote the comment is the only one who knows what was meant.
    @Test
    void theFindingCarriesNoFix() {
        assertFalse(rule.scan(entryWithComment("% jabref-format-ignore surounding-whitespace")).getFirst().isFixable());
    }

    @Test
    void aFieldScopedIdIsReportedOnThatField() {
        List<Finding> findings = rule.scan(entryWithComment("% jabref-format-ignore author:surounding-whitespace"));

        assertEquals(1, findings.size());
        assertEquals(Optional.of(StandardField.AUTHOR), findings.getFirst().field());
    }

    /// A token the parser could not take apart stays one rule id, and so names no rule either.
    @Test
    void aTokenWithSeveralColonsIsReportedAsAWhole() {
        List<Finding> findings = rule.scan(entryWithComment("% jabref-format-ignore author:title:surrounding-whitespace"));

        assertEquals(1, findings.size());
        assertEquals("the magic comment switches off \"author:title:surrounding-whitespace\", which is no rule of this run",
                findings.getFirst().message());
    }

    @Test
    void anIdOfNoRuleIsReportedOnEveryFieldTheTokenNames() {
        List<Finding> findings = rule.scan(entryWithComment("% jabref-format-ignore author,title:surounding-whitespace"));

        assertEquals(List.of(Optional.of(StandardField.AUTHOR), Optional.of(StandardField.TITLE)),
                findings.stream().map(Finding::field).toList());
    }

    @Test
    void aRuleRegexThatMatchesNoRuleOfTheRunIsReported() {
        List<Finding> findings = rule.scan(entryWithComment("% jabref-format-ignore /surounding-.*/"));

        assertEquals(List.of("the magic comment switches off /surounding-.*/, which matches no rule of this run"),
                findings.stream().map(Finding::message).toList());
    }

    @Test
    void aRuleRegexThatMatchesARuleIsLeftAlone() {
        assertEquals(List.of(), rule.scan(entryWithComment("% jabref-format-ignore /surrounding-.*/")));
    }

    @Test
    void aFieldRegexThatMatchesNoFieldIsReported() {
        List<Finding> findings = rule.scan(entryWithComment("% jabref-format-ignore /autor.*/:surrounding-whitespace"));

        assertEquals(List.of("the magic comment names the fields /autor.*/, which match neither a BibTeX field nor one this entry has"),
                findings.stream().map(Finding::message).toList());
    }

    /// Like a field named outright, it may be meant for what is added to the entry later.
    @Test
    void aFieldRegexThatMatchesABibTeXFieldIsLeftAlone() {
        assertEquals(List.of(), rule.scan(entryWithComment("% jabref-format-ignore /(book)?title/:surrounding-whitespace")));
    }

    @Test
    void aFieldRegexThatMatchesAFieldOfTheEntryIsLeftAlone() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(new UnknownField("comment-alice"), "a note")
                .withUserComments("% jabref-format-ignore /comment-.*/:surrounding-whitespace");

        assertEquals(List.of(), rule.scan(entry));
    }

    /// A name JabRef does not know, on an entry that does not carry it, switches nothing off.
    @Test
    void aFieldNeitherKnownNorPresentIsReported() {
        List<Finding> findings = rule.scan(entryWithComment("% jabref-format-ignore autor:surrounding-whitespace"));

        assertEquals(1, findings.size());
        assertEquals("the magic comment names the field \"autor\", which is neither a BibTeX field nor one this entry has",
                findings.getFirst().message());
    }

    /// It may well be meant for what is added to the entry later.
    @Test
    void aBibTeXFieldTheEntryDoesNotHaveIsLeftAlone() {
        assertEquals(List.of(), rule.scan(entryWithComment("% jabref-format-ignore title:surrounding-whitespace")));
    }

    @Test
    void aFieldTheEntryCarriesIsLeftAlone() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(new UnknownField("mynote"), "a note")
                .withUserComments("% jabref-format-ignore mynote:surrounding-whitespace");

        assertEquals(List.of(), rule.scan(entry));
    }

    @Test
    void aFieldOfTheEntryWhoseNameContainsAColonIsLeftAlone() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(new UnknownField("note:de"), "eine Notiz")
                .withUserComments("% jabref-format-ignore note:de:surrounding-whitespace");

        assertEquals(List.of(), rule.scan(entry));
    }

    /// So that a comment meant as it is written can be left alone.
    @Test
    void itsOwnIdCountsAsKnown() {
        assertEquals(List.of(), rule.scan(entryWithComment("% jabref-format-ignore magic-comment")));
    }

    private static BibEntry entryWithComment(String comment) {
        return new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Doe, Jane")
                .withUserComments(comment);
    }
}
