package org.jabref.logic.cleanup;

import java.util.HashMap;
import java.util.Map;

import org.jabref.logic.formatter.bibtexfields.UnicodeToLatexFormatter;
import org.jabref.logic.formatter.casechanger.UpperCaseFormatter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FieldFormatterCleanupTest {

    private BibEntry entry;
    private Map<Field, String> fieldMap;

    @BeforeEach
    public void setUp() {
        fieldMap = new HashMap<>();
        entry = new BibEntry();

        entry.setType(StandardEntryType.Article);
        fieldMap.put(StandardField.TITLE, "JabRef");
        fieldMap.put(StandardField.BOOKTITLE, "JabRefBook");
        fieldMap.put(StandardField.YEAR, "twohundredsixteen");
        fieldMap.put(StandardField.MONTH, "october");
        fieldMap.put(StandardField.ABSTRACT, "JabRefAbstract");
        fieldMap.put(StandardField.DOI, "jabrefdoi");
        fieldMap.put(StandardField.ISSN, "jabrefissn");
        entry.setField(fieldMap);
    }

    @Test
    public void testInternalAllField() throws Exception {
        FieldFormatterCleanup cleanup = new FieldFormatterCleanup(InternalField.INTERNAL_ALL_FIELD, new UpperCaseFormatter());
        cleanup.cleanup(entry);

        assertEquals(fieldMap.get(StandardField.TITLE).toUpperCase(), entry.getField(StandardField.TITLE).get());
        assertEquals(fieldMap.get(StandardField.BOOKTITLE).toUpperCase(), entry.getField(StandardField.BOOKTITLE).get());
        assertEquals(fieldMap.get(StandardField.YEAR).toUpperCase(), entry.getField(StandardField.YEAR).get());
        assertEquals(fieldMap.get(StandardField.MONTH).toUpperCase(), entry.getField(StandardField.MONTH).get());
        assertEquals(fieldMap.get(StandardField.ABSTRACT).toUpperCase(), entry.getField(StandardField.ABSTRACT).get());
        assertEquals(fieldMap.get(StandardField.DOI).toUpperCase(), entry.getField(StandardField.DOI).get());
        assertEquals(fieldMap.get(StandardField.ISSN).toUpperCase(), entry.getField(StandardField.ISSN).get());
    }

    @Test
    public void testInternalAllTextFieldsField() throws Exception {
        FieldFormatterCleanup cleanup = new FieldFormatterCleanup(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, new UpperCaseFormatter());
        cleanup.cleanup(entry);

        assertEquals(fieldMap.get(StandardField.TITLE).toUpperCase(), entry.getField(StandardField.TITLE).get());
        assertEquals(fieldMap.get(StandardField.BOOKTITLE).toUpperCase(), entry.getField(StandardField.BOOKTITLE).get());
        assertEquals(fieldMap.get(StandardField.YEAR), entry.getField(StandardField.YEAR).get());
        assertEquals(fieldMap.get(StandardField.MONTH), entry.getField(StandardField.MONTH).get());
        assertEquals(fieldMap.get(StandardField.ABSTRACT).toUpperCase(), entry.getField(StandardField.ABSTRACT).get());
        assertEquals(fieldMap.get(StandardField.DOI), entry.getField(StandardField.DOI).get());
        assertEquals(fieldMap.get(StandardField.ISSN), entry.getField(StandardField.ISSN).get());
    }

    @Test
    public void testCleanupAllFieldsIgnoresKeyField() throws Exception {
        FieldFormatterCleanup cleanup = new FieldFormatterCleanup(InternalField.INTERNAL_ALL_FIELD, new UnicodeToLatexFormatter());
        entry.setField(InternalField.KEY_FIELD, "François-Marie Arouet"); // Contains ç, not in Basic Latin
        cleanup.cleanup(entry);

        assertEquals("François-Marie Arouet", entry.getField(InternalField.KEY_FIELD).get());
    }

    @Test
    public void testCleanupAllTextFieldsIgnoresKeyField() throws Exception {
        FieldFormatterCleanup cleanup = new FieldFormatterCleanup(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, new UnicodeToLatexFormatter());
        entry.setField(InternalField.KEY_FIELD, "François-Marie Arouet"); // Contains ç, not in Basic Latin
        cleanup.cleanup(entry);

        assertEquals("François-Marie Arouet", entry.getField(InternalField.KEY_FIELD).get());
    }

    @Test
    public void testCleanupKeyFieldCleansUpKeyField() throws Exception {
        FieldFormatterCleanup cleanup = new FieldFormatterCleanup(InternalField.KEY_FIELD, new UnicodeToLatexFormatter());
        entry.setField(InternalField.KEY_FIELD, "François-Marie Arouet"); // Contains ç, not in Basic Latin
        cleanup.cleanup(entry);

        assertEquals("Fran{\\c{c}}ois-Marie Arouet", entry.getField(InternalField.KEY_FIELD).get());
    }
}
