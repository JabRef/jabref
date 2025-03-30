package org.jabref.gui.mergeentries;

import java.time.Year;
import java.util.Optional;

import org.jabref.gui.mergeentries.newmergedialog.FieldRowViewModel;
import org.jabref.gui.mergeentries.newmergedialog.fieldsmerger.FieldMergerFactory;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jbibtex.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FieldRowViewModelTest {

    BibEntry leftEntry;
    BibEntry rightEntry;

    BibEntry extraEntry;
    BibEntry extraEntry2;
    BibEntry extraEntry3;
    BibEntry extraEntry4;
    BibEntry extraEntry5;
    BibEntry extraEntry6;

    BibEntry mergedEntry;

    FieldMergerFactory fieldMergerFactory;

    BibEntryPreferences bibEntryPreferences = mock(BibEntryPreferences.class);

    @BeforeEach
    void setup() throws ParseException {
        leftEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("LajnDiezScheinEtAl2012")
                .withField(StandardField.AUTHOR, "Lajn, A and Diez, T and Schein, F and Frenzel, H and von Wenckstern, H and Grundmann, M")
                .withField(StandardField.TITLE, "Light and temperature stability of fully transparent ZnO-based inverter circuits")
                .withField(StandardField.NUMBER, "4")
                .withField(StandardField.PAGES, "515--517")
                .withField(StandardField.VOLUME, "32")
                .withField(StandardField.GROUPS, "Skimmed")
                .withField(StandardField.JOURNAL, "Electron Device Letters, IEEE")
                .withField(StandardField.PUBLISHER, "IEEE")
                .withField(StandardField.YEAR, "2012");

        rightEntry = new BibEntry(StandardEntryType.Book)
                .withCitationKey("KolbLenhardWirtz2012")
                .withField(StandardField.AUTHOR, "Stefan Kolb and Guido Wirtz")
                .withField(StandardField.BOOKTITLE, "Proceedings of the 5\\textsuperscript{th} {IEEE} International Conference on Service-Oriented Computing and Applications {(SOCA'12)}, Taipei, Taiwan")
                .withField(StandardField.TITLE, "{Bridging the Heterogeneity of Orchestrations - A Petri Net-based Integration of BPEL and Windows Workflow}")
                .withField(StandardField.ORGANIZATION, "IEEE")
                .withField(StandardField.PAGES, "1--8")
                .withField(StandardField.ADDRESS, "Oxford, United Kingdom")
                .withField(StandardField.GROUPS, "By rating, Skimmed")
                .withMonth(Month.DECEMBER)
                .withField(StandardField.KEYWORDS, "a, b, c")
                .withField(StandardField.YEAR, "2012");

        extraEntry = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("BoopalGarridoGustafsson2013")
                .withField(StandardField.AUTHOR, "Padma Prasad Boopal and Mario Garrido and Oscar Gustafsson")
                .withField(StandardField.BOOKTITLE, "2013 {IEEE} International Symposium on Circuits and Systems (ISCAS2013), Beijing, China, May 19-23, 2013")
                .withField(StandardField.TITLE, "A reconfigurable {FFT} architecture for variable-length and multi-streaming {OFDM} standards")
                .withField(StandardField.KEYWORDS, "b, c, a")
                .withField(StandardField.YEAR, "2013");

        extraEntry2 = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.YEAR, "1750");

        extraEntry3 = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.YEAR, String.valueOf(Year.now().getValue() + 110));

        extraEntry4 = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.YEAR, "2001");

        extraEntry5 = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.YEAR, "2022");

        extraEntry6 = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.YEAR, "2011");

        mergedEntry = new BibEntry();
        fieldMergerFactory = new FieldMergerFactory(bibEntryPreferences);

        when(bibEntryPreferences.getKeywordSeparator()).thenReturn(',');
    }

    @Test
    void selectNonEmptyValueShouldSelectLeftFieldValueIfItIsNotEmpty() {
        var numberFieldViewModel = createViewModelForField(StandardField.NUMBER);
        numberFieldViewModel.selectNonEmptyValue();
        assertEquals(FieldRowViewModel.Selection.LEFT, numberFieldViewModel.getSelection());

        var authorFieldViewModel = createViewModelForField(StandardField.AUTHOR);
        authorFieldViewModel.selectNonEmptyValue();
        assertEquals(FieldRowViewModel.Selection.LEFT, authorFieldViewModel.getSelection());
    }

    @Test
    void selectNonEmptyValueShouldSelectRightFieldValueIfLeftValueIsEmpty() {
        var monthFieldViewModel = createViewModelForField(StandardField.MONTH);
        monthFieldViewModel.selectNonEmptyValue();
        assertEquals(FieldRowViewModel.Selection.RIGHT, monthFieldViewModel.getSelection());

        var addressFieldViewModel = createViewModelForField(StandardField.ADDRESS);
        addressFieldViewModel.selectNonEmptyValue();
        assertEquals(FieldRowViewModel.Selection.RIGHT, addressFieldViewModel.getSelection());
    }

    @Test
    void hasEqualLeftAndRightValuesShouldReturnFalseIfOneOfTheValuesIsEmpty() {
        var monthFieldViewModel = createViewModelForField(StandardField.MONTH);
        monthFieldViewModel.selectNonEmptyValue();
        assertFalse(monthFieldViewModel.hasEqualLeftAndRightValues());
    }

    @Test
    void hasEqualLeftAndRightValuesShouldReturnTrueIfLeftAndRightAreEqual() {
        var yearFieldViewModel = createViewModelForField(StandardField.YEAR);
        yearFieldViewModel.selectNonEmptyValue();
        assertTrue(yearFieldViewModel.hasEqualLeftAndRightValues());
    }

    @Test
    @Disabled("This test is kept as a reminder to implement a different comparison logic based on the given field.")
    void hasEqualLeftAndRightValuesShouldReturnTrueIfKeywordsAreEqual() {
        FieldRowViewModel keywordsField = new FieldRowViewModel(StandardField.KEYWORDS, rightEntry, extraEntry, mergedEntry, fieldMergerFactory);
        assertTrue(keywordsField.hasEqualLeftAndRightValues());
    }

    @Test
    void selectLeftValueShouldBeCorrect() {
        var monthFieldViewModel = createViewModelForField(StandardField.MONTH);
        monthFieldViewModel.selectLeftValue();

        assertEquals(FieldRowViewModel.Selection.LEFT, monthFieldViewModel.getSelection());
        assertEquals(Optional.of(""), Optional.ofNullable(monthFieldViewModel.getMergedFieldValue()));

        monthFieldViewModel.selectRightValue();
        monthFieldViewModel.selectLeftValue();
        assertEquals(FieldRowViewModel.Selection.LEFT, monthFieldViewModel.getSelection());
        assertEquals(Optional.of(""), Optional.of(monthFieldViewModel.getMergedFieldValue()));
    }

    @Test
    void selectRightValueShouldBeCorrect() {
        var monthFieldViewModel = createViewModelForField(StandardField.MONTH);
        monthFieldViewModel.selectRightValue();
        assertEquals(FieldRowViewModel.Selection.RIGHT, monthFieldViewModel.getSelection());
        assertEquals(rightEntry.getField(StandardField.MONTH), Optional.of(monthFieldViewModel.getMergedFieldValue()));

        monthFieldViewModel.selectLeftValue();
        monthFieldViewModel.selectRightValue();
        assertEquals(FieldRowViewModel.Selection.RIGHT, monthFieldViewModel.getSelection());
        assertEquals(rightEntry.getField(StandardField.MONTH), Optional.of(monthFieldViewModel.getMergedFieldValue()));
    }

    @Test
    void isFieldsMergedShouldReturnTrueIfLeftAndRightValuesAreEqual() {
        var yearFieldViewModel = createViewModelForField(StandardField.YEAR);
        assertTrue(yearFieldViewModel.isFieldsMerged());
    }

    @Test
    void isFieldsMergedShouldReturnFalseIfLeftAndRightValuesAreNotEqual() {
        var monthFieldViewModel = createViewModelForField(StandardField.MONTH);
        assertFalse(monthFieldViewModel.isFieldsMerged());

        var addressFieldViewModel = createViewModelForField(StandardField.ADDRESS);
        assertFalse(addressFieldViewModel.isFieldsMerged());

        var authorFieldViewModel = createViewModelForField(StandardField.AUTHOR);
        assertFalse(authorFieldViewModel.isFieldsMerged());
    }

    @Test
    void mergeFieldsShouldResultInLeftAndRightValuesBeingEqual() {
        var groupsField = createViewModelForField(StandardField.GROUPS);
        groupsField.mergeFields();
        assertEquals(groupsField.getLeftFieldValue(), groupsField.getRightFieldValue());
    }

    @Test
    void mergeFieldsShouldBeCorrectEvenWhenOnOfTheValuesIsEmpty() {
        var keywordsField = createViewModelForField(StandardField.KEYWORDS);
        keywordsField.mergeFields();
        assertEquals(keywordsField.getLeftFieldValue(), keywordsField.getRightFieldValue());
    }

    @Test
    void mergeFieldsShouldThrowUnsupportedOperationExceptionIfTheGivenFieldCanBeMerged() {
        var authorField = createViewModelForField(StandardField.AUTHOR);
        assertThrows(UnsupportedOperationException.class, authorField::mergeFields);
    }

    @Test
    void mergeFieldsShouldSelectLeftFieldValue() {
        var groupsField = createViewModelForField(StandardField.GROUPS);
        groupsField.mergeFields();

        assertEquals(FieldRowViewModel.Selection.LEFT, groupsField.getSelection());
    }

    @Test
    void unmergeFieldsShouldBeCorrect() {
        var groupsField = createViewModelForField(StandardField.GROUPS);
        var oldLeftGroups = groupsField.getLeftFieldValue();
        var oldRightGroups = groupsField.getRightFieldValue();
        groupsField.mergeFields();
        groupsField.unmergeFields();

        assertEquals(oldLeftGroups, groupsField.getLeftFieldValue());
        assertEquals(oldRightGroups, groupsField.getRightFieldValue());
    }

    @Test
    void unmergeFieldsShouldDoNothingIfFieldsAreNotMerged() {
        var groupsField = createViewModelForField(StandardField.GROUPS);
        var oldLeftGroups = groupsField.getLeftFieldValue();
        var oldRightGroups = groupsField.getRightFieldValue();
        groupsField.unmergeFields();

        assertEquals(oldLeftGroups, groupsField.getLeftFieldValue());
        assertEquals(oldRightGroups, groupsField.getRightFieldValue());
    }

    @Test
    void autoSelectBetterYearWhenMergingExistingYearOutOfBounds() {
        var yearField = create2ndViewModelForField(StandardField.YEAR);
        yearField.autoSelectBetterValue();

        var yearField2 = create3rdViewModelForField(StandardField.YEAR);
        yearField2.autoSelectBetterValue();

        assertEquals(FieldRowViewModel.Selection.RIGHT, yearField.getSelection());
        assertEquals(FieldRowViewModel.Selection.RIGHT, yearField2.getSelection());
    }

    @Test
    void autoSelectBetterYearWhenMergingExistingYearWithinBounds() {
        var yearField = create4thViewModelForField(StandardField.YEAR);
        yearField.autoSelectBetterValue();

        var yearField2 = create5thViewModelForField(StandardField.YEAR);
        yearField2.autoSelectBetterValue();

        var yearField3 = create6thViewModelForField(StandardField.YEAR);
        yearField3.autoSelectBetterValue();

        assertEquals(FieldRowViewModel.Selection.RIGHT, yearField.getSelection());
        assertEquals(FieldRowViewModel.Selection.LEFT, yearField2.getSelection());
        assertEquals(FieldRowViewModel.Selection.LEFT, yearField3.getSelection());
    }

    public FieldRowViewModel createViewModelForField(Field field) {
        return new FieldRowViewModel(field, leftEntry, rightEntry, mergedEntry, fieldMergerFactory);
    }

    public FieldRowViewModel create2ndViewModelForField(Field field) {
        return new FieldRowViewModel(field, extraEntry2, extraEntry6, mergedEntry, fieldMergerFactory);
    }

    public FieldRowViewModel create3rdViewModelForField(Field field) {
        return new FieldRowViewModel(field, extraEntry3, extraEntry6, mergedEntry, fieldMergerFactory);
    }

    public FieldRowViewModel create4thViewModelForField(Field field) {
        return new FieldRowViewModel(field, extraEntry4, extraEntry, mergedEntry, fieldMergerFactory);
    }

    public FieldRowViewModel create5thViewModelForField(Field field) {
        return new FieldRowViewModel(field, extraEntry5, extraEntry6, mergedEntry, fieldMergerFactory);
    }

    public FieldRowViewModel create6thViewModelForField(Field field) {
        return new FieldRowViewModel(field, extraEntry, extraEntry5, mergedEntry, fieldMergerFactory);
    }
}
