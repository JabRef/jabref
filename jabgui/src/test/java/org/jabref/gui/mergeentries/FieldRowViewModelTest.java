package org.jabref.gui.mergeentries;

import java.util.Optional;

import org.jabref.gui.mergeentries.newmergedialog.FieldRowViewModel;
import org.jabref.gui.mergeentries.newmergedialog.fieldsmerger.FieldMergerFactory;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

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

    BibEntry mergedEntry;

    FieldMergerFactory fieldMergerFactory;

    BibEntryPreferences bibEntryPreferences = mock(BibEntryPreferences.class);

    @BeforeEach
    void setup() {
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

        mergedEntry = new BibEntry();
        fieldMergerFactory = new FieldMergerFactory(bibEntryPreferences);

        when(bibEntryPreferences.getKeywordSeparator()).thenReturn(',');
    }

    @Test
    void selectNonEmptyValueShouldSelectLeftFieldValueIfItIsNotEmpty() {
        FieldRowViewModel numberFieldViewModel = createViewModelForField(StandardField.NUMBER);
        numberFieldViewModel.selectNonEmptyValue();
        assertEquals(FieldRowViewModel.Selection.LEFT, numberFieldViewModel.getSelection());

        FieldRowViewModel authorFieldViewModel = createViewModelForField(StandardField.AUTHOR);
        authorFieldViewModel.selectNonEmptyValue();
        assertEquals(FieldRowViewModel.Selection.LEFT, authorFieldViewModel.getSelection());
    }

    @Test
    void selectNonEmptyValueShouldSelectRightFieldValueIfLeftValueIsEmpty() {
        FieldRowViewModel monthFieldViewModel = createViewModelForField(StandardField.MONTH);
        monthFieldViewModel.selectNonEmptyValue();
        assertEquals(FieldRowViewModel.Selection.RIGHT, monthFieldViewModel.getSelection());

        FieldRowViewModel addressFieldViewModel = createViewModelForField(StandardField.ADDRESS);
        addressFieldViewModel.selectNonEmptyValue();
        assertEquals(FieldRowViewModel.Selection.RIGHT, addressFieldViewModel.getSelection());
    }

    @Test
    void hasEqualLeftAndRightValuesShouldReturnFalseIfOneOfTheValuesIsEmpty() {
        FieldRowViewModel monthFieldViewModel = createViewModelForField(StandardField.MONTH);
        monthFieldViewModel.selectNonEmptyValue();
        assertFalse(monthFieldViewModel.hasEqualLeftAndRightValues());
    }

    @Test
    void hasEqualLeftAndRightValuesShouldReturnTrueIfLeftAndRightAreEqual() {
        FieldRowViewModel yearFieldViewModel = createViewModelForField(StandardField.YEAR);
        yearFieldViewModel.selectNonEmptyValue();
        assertTrue(yearFieldViewModel.hasEqualLeftAndRightValues());
    }

    @Test
    void newYearShouldBeSelectedForYearsWithLargeValueGap() {
        BibEntry leftEntry = new BibEntry().withField(StandardField.YEAR, "1990");
        BibEntry rightEntry = new BibEntry().withField(StandardField.YEAR, "2020");
        FieldRowViewModel yearField = new FieldRowViewModel(StandardField.YEAR, leftEntry, rightEntry, mergedEntry, fieldMergerFactory);
        yearField.autoSelectBetterValue();
        assertEquals(FieldRowViewModel.Selection.RIGHT, yearField.getSelection());
    }

    @Test
    void yearInRangeShouldBeSelected() {
        BibEntry leftEntry = new BibEntry().withField(StandardField.YEAR, "1700");
        BibEntry rightEntry = new BibEntry().withField(StandardField.YEAR, "2000");
        FieldRowViewModel yearField = new FieldRowViewModel(StandardField.YEAR, leftEntry, rightEntry, mergedEntry, fieldMergerFactory);
        yearField.autoSelectBetterValue();
        assertEquals(FieldRowViewModel.Selection.RIGHT, yearField.getSelection());
    }

    @Test
    @Disabled("This test is kept as a reminder to implement a different comparison logic based on the given field.")
    void hasEqualLeftAndRightValuesShouldReturnTrueIfKeywordsAreEqual() {
        FieldRowViewModel keywordsField = new FieldRowViewModel(StandardField.KEYWORDS, rightEntry, extraEntry, mergedEntry, fieldMergerFactory);
        assertTrue(keywordsField.hasEqualLeftAndRightValues());
    }

    @Test
    void selectLeftValueShouldBeCorrect() {
        FieldRowViewModel monthFieldViewModel = createViewModelForField(StandardField.MONTH);
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
        FieldRowViewModel monthFieldViewModel = createViewModelForField(StandardField.MONTH);
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
        FieldRowViewModel yearFieldViewModel = createViewModelForField(StandardField.YEAR);
        assertTrue(yearFieldViewModel.isFieldsMerged());
    }

    @Test
    void isFieldsMergedShouldReturnFalseIfLeftAndRightValuesAreNotEqual() {
        FieldRowViewModel monthFieldViewModel = createViewModelForField(StandardField.MONTH);
        assertFalse(monthFieldViewModel.isFieldsMerged());

        FieldRowViewModel addressFieldViewModel = createViewModelForField(StandardField.ADDRESS);
        assertFalse(addressFieldViewModel.isFieldsMerged());

        FieldRowViewModel authorFieldViewModel = createViewModelForField(StandardField.AUTHOR);
        assertFalse(authorFieldViewModel.isFieldsMerged());
    }

    @Test
    void mergeFieldsShouldResultInLeftAndRightValuesBeingEqual() {
        FieldRowViewModel groupsField = createViewModelForField(StandardField.GROUPS);
        groupsField.mergeFields();
        assertEquals(groupsField.getLeftFieldValue(), groupsField.getRightFieldValue());
    }

    @Test
    void mergeFieldsShouldBeCorrectEvenWhenOnOfTheValuesIsEmpty() {
        FieldRowViewModel keywordsField = createViewModelForField(StandardField.KEYWORDS);
        keywordsField.mergeFields();
        assertEquals(keywordsField.getLeftFieldValue(), keywordsField.getRightFieldValue());
    }

    @Test
    void mergeFieldsShouldThrowUnsupportedOperationExceptionIfTheGivenFieldCannotBeMerged() {
        FieldRowViewModel authorField = createViewModelForField(StandardField.AUTHOR);
        assertThrows(UnsupportedOperationException.class, authorField::mergeFields);
    }

    @Test
    void mergeFieldsShouldSelectLeftFieldValue() {
        FieldRowViewModel groupsField = createViewModelForField(StandardField.GROUPS);
        groupsField.mergeFields();

        assertEquals(FieldRowViewModel.Selection.LEFT, groupsField.getSelection());
    }

    @Test
    void unmergeFieldsShouldBeCorrect() {
        FieldRowViewModel groupsField = createViewModelForField(StandardField.GROUPS);
        String oldLeftGroups = groupsField.getLeftFieldValue();
        String oldRightGroups = groupsField.getRightFieldValue();
        groupsField.mergeFields();
        groupsField.unmergeFields();

        assertEquals(oldLeftGroups, groupsField.getLeftFieldValue());
        assertEquals(oldRightGroups, groupsField.getRightFieldValue());
    }

    @Test
    void unmergeFieldsShouldDoNothingIfFieldsAreNotMerged() {
        FieldRowViewModel groupsField = createViewModelForField(StandardField.GROUPS);
        String oldLeftGroups = groupsField.getLeftFieldValue();
        String oldRightGroups = groupsField.getRightFieldValue();
        groupsField.unmergeFields();

        assertEquals(oldLeftGroups, groupsField.getLeftFieldValue());
        assertEquals(oldRightGroups, groupsField.getRightFieldValue());
    }

    public FieldRowViewModel createViewModelForField(Field field) {
        return new FieldRowViewModel(field, leftEntry, rightEntry, mergedEntry, fieldMergerFactory);
    }
}
