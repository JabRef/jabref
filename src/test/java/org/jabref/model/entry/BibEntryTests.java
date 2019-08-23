package org.jabref.model.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BibEntryTests {

    private BibEntry keywordEntry;
    private BibEntry emptyEntry;

    @BeforeEach
    public void setUp() {
        // Default entry for most keyword and some type tests
        keywordEntry = new BibEntry();
        keywordEntry.setType(StandardEntryType.Article);
        keywordEntry.setField(StandardField.KEYWORDS, "Foo, Bar");
        keywordEntry.setChanged(false);

        // Empty entry for some tests
        emptyEntry = new BibEntry();
        emptyEntry.setType(StandardEntryType.Article);
        emptyEntry.setChanged(false);
    }

    @Test
    public void testDefaultConstructor() {
        BibEntry entry = new BibEntry();
        assertEquals(StandardEntryType.Misc, entry.getType());
        assertNotNull(entry.getId());
        assertFalse(entry.getField(StandardField.AUTHOR).isPresent());
    }

    @Test
    public void allFieldsPresentDefault() {
        BibEntry e = new BibEntry(StandardEntryType.Article);
        e.setField(StandardField.AUTHOR, "abc");
        e.setField(StandardField.TITLE, "abc");
        e.setField(StandardField.JOURNAL, "abc");

        List<OrFields> requiredFields = new ArrayList<>();
        requiredFields.add(new OrFields(StandardField.AUTHOR));
        requiredFields.add(new OrFields(StandardField.TITLE));
        assertTrue(e.allFieldsPresent(requiredFields, null));

        requiredFields.add(new OrFields(StandardField.YEAR));
        assertFalse(e.allFieldsPresent(requiredFields, null));
    }

    @Test
    public void allFieldsPresentOr() {
        BibEntry e = new BibEntry(StandardEntryType.Article);
        e.setField(StandardField.AUTHOR, "abc");
        e.setField(StandardField.TITLE, "abc");
        e.setField(StandardField.JOURNAL, "abc");

        List<OrFields> requiredFields = new ArrayList<>();
        requiredFields.add(new OrFields(StandardField.JOURNAL, StandardField.YEAR));
        assertTrue(e.allFieldsPresent(requiredFields, null));

        requiredFields.add(new OrFields(StandardField.YEAR, StandardField.ADDRESS));
        assertFalse(e.allFieldsPresent(requiredFields, null));
    }

    @Test
    public void isNullCiteKeyThrowsNPE() {
        BibEntry e = new BibEntry(StandardEntryType.Article);
        assertThrows(NullPointerException.class, () -> e.setCiteKey(null));
    }

    @Test
    public void isEmptyCiteKey() {
        BibEntry e = new BibEntry(StandardEntryType.Article);
        assertFalse(e.hasCiteKey());

        e.setCiteKey("");
        assertFalse(e.hasCiteKey());

        e.setCiteKey("key");
        assertTrue(e.hasCiteKey());

        e.clearField(InternalField.KEY_FIELD);
        assertFalse(e.hasCiteKey());
    }

    @Test
    public void settingTypeToNullThrowsException() {
        assertThrows(NullPointerException.class, () -> keywordEntry.setType(null));
    }

    @Test
    public void getFieldOrAliasDateWithYearNumericalMonthString() {
        emptyEntry.setField(StandardField.YEAR, "2003");
        emptyEntry.setField(StandardField.MONTH, "3");
        assertEquals(Optional.of("2003-03"), emptyEntry.getFieldOrAlias(StandardField.DATE));
    }

    @Test
    public void getFieldOrAliasDateWithYearAbbreviatedMonth() {
        emptyEntry.setField(StandardField.YEAR, "2003");
        emptyEntry.setField(StandardField.MONTH, "#mar#");
        assertEquals(Optional.of("2003-03"), emptyEntry.getFieldOrAlias(StandardField.DATE));
    }

    @Test
    public void getFieldOrAliasDateWithYearAbbreviatedMonthString() {
        emptyEntry.setField(StandardField.YEAR, "2003");
        emptyEntry.setField(StandardField.MONTH, "mar");
        assertEquals(Optional.of("2003-03"), emptyEntry.getFieldOrAlias(StandardField.DATE));
    }

    @Test
    public void getFieldOrAliasDateWithOnlyYear() {
        emptyEntry.setField(StandardField.YEAR, "2003");
        assertEquals(Optional.of("2003"), emptyEntry.getFieldOrAlias(StandardField.DATE));
    }

    @Test
    public void getFieldOrAliasYearWithDateYYYY() {
        emptyEntry.setField(StandardField.DATE, "2003");
        assertEquals(Optional.of("2003"), emptyEntry.getFieldOrAlias(StandardField.YEAR));
    }

    @Test
    public void getFieldOrAliasYearWithDateYYYYMM() {
        emptyEntry.setField(StandardField.DATE, "2003-03");
        assertEquals(Optional.of("2003"), emptyEntry.getFieldOrAlias(StandardField.YEAR));
    }

    @Test
    public void getFieldOrAliasYearWithDateYYYYMMDD() {
        emptyEntry.setField(StandardField.DATE, "2003-03-30");
        assertEquals(Optional.of("2003"), emptyEntry.getFieldOrAlias(StandardField.YEAR));
    }

    @Test
    public void getFieldOrAliasMonthWithDateYYYYReturnsNull() {
        emptyEntry.setField(StandardField.DATE, "2003");
        assertEquals(Optional.empty(), emptyEntry.getFieldOrAlias(StandardField.MONTH));
    }

    @Test
    public void getFieldOrAliasMonthWithDateYYYYMM() {
        emptyEntry.setField(StandardField.DATE, "2003-03");
        assertEquals(Optional.of("#mar#"), emptyEntry.getFieldOrAlias(StandardField.MONTH));
    }

    @Test
    public void getFieldOrAliasMonthWithDateYYYYMMDD() {
        emptyEntry.setField(StandardField.DATE, "2003-03-30");
        assertEquals(Optional.of("#mar#"), emptyEntry.getFieldOrAlias(StandardField.MONTH));
    }

    @Test
    public void getFieldOrAliasLatexFreeAlreadyFreeValueIsUnchanged() {
        emptyEntry.setField(StandardField.TITLE, "A Title Without any LaTeX commands");
        assertEquals(Optional.of("A Title Without any LaTeX commands"), emptyEntry.getFieldOrAliasLatexFree(StandardField.TITLE));
    }

    @Test
    public void getFieldOrAliasLatexFreeAlreadyFreeAliasValueIsUnchanged() {
        emptyEntry.setField(StandardField.JOURNAL, "A Title Without any LaTeX commands");
        assertEquals(Optional.of("A Title Without any LaTeX commands"), emptyEntry.getFieldOrAliasLatexFree(StandardField.JOURNALTITLE));
    }

    @Test
    public void getFieldOrAliasLatexFreeBracesAreRemoved() {
        emptyEntry.setField(StandardField.TITLE, "{A Title with some {B}ra{C}es}");
        assertEquals(Optional.of("A Title with some BraCes"), emptyEntry.getFieldOrAliasLatexFree(StandardField.TITLE));
    }

    @Test
    public void getFieldOrAliasLatexFreeBracesAreRemovedFromAlias() {
        emptyEntry.setField(StandardField.JOURNAL, "{A Title with some {B}ra{C}es}");
        assertEquals(Optional.of("A Title with some BraCes"), emptyEntry.getFieldOrAliasLatexFree(StandardField.JOURNALTITLE));
    }

    @Test
    public void getFieldOrAliasLatexFreeComplexConversionInAlias() {
        emptyEntry.setField(StandardField.JOURNAL, "A 32~{mA} {$\\Sigma\\Delta$}-modulator");
        assertEquals(Optional.of("A 32 mA ΣΔ-modulator"), emptyEntry.getFieldOrAliasLatexFree(StandardField.JOURNALTITLE));
    }

    @Test
    public void setNullField() {
        assertThrows(NullPointerException.class, () -> emptyEntry.setField(null));
    }

    @Test
    public void addNullKeywordThrowsNPE() {
        assertThrows(NullPointerException.class, () -> keywordEntry.addKeyword((Keyword) null, ','));
    }

    @Test
    public void putNullKeywordListThrowsNPE() {
        assertThrows(NullPointerException.class, () -> keywordEntry.putKeywords((KeywordList) null, ','));
    }

    @Test
    public void putNullKeywordSeparatorThrowsNPE() {
        assertThrows(NullPointerException.class, () -> keywordEntry.putKeywords(Arrays.asList("A", "B"), null));
    }

    @Test
    public void testGetSeparatedKeywordsAreCorrect() {
        assertEquals(new KeywordList("Foo", "Bar"), keywordEntry.getKeywords(','));
    }

    @Test
    public void testAddKeywordIsCorrect() {
        keywordEntry.addKeyword("FooBar", ',');
        assertEquals(new KeywordList("Foo", "Bar", "FooBar"), keywordEntry.getKeywords(','));
    }

    @Test
    public void testAddKeywordHasChanged() {
        keywordEntry.addKeyword("FooBar", ',');
        assertTrue(keywordEntry.hasChanged());
    }

    @Test
    public void testAddKeywordTwiceYiedsOnlyOne() {
        keywordEntry.addKeyword("FooBar", ',');
        keywordEntry.addKeyword("FooBar", ',');
        assertEquals(new KeywordList("Foo", "Bar", "FooBar"), keywordEntry.getKeywords(','));
    }

    @Test
    public void addKeywordIsCaseSensitive() {
        keywordEntry.addKeyword("FOO", ',');
        assertEquals(new KeywordList("Foo", "Bar", "FOO"), keywordEntry.getKeywords(','));
    }

    @Test
    public void testAddKeywordWithDifferentCapitalizationChanges() {
        keywordEntry.addKeyword("FOO", ',');
        assertTrue(keywordEntry.hasChanged());
    }

    @Test
    public void testAddKeywordEmptyKeywordIsNotAdded() {
        keywordEntry.addKeyword("", ',');
        assertEquals(new KeywordList("Foo", "Bar"), keywordEntry.getKeywords(','));
    }

    @Test
    public void testAddKeywordEmptyKeywordNotChanged() {
        keywordEntry.addKeyword("", ',');
        assertFalse(keywordEntry.hasChanged());
    }

    @Test
    public void texNewBibEntryHasNoKeywords() {
        assertTrue(emptyEntry.getKeywords(',').isEmpty());
    }

    @Test
    public void texNewBibEntryHasNoKeywordsEvenAfterAddingEmptyKeyword() {
        emptyEntry.addKeyword("", ',');
        assertTrue(emptyEntry.getKeywords(',').isEmpty());
    }

    @Test
    public void texNewBibEntryAfterAddingEmptyKeywordNotChanged() {
        emptyEntry.addKeyword("", ',');
        assertFalse(emptyEntry.hasChanged());
    }

    @Test
    public void testAddKeywordsWorksAsExpected() {
        emptyEntry.addKeywords(Arrays.asList("Foo", "Bar"), ',');
        assertEquals(new KeywordList("Foo", "Bar"), emptyEntry.getKeywords(','));
    }

    @Test
    public void testPutKeywordsOverwritesOldKeywords() {
        keywordEntry.putKeywords(Arrays.asList("Yin", "Yang"), ',');
        assertEquals(new KeywordList("Yin", "Yang"), keywordEntry.getKeywords(','));
    }

    @Test
    public void testPutKeywordsHasChanged() {
        keywordEntry.putKeywords(Arrays.asList("Yin", "Yang"), ',');
        assertTrue(keywordEntry.hasChanged());
    }

    @Test
    public void testPutKeywordsPutEmpyListErasesPreviousKeywords() {
        keywordEntry.putKeywords(Collections.emptyList(), ',');
        assertTrue(keywordEntry.getKeywords(',').isEmpty());
    }

    @Test
    public void testPutKeywordsPutEmpyListHasChanged() {
        keywordEntry.putKeywords(Collections.emptyList(), ',');
        assertTrue(keywordEntry.hasChanged());
    }

    @Test
    public void testPutKeywordsPutEmpyListToEmptyBibentry() {
        emptyEntry.putKeywords(Collections.emptyList(), ',');
        assertTrue(emptyEntry.getKeywords(',').isEmpty());
    }

    @Test
    public void testPutKeywordsPutEmpyListToEmptyBibentryNotChanged() {
        emptyEntry.putKeywords(Collections.emptyList(), ',');
        assertFalse(emptyEntry.hasChanged());
    }

    @Test
    public void putKeywordsToEmptyReturnsNoChange() {
        Optional<FieldChange> change = emptyEntry.putKeywords(Collections.emptyList(), ',');
        assertEquals(Optional.empty(), change);
    }

    @Test
    public void clearKeywordsReturnsChange() {
        Optional<FieldChange> change = keywordEntry.putKeywords(Collections.emptyList(), ',');
        assertEquals(Optional.of(new FieldChange(keywordEntry, StandardField.KEYWORDS, "Foo, Bar", null)), change);
    }

    @Test
    public void changeKeywordsReturnsChange() {
        Optional<FieldChange> change = keywordEntry.putKeywords(Arrays.asList("Test", "FooTest"), ',');
        assertEquals(Optional.of(new FieldChange(keywordEntry, StandardField.KEYWORDS, "Foo, Bar", "Test, FooTest")),
                change);
    }

    @Test
    public void putKeywordsToSameReturnsNoChange() {
        Optional<FieldChange> change = keywordEntry.putKeywords(Arrays.asList("Foo", "Bar"), ',');
        assertEquals(Optional.empty(), change);
    }

    @Test
    public void getKeywordsReturnsParsedKeywordListFromKeywordsField() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.KEYWORDS, "w1, w2a w2b, w3");
        assertEquals(new KeywordList("w1", "w2a w2b", "w3"), entry.getKeywords(','));
    }

    @Test
    public void removeKeywordsOnEntryWithoutKeywordsDoesNothing() {
        BibEntry entry = new BibEntry();
        Optional<FieldChange> change = entry.removeKeywords(SpecialField.RANKING.getKeyWords(), ',');
        assertEquals(Optional.empty(), change);
    }

    @Test
    public void removeKeywordsWithEmptyListDoesNothing() {
        keywordEntry.putKeywords(Arrays.asList("kw1", "kw2"), ',');
        Optional<FieldChange> change = keywordEntry.removeKeywords(new KeywordList(), ',');
        assertEquals(Optional.empty(), change);
    }

    @Test
    public void removeKeywordsWithNonExistingKeywordsDoesNothing() {
        keywordEntry.putKeywords(Arrays.asList("kw1", "kw2"), ',');
        Optional<FieldChange> change = keywordEntry.removeKeywords(KeywordList.parse("kw3, kw4", ','), ',');
        assertEquals(Optional.empty(), change);
        assertEquals(Sets.newHashSet("kw1", "kw2"), keywordEntry.getKeywords(',').toStringList());
    }

    @Test
    public void removeKeywordsWithExistingKeywordsRemovesThem() {
        keywordEntry.putKeywords(Arrays.asList("kw1", "kw2", "kw3"), ',');
        Optional<FieldChange> change = keywordEntry.removeKeywords(KeywordList.parse("kw1, kw2", ','), ',');
        assertTrue(change.isPresent());
        assertEquals(KeywordList.parse("kw3", ','), keywordEntry.getKeywords(','));
    }

    @Test
    public void setCiteKey() {
        BibEntry be = new BibEntry();
        assertFalse(be.hasCiteKey());
        be.setField(StandardField.AUTHOR, "Albert Einstein");
        be.setCiteKey("Einstein1931");
        assertTrue(be.hasCiteKey());
        assertEquals(Optional.of("Einstein1931"), be.getCiteKeyOptional());
        assertEquals(Optional.of("Albert Einstein"), be.getField(StandardField.AUTHOR));
        be.clearField(StandardField.AUTHOR);
        assertEquals(Optional.empty(), be.getField(StandardField.AUTHOR));
    }
}
