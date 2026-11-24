package org.jabref.logic.lint.rule;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuppressionsTest {

    @Test
    void anEntryWithoutCommentsSuppressesNothing() {
        assertEquals(Suppressions.NONE, Suppressions.in(new BibEntry(StandardEntryType.Article)));
    }

    @Test
    void readsTheCommentsOfAnEntry() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withUserComments("% jabref-format-ignore surrounding-whitespace");

        assertEquals(suppressing(forEntry("surrounding-whitespace")), Suppressions.in(entry));
    }

    @Test
    void anOrdinaryCommentSuppressesNothing() {
        assertEquals(Suppressions.NONE, Suppressions.parse("% Knuth wrote this one by hand, leave it be"));
    }

    @Test
    void aRuleWithoutAFieldCountsForTheWholeEntry() {
        assertEquals(suppressing(forEntry("page-ranges")),
                Suppressions.parse("% jabref-format-ignore page-ranges"));
    }

    @Test
    void aRuleBehindAFieldCountsForThatFieldOnly() {
        assertEquals(suppressing(forFields(List.of(StandardField.AUTHOR), "page-ranges")),
                Suppressions.parse("% jabref-format-ignore author:page-ranges"));
    }

    @Test
    void oneCommentTakesSeveralTokens() {
        assertEquals(suppressing(
                        forEntry("surrounding-whitespace"),
                        forFields(List.of(StandardField.AUTHOR), "page-ranges"),
                        forFields(List.of(StandardField.TITLE), "page-ranges")),
                Suppressions.parse("% jabref-format-ignore surrounding-whitespace author:page-ranges title:page-ranges"));
    }

    @Test
    void severalCommentsAboveOneEntryAddUp() {
        assertEquals(suppressing(
                        forFields(List.of(StandardField.AUTHOR), "page-ranges"),
                        forEntry("surrounding-whitespace"),
                        forFields(List.of(StandardField.AUTHOR), "author-et-al")),
                Suppressions.parse("""
                        % jabref-format-ignore author:page-ranges
                        % a note in between
                        % jabref-format-ignore surrounding-whitespace author:author-et-al
                        """));
    }

    @Test
    void bothSidesTakeAList() {
        assertEquals(suppressing(forFields(List.of(StandardField.AUTHOR, StandardField.TITLE), "page-ranges", "surrounding-whitespace")),
                Suppressions.parse("% jabref-format-ignore author,title:page-ranges,surrounding-whitespace"));
    }

    @Test
    void aListOfRulesWithoutFieldsCountsForTheWholeEntry() {
        assertEquals(suppressing(forEntry("page-ranges", "surrounding-whitespace")),
                Suppressions.parse("% jabref-format-ignore page-ranges,surrounding-whitespace"));
    }

    @Test
    void everyFieldOfAListIsCoveredByEveryRuleOfTheOther() {
        Suppressions suppressions = Suppressions.parse("% jabref-format-ignore author,title:page-ranges,surrounding-whitespace");

        assertTrue(suppressions.suppresses("page-ranges", Optional.of(StandardField.TITLE)));
        assertTrue(suppressions.suppresses("surrounding-whitespace", Optional.of(StandardField.AUTHOR)));
        assertFalse(suppressions.suppresses("page-ranges", Optional.of(StandardField.YEAR)));
    }

    /// The `.bib` format does not care about the case of a field name, so neither does this.
    @Test
    void theCaseOfAFieldNameDoesNotMatter() {
        assertEquals(suppressing(forFields(List.of(StandardField.AUTHOR), "page-ranges")),
                Suppressions.parse("% jabref-format-ignore Author:page-ranges"));
    }

    @Test
    void aFieldJabRefDoesNotKnowIsKeptAsWritten() {
        assertEquals(suppressing(forFields(List.of(new UnknownField("mynote")), "page-ranges")),
                Suppressions.parse("% jabref-format-ignore mynote:page-ranges"));
    }

    @Test
    void aFieldOfTheEntryWhoseNameContainsAColonIsReadAsAWhole() {
        assertEquals(suppressing(forFields(List.of(StandardField.AUTHOR, new UnknownField("note:de")), "page-ranges")),
                Suppressions.parse("% jabref-format-ignore author,Note:DE:page-ranges", Set.of(new UnknownField("note:de"))));
    }

    @Test
    void theLongestFieldNameOfTheEntryIsReadAsAWhole() {
        assertEquals(suppressing(forFields(List.of(new UnknownField("a:b:c")), "page-ranges")),
                Suppressions.parse("% jabref-format-ignore a:b:c:page-ranges", Set.of(new UnknownField("a:b"), new UnknownField("a:b:c"))));
    }

    @Test
    void readsTheFieldNamesOfTheEntryItStandsAbove() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(new UnknownField("note:de"), "eine Notiz")
                .withUserComments("% jabref-format-ignore note:de:surrounding-whitespace");

        assertEquals(suppressing(forFields(List.of(new UnknownField("note:de")), "surrounding-whitespace")), Suppressions.in(entry));
    }

    @Test
    void anItemBetweenSlashesIsARegex() {
        assertEquals(suppressing(new Suppressions.Token(List.of(StandardField.NOTE), List.of("comment-.*"), List.of("page-ranges"), List.of("normalize-.*"))),
                Suppressions.parse("% jabref-format-ignore note,/comment-.*/:page-ranges,/normalize-.*/"));
    }

    /// Only the slashes delimit a regex, so the separators may be part of one.
    @Test
    void aRegexMayContainCommasAndColons() {
        assertEquals(suppressing(new Suppressions.Token(List.of(), List.of("a{1,3}|b:c"), List.of("page-ranges"), List.of())),
                Suppressions.parse("% jabref-format-ignore /a{1,3}|b:c/:page-ranges"));
    }

    @Test
    void anEscapedSlashDoesNotEndTheRegex() {
        assertEquals(suppressing(new Suppressions.Token(List.of(), List.of("a\\/b"), List.of("page-ranges"), List.of())),
                Suppressions.parse("% jabref-format-ignore /a\\/b/:page-ranges"));
    }

    @Test
    void aFieldRegexMatchesTheWholeNameIgnoringCase() {
        Suppressions suppressions = Suppressions.parse("% jabref-format-ignore /COMMENT-.*/:page-ranges");

        assertTrue(suppressions.suppresses("page-ranges", Optional.of(new UnknownField("comment-alice"))));
        assertFalse(suppressions.suppresses("page-ranges", Optional.of(new UnknownField("mycomment-alice"))));
    }

    @Test
    void aRuleRegexMatchesTheWholeId() {
        Suppressions suppressions = Suppressions.parse("% jabref-format-ignore /surrounding-.*/ /page/");

        assertTrue(suppressions.suppresses("surrounding-whitespace", Optional.of(StandardField.AUTHOR)));
        assertFalse(suppressions.suppresses("page-ranges", Optional.of(StandardField.AUTHOR)));
    }

    /// A field name JabRef does not know is no error in BibTeX, so it is kept -- and then matches no
    /// finding, which is why a misspelled one switches nothing off.
    @Test
    void aMisspelledFieldNameSwitchesNothingOff() {
        Suppressions suppressions = Suppressions.parse("% jabref-format-ignore autor:page-ranges");

        assertFalse(suppressions.suppresses("page-ranges", Optional.of(StandardField.AUTHOR)));
    }

    /// Rather than dropping it, so that it is reported once ids are checked against the rules.
    @Test
    void aTokenWithAnEmptyHalfStaysOneRuleId() {
        assertEquals(suppressing(forEntry("author:"), forEntry(":page-ranges")),
                Suppressions.parse("% jabref-format-ignore author: :page-ranges"));
    }

    @Test
    void aTokenWithAnEmptyItemStaysOneRuleId() {
        assertEquals(suppressing(forEntry("author,,title:page-ranges")),
                Suppressions.parse("% jabref-format-ignore author,,title:page-ranges"));
    }

    /// Unless the entry has a field of that name, see [#aFieldOfTheEntryWhoseNameContainsAColonIsReadAsAWhole].
    @Test
    void aTokenWithASecondColonStaysOneRuleId() {
        assertEquals(suppressing(forEntry("author:title:page-ranges")),
                Suppressions.parse("% jabref-format-ignore author:title:page-ranges"));
    }

    @Test
    void aRegexThatIsNotClosedStaysOneRuleId() {
        assertEquals(suppressing(forEntry("/comment-.*:page-ranges")),
                Suppressions.parse("% jabref-format-ignore /comment-.*:page-ranges"));
    }

    @Test
    void aRegexFollowedByMoreTextStaysOneRuleId() {
        assertEquals(suppressing(forEntry("/comment/s:page-ranges")),
                Suppressions.parse("% jabref-format-ignore /comment/s:page-ranges"));
    }

    @Test
    void aRegexThatDoesNotCompileStaysOneRuleId() {
        assertEquals(suppressing(forEntry("/comment-[/:page-ranges")),
                Suppressions.parse("% jabref-format-ignore /comment-[/:page-ranges"));
    }

    @Test
    void aCommentWithoutTokensSwitchesEveryRuleOff() {
        Suppressions suppressions = Suppressions.parse("% jabref-format-ignore");

        assertEquals(new Suppressions(true, List.of()), suppressions);
        assertTrue(suppressions.suppresses("page-ranges", Optional.of(StandardField.TITLE)));
    }

    @Test
    void aLongerWordIsNotTheMagicComment() {
        assertEquals(Suppressions.NONE, Suppressions.parse("% jabref-format-ignored page-ranges"));
    }

    @Test
    void aFieldScopedRuleSuppressesThatFieldOnly() {
        Suppressions suppressions = Suppressions.parse("% jabref-format-ignore author:page-ranges");

        assertTrue(suppressions.suppresses("page-ranges", Optional.of(StandardField.AUTHOR)));
        assertFalse(suppressions.suppresses("page-ranges", Optional.of(StandardField.TITLE)));
        assertFalse(suppressions.suppresses("page-ranges", Optional.empty()));
    }

    @Test
    void aRuleOfTheWholeEntrySuppressesEveryField() {
        Suppressions suppressions = Suppressions.parse("% jabref-format-ignore page-ranges");

        assertTrue(suppressions.suppresses("page-ranges", Optional.of(StandardField.AUTHOR)));
        assertTrue(suppressions.suppresses("page-ranges", Optional.empty()));
        assertFalse(suppressions.suppresses("surrounding-whitespace", Optional.of(StandardField.AUTHOR)));
    }

    private static Suppressions suppressing(Suppressions.Token... tokens) {
        return new Suppressions(false, List.of(tokens));
    }

    private static Suppressions.Token forEntry(String... rules) {
        return new Suppressions.Token(List.of(), List.of(), List.of(rules), List.of());
    }

    private static Suppressions.Token forFields(List<Field> fields, String... rules) {
        return new Suppressions.Token(fields, List.of(), List.of(rules), List.of());
    }
}
