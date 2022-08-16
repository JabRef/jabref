package org.jabref.gui.mergeentries;

import java.util.Optional;

import org.jabref.gui.mergeentries.newmergedialog.FieldRowViewModel;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jbibtex.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FieldRowViewModelTest {

    BibEntry leftEntry;
    BibEntry rightEntry;

    BibEntry mergedEntry;

    FieldRowViewModel viewModel;

    @BeforeEach
    public void setup() throws ParseException {
        leftEntry = new BibEntry(StandardEntryType.Article);
        leftEntry.setCitationKey("LajnDiezScheinEtAl2012");
        leftEntry.setField(StandardField.AUTHOR, "Lajn, A and Diez, T and Schein, F and Frenzel, H and von Wenckstern, H and Grundmann, M");
        leftEntry.setField(StandardField.TITLE, "Light and temperature stability of fully transparent ZnO-based inverter circuits");
        leftEntry.setField(StandardField.NUMBER, "4");
        leftEntry.setField(StandardField.PAGES, "515--517");
        leftEntry.setField(StandardField.VOLUME, "32");
        leftEntry.setField(StandardField.GROUPS, "Skimmed");
        leftEntry.setField(StandardField.JOURNAL, "Electron Device Letters, IEEE");
        leftEntry.setField(StandardField.PUBLISHER, "IEEE");
        leftEntry.setField(StandardField.YEAR, "2012");

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
                .withField(StandardField.YEAR, "2012");

        mergedEntry = new BibEntry();
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

    /*
// TODO: Fix this test
    @Test
    public void hasEqualLeftAndRightValuesShouldReturnTrueIfKeywordsAreEqual() {
        var keywordsFieldViewModel = createViewModelForField(StandardField.KEYWORDS);
        keywordsFieldViewModel.selectNonEmptyValue();
        assertTrue(keywordsFieldViewModel.hasEqualLeftAndRightValues());
    }*/

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

    public FieldRowViewModel createViewModelForField(Field field) {
        return new FieldRowViewModel(field, leftEntry, rightEntry, mergedEntry);
    }
}
