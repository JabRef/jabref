package org.jabref.gui.mergeentries;

import java.util.Optional;

import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.mergeentries.newmergedialog.FieldRowViewModel;
import org.jabref.gui.mergeentries.newmergedialog.fieldsmerger.FieldMergerFactory;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.PreferencesService;

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

    BibEntry mergedEntry;

    FieldRowViewModel viewModel;

    FieldMergerFactory fieldMergerFactory;

    PreferencesService mockedPrefs = mock(PreferencesService.class);
    GroupsPreferences mockedGroupsPrefs = mock(GroupsPreferences.class);

    @BeforeEach
    public void setup() throws ParseException {
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
        fieldMergerFactory = new FieldMergerFactory(mockedPrefs);

        when(mockedPrefs.getGroupsPreferences()).thenReturn(mockedGroupsPrefs);
        when(mockedGroupsPrefs.getKeywordSeparator()).thenReturn(',');
    }

    @Test
    public void selectNonEmptyValueShouldSelectLeftFieldValueIfItIsNotEmpty() {
        var numberFieldViewModel = createViewModelForField(StandardField.NUMBER);
        numberFieldViewModel.selectNonEmptyValue();
        assertEquals(FieldRowViewModel.Selection.LEFT, numberFieldViewModel.getSelection());

        var authorFieldViewModel = createViewModelForField(StandardField.AUTHOR);
        authorFieldViewModel.selectNonEmptyValue();
        assertEquals(FieldRowViewModel.Selection.LEFT, authorFieldViewModel.getSelection());
    }

    @Test
    public void selectNonEmptyValueShouldSelectRightFieldValueIfLeftValueIsEmpty() {
        var monthFieldViewModel = createViewModelForField(StandardField.MONTH);
        monthFieldViewModel.selectNonEmptyValue();
        assertEquals(FieldRowViewModel.Selection.RIGHT, monthFieldViewModel.getSelection());

        var addressFieldViewModel = createViewModelForField(StandardField.ADDRESS);
        addressFieldViewModel.selectNonEmptyValue();
        assertEquals(FieldRowViewModel.Selection.RIGHT, addressFieldViewModel.getSelection());
    }

    @Test
    public void hasEqualLeftAndRightValuesShouldReturnFalseIfOneOfTheValuesIsEmpty() {
        var monthFieldViewModel = createViewModelForField(StandardField.MONTH);
        monthFieldViewModel.selectNonEmptyValue();
        assertFalse(monthFieldViewModel.hasEqualLeftAndRightValues());
    }

    @Test
    public void hasEqualLeftAndRightValuesShouldReturnTrueIfLeftAndRightAreEqual() {
        var yearFieldViewModel = createViewModelForField(StandardField.YEAR);
        yearFieldViewModel.selectNonEmptyValue();
        assertTrue(yearFieldViewModel.hasEqualLeftAndRightValues());
    }


    @Test
    @Disabled("This test is kept as a reminder to implement a different comparison logic based on the given field.")
    public void hasEqualLeftAndRightValuesShouldReturnTrueIfKeywordsAreEqual() {
        FieldRowViewModel keywordsField = new FieldRowViewModel(StandardField.KEYWORDS, rightEntry, extraEntry, mergedEntry, fieldMergerFactory);
        assertTrue(keywordsField.hasEqualLeftAndRightValues());
    }

    @Test
    public void selectLeftValueShouldBeCorrect() {
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
    public void selectRightValueShouldBeCorrect() {
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
    public void isFieldsMergedShouldReturnTrueIfLeftAndRightValuesAreEqual() {
        var yearFieldViewModel = createViewModelForField(StandardField.YEAR);
        assertTrue(yearFieldViewModel.isFieldsMerged());
    }

    @Test
    public void isFieldsMergedShouldReturnFalseIfLeftAndRightValuesAreNotEqual() {
        var monthFieldViewModel = createViewModelForField(StandardField.MONTH);
        assertFalse(monthFieldViewModel.isFieldsMerged());

        var addressFieldViewModel = createViewModelForField(StandardField.ADDRESS);
        assertFalse(addressFieldViewModel.isFieldsMerged());

        var authorFieldViewModel = createViewModelForField(StandardField.AUTHOR);
        assertFalse(authorFieldViewModel.isFieldsMerged());
    }

    @Test
    public void mergeFieldsShouldResultInLeftAndRightValuesBeingEqual() {
        var groupsField = createViewModelForField(StandardField.GROUPS);
        groupsField.mergeFields();
        assertEquals(groupsField.getLeftFieldValue(), groupsField.getRightFieldValue());
    }

    @Test
    public void mergeFieldsShouldBeCorrectEvenWhenOnOfTheValuesIsEmpty() {
        var keywordsField = createViewModelForField(StandardField.KEYWORDS);
        keywordsField.mergeFields();
        assertEquals(keywordsField.getLeftFieldValue(), keywordsField.getRightFieldValue());
    }

    @Test
    public void mergeFieldsShouldThrowUnsupportedOperationExceptionIfTheGivenFieldCanBeMerged() {
        var authorField = createViewModelForField(StandardField.AUTHOR);
        assertThrows(UnsupportedOperationException.class, authorField::mergeFields);
    }

    @Test
    public void mergeFieldsShouldSelectLeftFieldValue() {
        var groupsField = createViewModelForField(StandardField.GROUPS);
        groupsField.mergeFields();

        assertEquals(FieldRowViewModel.Selection.LEFT, groupsField.getSelection());
    }

    @Test
    public void unmergeFieldsShouldBeCorrect() {
        var groupsField = createViewModelForField(StandardField.GROUPS);
        var oldLeftGroups = groupsField.getLeftFieldValue();
        var oldRightGroups = groupsField.getRightFieldValue();
        groupsField.mergeFields();
        groupsField.unmergeFields();

        assertEquals(oldLeftGroups, groupsField.getLeftFieldValue());
        assertEquals(oldRightGroups, groupsField.getRightFieldValue());
    }

    @Test
    public void unmergeFieldsShouldDoNothingIfFieldsAreNotMerged() {
        var groupsField = createViewModelForField(StandardField.GROUPS);
        var oldLeftGroups = groupsField.getLeftFieldValue();
        var oldRightGroups = groupsField.getRightFieldValue();
        groupsField.unmergeFields();

        assertEquals(oldLeftGroups, groupsField.getLeftFieldValue());
        assertEquals(oldRightGroups, groupsField.getRightFieldValue());
    }

    public FieldRowViewModel createViewModelForField(Field field) {
        return new FieldRowViewModel(field, leftEntry, rightEntry, mergedEntry, fieldMergerFactory);
    }
}
