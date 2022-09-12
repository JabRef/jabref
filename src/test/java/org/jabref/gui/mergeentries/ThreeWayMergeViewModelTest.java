package org.jabref.gui.mergeentries;

import java.util.HashSet;
import java.util.List;

import org.jabref.gui.mergeentries.newmergedialog.ThreeWayMergeViewModel;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;

import com.google.common.collect.Comparators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThreeWayMergeViewModelTest {

    ThreeWayMergeViewModel viewModel;
    BibEntry leftEntry;
    BibEntry rightEntry;
    List<Field> visibleFields;

    @BeforeEach
    void setup() {
        leftEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Erik G. Larsson and Oscar Gustafsson")
                .withField(StandardField.TITLE, "The Impact of Dynamic Voltage and Frequency Scaling on Multicore {DSP} Algorithm Design [Exploratory {DSP]}")
                .withField(StandardField.NUMBER, "1")
                .withField(new UnknownField("custom"), "1.2.3");

        rightEntry = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.AUTHOR, "Henrik Ohlsson and Oscar Gustafsson and Lars Wanhammar")
                .withField(StandardField.TITLE, "Arithmetic transformations for increased maximal sample rate of bit-parallel bireciprocal lattice wave digital filters")
                .withField(StandardField.BOOKTITLE, "Proceedings of the 2001 International Symposium on Circuits and Systems, {ISCAS} 2001, Sydney, Australia, May 6-9, 2001")
                .withField(StandardField.NUMBER, "2");
        viewModel = new ThreeWayMergeViewModel(leftEntry, rightEntry, "left", "right");
        visibleFields = viewModel.getVisibleFields();
    }

    @Test
    void getVisibleFieldsShouldReturnASortedListOfFieldsWithEntryTypeAtTheHeadOfTheList() {
        List<String> names = visibleFields.stream().map(Field::getName).skip(1).toList();
        Comparators.isInOrder(names, String::compareTo);
    }

    @Test
    void getVisibleFieldsShouldNotHaveDuplicates() {
        assertEquals(new HashSet<>(visibleFields).size(), viewModel.numberOfVisibleFields());
    }

    @Test
    void getVisibleFieldsShouldHaveEntryTypeFieldAtTheHeadOfTheList() {
        assertEquals(InternalField.TYPE_HEADER, visibleFields.get(0));
    }

    @Test
    void getVisibleFieldsShouldContainAllNonInternalFieldsInRightAndLeftEntry() {
        assertTrue(visibleFields.containsAll(leftEntry.getFields().stream().filter(this::isNotInternalField).toList()));
        assertTrue(visibleFields.containsAll(rightEntry.getFields().stream().filter(this::isNotInternalField).toList()));
    }

    @Test
    void getVisibleFieldsShouldIncludeCustomFields() {
        assertTrue(visibleFields.contains(new UnknownField("custom")));
    }

    private boolean isNotInternalField(Field field) {
        return !FieldFactory.isInternalField(field);
    }
}
