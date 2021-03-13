package org.jabref.logic.bibtex.comparator;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FieldComparatorTest {
    @Test
    public void compareMonthFieldIdentity() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.MONTH);
        BibEntry equal = new BibEntry();
        equal.setField(StandardField.MONTH, "1");

        assertEquals(0, comparator.compare(equal, equal));
    }

    @Test
    public void compareMonthFieldEquality() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.MONTH);
        BibEntry equal = new BibEntry();
        equal.setField(StandardField.MONTH, "1");
        BibEntry equal2 = new BibEntry();
        equal2.setField(StandardField.MONTH, "1");

        assertEquals(0, comparator.compare(equal, equal2));
    }

    @Test
    public void compareMonthFieldBiggerAscending() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.MONTH);
        BibEntry smaller = new BibEntry();
        smaller.setField(StandardField.MONTH, "jan");
        BibEntry bigger = new BibEntry();
        bigger.setField(StandardField.MONTH, "feb");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareMonthFieldBiggerDescending() throws Exception {
        FieldComparator comparator = new FieldComparator(new OrFields(StandardField.MONTH), true);
        BibEntry smaller = new BibEntry();
        smaller.setField(StandardField.MONTH, "feb");
        BibEntry bigger = new BibEntry();
        bigger.setField(StandardField.MONTH, "jan");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareYearFieldIdentity() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.YEAR);
        BibEntry equal = new BibEntry();
        equal.setField(StandardField.YEAR, "2016");

        assertEquals(0, comparator.compare(equal, equal));
    }

    @Test
    public void compareYearFieldEquality() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.YEAR);
        BibEntry equal = new BibEntry();
        equal.setField(StandardField.YEAR, "2016");
        BibEntry equal2 = new BibEntry();
        equal2.setField(StandardField.YEAR, "2016");

        assertEquals(0, comparator.compare(equal, equal2));
    }

    @Test
    public void compareYearFieldBiggerAscending() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.YEAR);
        BibEntry smaller = new BibEntry();
        smaller.setField(StandardField.YEAR, "2000");
        BibEntry bigger = new BibEntry();
        bigger.setField(StandardField.YEAR, "2016");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareYearFieldBiggerDescending() throws Exception {
        FieldComparator comparator = new FieldComparator(new OrFields(StandardField.YEAR), true);
        BibEntry smaller = new BibEntry();
        smaller.setField(StandardField.YEAR, "2016");
        BibEntry bigger = new BibEntry();
        bigger.setField(StandardField.YEAR, "2000");

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
        BibEntry equal = new BibEntry();
        equal.setField(StandardField.TITLE, "title");

        assertEquals(0, comparator.compare(equal, equal));
    }

    @Test
    public void compareStringFieldsEquality() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.TITLE);
        BibEntry equal = new BibEntry();
        equal.setField(StandardField.TITLE, "title");
        BibEntry equal2 = new BibEntry();
        equal2.setField(StandardField.TITLE, "title");

        assertEquals(0, comparator.compare(equal, equal2));
    }

    @Test
    public void compareStringFieldsBiggerAscending() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.TITLE);
        BibEntry bigger = new BibEntry();
        bigger.setField(StandardField.TITLE, "b");
        BibEntry smaller = new BibEntry();
        smaller.setField(StandardField.TITLE, "a");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareStringFieldsBiggerDescending() throws Exception {
        FieldComparator comparator = new FieldComparator(new OrFields(StandardField.TITLE), true);
        BibEntry bigger = new BibEntry();
        bigger.setField(StandardField.TITLE, "a");
        BibEntry smaller = new BibEntry();
        smaller.setField(StandardField.TITLE, "b");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareNumericFieldsIdentity() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.PMID);
        BibEntry equal = new BibEntry();
        equal.setField(StandardField.PMID, "123456");

        assertEquals(0, comparator.compare(equal, equal));
    }

    @Test
    public void compareNumericFieldsEquality() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.PMID);
        BibEntry equal = new BibEntry();
        equal.setField(StandardField.PMID, "123456");
        BibEntry equal2 = new BibEntry();
        equal2.setField(StandardField.PMID, "123456");

        assertEquals(0, comparator.compare(equal, equal2));
    }

    @Test
    public void compareNumericFieldsBiggerAscending() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.PMID);
        BibEntry bigger = new BibEntry();
        bigger.setField(StandardField.PMID, "234567");
        BibEntry smaller = new BibEntry();
        smaller.setField(StandardField.PMID, "123456");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareNumericFieldsBiggerDescending() throws Exception {
        FieldComparator comparator = new FieldComparator(new OrFields(StandardField.PMID), true);
        BibEntry smaller = new BibEntry();
        smaller.setField(StandardField.PMID, "234567");
        BibEntry bigger = new BibEntry();
        bigger.setField(StandardField.PMID, "123456");

        assertEquals(1, comparator.compare(bigger, smaller));
    }

    @Test
    public void compareParsableWithNonParsableNumericFieldDescending() throws Exception {
        FieldComparator comparator = new FieldComparator(new OrFields(StandardField.PMID), true);
        BibEntry parsable = new BibEntry();
        parsable.setField(StandardField.PMID, "123456");
        BibEntry unparsable = new BibEntry();
        unparsable.setField(StandardField.PMID, "abc##z");

        assertEquals(1, comparator.compare(parsable, unparsable));
    }

    @Test
    public void compareNonParsableWithParsableFieldAscending() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.PMID);
        BibEntry nonparsable = new BibEntry();
        nonparsable.setField(StandardField.PMID, "abc##z");
        BibEntry parsable = new BibEntry();
        parsable.setField(StandardField.PMID, "123456");

        assertEquals(1, comparator.compare(nonparsable, parsable));
    }

    @Test
    public void compareEmptyFieldsAscending() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.PMID);
        BibEntry empty1 = new BibEntry();
        BibEntry empty2 = new BibEntry();

        assertEquals(0, comparator.compare(empty1, empty2));
    }

    @Test
    public void compareEmptyWithAssignedFieldAscending() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.PMID);
        BibEntry empty1 = new BibEntry();
        BibEntry assigned = new BibEntry();
        assigned.setField(StandardField.PMID, "123456");

        assertEquals(1, comparator.compare(empty1, assigned));
    }

    @Test
    public void compareAssignedWithEmptyFieldAscending() throws Exception {
        FieldComparator comparator = new FieldComparator(StandardField.PMID);
        BibEntry assigned = new BibEntry();
        assigned.setField(StandardField.PMID, "123456");
        BibEntry empty = new BibEntry();

        assertEquals(-1, comparator.compare(assigned, empty));
    }
}
