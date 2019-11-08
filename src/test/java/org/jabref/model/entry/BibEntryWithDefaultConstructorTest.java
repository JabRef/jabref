package org.jabref.model.entry;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simple tests for a BibEntry. There are more test classes testing special features.
 */
public class BibEntryWithDefaultConstructorTest {

    private BibEntry entry;

    @BeforeEach
    public void setUp() {
        entry = new BibEntry();
    }

    @AfterEach
    public void tearDown() {
        entry = null;
    }

    @Test
    public void testDefaultConstructor() {
        assertEquals(StandardEntryType.Misc, entry.getType());
        assertNotNull(entry.getId());
        assertFalse(entry.getField(StandardField.AUTHOR).isPresent());
    }

    @Test
    public void settingTypeToNullThrowsException() {
        assertThrows(NullPointerException.class, () -> entry.setType(null));
    }

    @Test
    public void setNullFieldThrowsNPE() {
        assertThrows(NullPointerException.class, () -> entry.setField(null));
    }

    @Test
    public void getFieldIsCaseInsensitive() throws Exception {
        entry.setField(new UnknownField("TeSt"), "value");
        assertEquals(Optional.of("value"), entry.getField(new UnknownField("tEsT")));
    }

    @Test
    public void getFieldWorksWithBibFieldAsWell() throws Exception {
        entry.setField(StandardField.AUTHOR, "value");
        assertEquals(Optional.of("value"), entry.getField(new BibField(StandardField.AUTHOR, FieldPriority.IMPORTANT).getField()));
    }

    @Test
    public void setFieldWorksWithBibFieldAsWell() throws Exception {
        entry.setField(new BibField(StandardField.AUTHOR, FieldPriority.IMPORTANT).getField(), "value");
        assertEquals(Optional.of("value"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    public void clonedBibEntryHasUniqueID() throws Exception {
        BibEntry entryClone = (BibEntry) entry.clone();
        assertNotEquals(entry.getId(), entryClone.getId());
    }

    @Test
    public void setAndGetAreConsistentForMonth() throws Exception {
        entry.setField(StandardField.MONTH, "may");
        assertEquals(Optional.of("may"), entry.getField(StandardField.MONTH));
    }

    @Test
    public void setAndGetAreConsistentForCapitalizedMonth() throws Exception {
        entry.setField(StandardField.MONTH, "May");
        assertEquals(Optional.of("May"), entry.getField(StandardField.MONTH));
    }

    @Test
    public void setAndGetAreConsistentForMonthString() throws Exception {
        entry.setField(StandardField.MONTH, "#may#");
        assertEquals(Optional.of("#may#"), entry.getField(StandardField.MONTH));
    }

    @Test
    public void monthCorrectlyReturnedForMonth() throws Exception {
        entry.setField(StandardField.MONTH, "may");
        assertEquals(Optional.of(Month.MAY), entry.getMonth());
    }

    @Test
    public void monthCorrectlyReturnedForCapitalizedMonth() throws Exception {
        entry.setField(StandardField.MONTH, "May");
        assertEquals(Optional.of(Month.MAY), entry.getMonth());
    }

    @Test
    public void monthCorrectlyReturnedForMonthString() throws Exception {
        entry.setField(StandardField.MONTH, "#may#");
        assertEquals(Optional.of(Month.MAY), entry.getMonth());
    }

    @Test
    public void monthCorrectlyReturnedForMonthMay() throws Exception {
        entry.setMonth(Month.MAY);
        assertEquals(Optional.of(Month.MAY), entry.getMonth());
    }

    @Test
    public void monthFieldCorrectlyReturnedForMonthMay() throws Exception {
        entry.setMonth(Month.MAY);
        assertEquals(Optional.of("#may#"), entry.getField(StandardField.MONTH));
    }

    @Test
    public void getFieldOrAliasDateWithYearNumericalMonthString() {
        entry.setField(StandardField.YEAR, "2003");
        entry.setField(StandardField.MONTH, "3");
        assertEquals(Optional.of("2003-03"), entry.getFieldOrAlias(StandardField.DATE));
    }

    @Test
    public void getFieldOrAliasDateWithYearAbbreviatedMonth() {
        entry.setField(StandardField.YEAR, "2003");
        entry.setField(StandardField.MONTH, "#mar#");
        assertEquals(Optional.of("2003-03"), entry.getFieldOrAlias(StandardField.DATE));
    }

    @Test
    public void getFieldOrAliasDateWithYearAbbreviatedMonthString() {
        entry.setField(StandardField.YEAR, "2003");
        entry.setField(StandardField.MONTH, "mar");
        assertEquals(Optional.of("2003-03"), entry.getFieldOrAlias(StandardField.DATE));
    }

    @Test
    public void getFieldOrAliasDateWithOnlyYear() {
        entry.setField(StandardField.YEAR, "2003");
        assertEquals(Optional.of("2003"), entry.getFieldOrAlias(StandardField.DATE));
    }

    @Test
    public void getFieldOrAliasYearWithDateYYYY() {
        entry.setField(StandardField.DATE, "2003");
        assertEquals(Optional.of("2003"), entry.getFieldOrAlias(StandardField.YEAR));
    }

    @Test
    public void getFieldOrAliasYearWithDateYYYYMM() {
        entry.setField(StandardField.DATE, "2003-03");
        assertEquals(Optional.of("2003"), entry.getFieldOrAlias(StandardField.YEAR));
    }

    @Test
    public void getFieldOrAliasYearWithDateYYYYMMDD() {
        entry.setField(StandardField.DATE, "2003-03-30");
        assertEquals(Optional.of("2003"), entry.getFieldOrAlias(StandardField.YEAR));
    }

    @Test
    public void getFieldOrAliasMonthWithDateYYYYReturnsNull() {
        entry.setField(StandardField.DATE, "2003");
        assertEquals(Optional.empty(), entry.getFieldOrAlias(StandardField.MONTH));
    }

    @Test
    public void getFieldOrAliasMonthWithDateYYYYMM() {
        entry.setField(StandardField.DATE, "2003-03");
        assertEquals(Optional.of("#mar#"), entry.getFieldOrAlias(StandardField.MONTH));
    }

    @Test
    public void getFieldOrAliasMonthWithDateYYYYMMDD() {
        entry.setField(StandardField.DATE, "2003-03-30");
        assertEquals(Optional.of("#mar#"), entry.getFieldOrAlias(StandardField.MONTH));
    }

    @Test
    public void getFieldOrAliasLatexFreeAlreadyFreeValueIsUnchanged() {
        entry.setField(StandardField.TITLE, "A Title Without any LaTeX commands");
        assertEquals(Optional.of("A Title Without any LaTeX commands"), entry.getFieldOrAliasLatexFree(StandardField.TITLE));
    }

    @Test
    public void getFieldOrAliasLatexFreeAlreadyFreeAliasValueIsUnchanged() {
        entry.setField(StandardField.JOURNAL, "A Title Without any LaTeX commands");
        assertEquals(Optional.of("A Title Without any LaTeX commands"), entry.getFieldOrAliasLatexFree(StandardField.JOURNALTITLE));
    }

    @Test
    public void getFieldOrAliasLatexFreeBracesAreRemoved() {
        entry.setField(StandardField.TITLE, "{A Title with some {B}ra{C}es}");
        assertEquals(Optional.of("A Title with some BraCes"), entry.getFieldOrAliasLatexFree(StandardField.TITLE));
    }

    @Test
    public void getFieldOrAliasLatexFreeBracesAreRemovedFromAlias() {
        entry.setField(StandardField.JOURNAL, "{A Title with some {B}ra{C}es}");
        assertEquals(Optional.of("A Title with some BraCes"), entry.getFieldOrAliasLatexFree(StandardField.JOURNALTITLE));
    }

    @Test
    public void getFieldOrAliasLatexFreeComplexConversionInAlias() {
        entry.setField(StandardField.JOURNAL, "A 32~{mA} {$\\Sigma\\Delta$}-modulator");
        assertEquals(Optional.of("A 32 mA ΣΔ-modulator"), entry.getFieldOrAliasLatexFree(StandardField.JOURNALTITLE));
    }

    @Test
    public void testGetAndAddToLinkedFileList() {
        List<LinkedFile> files = entry.getFiles();
        files.add(new LinkedFile("", "", ""));
        entry.setFiles(files);
        assertEquals(Arrays.asList(new LinkedFile("", "", "")), entry.getFiles());
    }

    @Test
    public void testGetEmptyKeywords() {
        KeywordList actual = entry.getKeywords(',');

        assertEquals(new KeywordList(), actual);
    }

    @Test
    public void testGetSingleKeywords() {
        entry.addKeyword("kw", ',');
        KeywordList actual = entry.getKeywords(',');

        assertEquals(new KeywordList(new Keyword("kw")), actual);
    }

    @Test
    public void settingCiteKeyLeadsToCorrectCiteKey() {
        assertFalse(entry.hasCiteKey());
        entry.setCiteKey("Einstein1931");
        assertEquals(Optional.of("Einstein1931"), entry.getCiteKeyOptional());
    }

    @Test
    public void settingCiteKeyLeadsToHasCiteKy() {
        assertFalse(entry.hasCiteKey());
        entry.setCiteKey("Einstein1931");
        assertTrue(entry.hasCiteKey());
    }

    @Test
    public void clearFieldWorksForAuthor() {
        entry.setField(StandardField.AUTHOR, "Albert Einstein");
        entry.clearField(StandardField.AUTHOR);
        assertEquals(Optional.empty(), entry.getField(StandardField.AUTHOR));
    }

    @Test
    public void setFieldWorksForAuthor() {
        entry.setField(StandardField.AUTHOR, "Albert Einstein");
        assertEquals(Optional.of("Albert Einstein"), entry.getField(StandardField.AUTHOR));
    }
}
