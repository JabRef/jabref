package org.jabref.logic.bibtex.comparator;

import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FieldComparatorTest {
    @Test
    public void compareMonthFieldIdentity() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.MONTH);
        BibEntry equal = new BibEntry()
                .withField(StandardField.MONTH, "1");

        assertEquals(0, comparator.compare(equal, equal));
    }

    @Test
    public void compareMonthFieldEquality() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.MONTH);
        BibEntry equal = new BibEntry()
                .withField(StandardField.MONTH, "1");
        BibEntry equal2 = new BibEntry()
                .withField(StandardField.MONTH, "1");

        assertEquals(0, comparator.compare(equal, equal2));
    }

    @Test
    public void compareMonthFieldBiggerAscending() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.MONTH);
        BibEntry smaller = new BibEntry()
                .withField(StandardField.MONTH, "jan");
        BibEntry bigger = new BibEntry()
                .withField(StandardField.MONTH, "feb");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareMonthFieldBiggerDescending() throws Exception {
        FieldComparator comparator = new FieldComparator(new OrFields(StandardField.MONTH), true);
        BibEntry smaller = new BibEntry()
                .withField(StandardField.MONTH, "feb");
        BibEntry bigger = new BibEntry()
                .withField(StandardField.MONTH, "jan");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareYearFieldIdentity() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.YEAR);
        BibEntry equal = new BibEntry()
                .withField(StandardField.YEAR, "2016");

        assertEquals(0, comparator.compare(equal, equal));
    }

    @Test
    public void compareYearFieldEquality() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.YEAR);
        BibEntry equal = new BibEntry()
                .withField(StandardField.YEAR, "2016");
        BibEntry equal2 = new BibEntry()
                .withField(StandardField.YEAR, "2016");

        assertEquals(0, comparator.compare(equal, equal2));
    }

    @Test
    public void compareYearFieldBiggerAscending() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.YEAR);
        BibEntry smaller = new BibEntry()
                .withField(StandardField.YEAR, "2000");
        BibEntry bigger = new BibEntry()
                .withField(StandardField.YEAR, "2016");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareYearFieldBiggerDescending() throws Exception {
        FieldComparator comparator = new FieldComparator(new OrFields(StandardField.YEAR), true);
        BibEntry smaller = new BibEntry()
                .withField(StandardField.YEAR, "2016");
        BibEntry bigger = new BibEntry()
                .withField(StandardField.YEAR, "2000");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareTypeFieldIdentity() throws Exception {
        FieldComparator comparator = new FieldComparator(InternalField.TYPE_HEADER);
        BibEntry equal = new BibEntry(StandardEntryType.Article);

        assertEquals(0, comparator.compare(equal, equal));
    }

    @Test
    public void compareTypeFieldEquality() throws Exception {
        FieldComparator comparator = new FieldComparator(InternalField.TYPE_HEADER);
        BibEntry equal = new BibEntry(StandardEntryType.Article);
        equal.setId("1");
        BibEntry equal2 = new BibEntry(StandardEntryType.Article);
        equal2.setId("1");

        assertEquals(0, comparator.compare(equal, equal2));
    }

    @Test
    public void compareTypeFieldBiggerAscending() throws Exception {
        FieldComparator comparator = new FieldComparator(InternalField.TYPE_HEADER);
        BibEntry smaller = new BibEntry(StandardEntryType.Article);
        BibEntry bigger = new BibEntry(StandardEntryType.Book);

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareTypeFieldBiggerDescending() throws Exception {
        FieldComparator comparator = new FieldComparator(new OrFields(InternalField.TYPE_HEADER), true);
        BibEntry bigger = new BibEntry(StandardEntryType.Article);
        BibEntry smaller = new BibEntry(StandardEntryType.Book);

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareStringFieldsIdentity() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.TITLE);
        BibEntry equal = new BibEntry()
                .withField(StandardField.TITLE, "title");

        assertEquals(0, comparator.compare(equal, equal));
    }

    @Test
    public void compareStringFieldsEquality() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.TITLE);
        BibEntry equal = new BibEntry()
                .withField(StandardField.TITLE, "title");
        BibEntry equal2 = new BibEntry()
                .withField(StandardField.TITLE, "title");

        assertEquals(0, comparator.compare(equal, equal2));
    }

    @Test
    public void compareStringFieldsBiggerAscending() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.TITLE);
        BibEntry bigger = new BibEntry()
                .withField(StandardField.TITLE, "b");
        BibEntry smaller = new BibEntry()
                .withField(StandardField.TITLE, "a");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareStringFieldsBiggerDescending() throws Exception {
        FieldComparator comparator = new FieldComparator(new OrFields(StandardField.TITLE), true);
        BibEntry bigger = new BibEntry()
                .withField(StandardField.TITLE, "a");
        BibEntry smaller = new BibEntry()
                .withField(StandardField.TITLE, "b");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareNumericFieldsBiggerDescending() throws Exception {
        FieldComparator comparator = new FieldComparator(new OrFields(StandardField.PMID), true);
        BibEntry smaller = new BibEntry()
                .withField(StandardField.PMID, "234567");
        BibEntry bigger = new BibEntry()
                .withField(StandardField.PMID, "123456");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareParsableWithNonParsableNumericFieldDescending() throws Exception {
        FieldComparator comparator = new FieldComparator(new OrFields(StandardField.PMID), true);
        BibEntry parsable = new BibEntry()
                .withField(StandardField.PMID, "123456");
        BibEntry unparsable = new BibEntry()
                .withField(StandardField.PMID, "abc##z");

        assertEquals(1, comparator.compare(parsable, unparsable));
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsForNumericalComparison")
    public void compareNumericalValues(int comparisonResult, String id1, String id2, String errorMessage) {
        FieldComparator comparator = new FieldComparator(StandardField.PMID);
        BibEntry entry1 = new BibEntry()
                .withField(StandardField.PMID, id1);
        BibEntry entry2 = new BibEntry()
                .withField(StandardField.PMID, id2);

        assertEquals(comparisonResult, comparator.compare(entry1, entry2), errorMessage);
    }

    private static Stream<Arguments> provideArgumentsForNumericalComparison() {
        return Stream.of(
                Arguments.of(0, "123456", "123456", "IDs are lexicographically not equal [1]"),
                Arguments.of(1, "234567", "123456", "234567 is lexicographically smaller than 123456"),
                Arguments.of(1, "abc##z", "123456", "abc##z is lexicographically smaller than 123456 "),
                Arguments.of(0, "", "", "IDs are lexicographically not equal [2]"),
                Arguments.of(1, "", "123456", "No ID is lexicographically smaller than 123456"),
                Arguments.of(-1, "123456", "", "123456 is lexicographically greater than no ID")
        );
    }
}
