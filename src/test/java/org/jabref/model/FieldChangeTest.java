package org.jabref.model;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldChangeTest {

  private FieldChange Fieldchange;

  @Test
  void testHashCode() {
    FieldChange fcNull = new FieldChange(null, null, null, null);
    assertEquals(923521, fcNull.hashCode());
  }

  @Test
  void testEquals() {
    BibEntry entry = new BibEntry();
    BibEntry entryOther = new BibEntry();
    final Field field = StandardField.DOI;
    final Field fieldOther = StandardField.DOI;
    entry.setField(StandardField.DOI, "foo");
    final String oldValue = "foo";
    final String newValue = "bar";
    final String oldValueOther = "fooX";
    final String newValueOther = "barX";

    FieldChange fc = new FieldChange(entry, field, oldValue, newValue);
    FieldChange fcOther = new FieldChange(entryOther, fieldOther, oldValueOther, newValueOther);
    FieldChange fcBlankAll = new FieldChange(null, null, null, null);
    FieldChange fcBlankField = new FieldChange(entry, null, oldValue, newValue);
    FieldChange fcBlankOldValue = new FieldChange(entry, field, null, newValue);
    FieldChange fcBlankNewValue = new FieldChange(entry, field, oldValue, null);

    assertFalse(fc.equals("foo"));
    assertTrue(fc.equals(fc));
    assertFalse(fcBlankAll.equals(fc));
    assertFalse(fc.equals(fcOther));
    assertFalse(fcBlankField.equals(fc));
    assertFalse(fcBlankOldValue.equals(fc));
    assertFalse(fcBlankNewValue.equals(fc));
    assertTrue(fcBlankAll.equals(fcBlankAll));
  }

  @Test
  void testToString() {
    BibEntry entry = new BibEntry();
    Field field = StandardField.DOI;
    entry.setCitationKey("CitationKey");
    final String oldValue = "Old";
    final String newValue = "New";

    FieldChange fc = new FieldChange(entry, field, oldValue, newValue);
    assertEquals("FieldChange [entry=CitationKey, field=DOI, oldValue=Old, newValue=New]", fc.toString());
  }
}
