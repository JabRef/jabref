package org.jabref.logic.cleanup;

import java.util.HashMap;
import java.util.Map;

import org.jabref.logic.formatter.casechanger.UpperCaseFormatter;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.FieldName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FieldFormatterCleanupTest {

    private BibEntry entry;
    private Map<String, String> fieldMap;

    @BeforeEach
    public void setUp() {
        fieldMap = new HashMap<>();
        entry = new BibEntry();

        entry.setType(BibtexEntryTypes.ARTICLE);
        fieldMap.put("title", "JabRef");
        fieldMap.put("booktitle", "JabRefBook");
        fieldMap.put("year", "twohundredsixteen");
        fieldMap.put("month", "october");
        fieldMap.put("abstract", "JabRefAbstract");
        fieldMap.put("doi", "jabrefdoi");
        fieldMap.put("issn", "jabrefissn");
        entry.setField(fieldMap);

    }

    @Test
    public void testInternalAllField() throws Exception {
        FieldFormatterCleanup cleanup = new FieldFormatterCleanup(FieldName.INTERNAL_ALL_FIELD, new UpperCaseFormatter());
        cleanup.cleanup(entry);

        assertEquals(fieldMap.get("title").toUpperCase(), entry.getField("title").get());
        assertEquals(fieldMap.get("booktitle").toUpperCase(), entry.getField("booktitle").get());
        assertEquals(fieldMap.get("year").toUpperCase(), entry.getField("year").get());
        assertEquals(fieldMap.get("month").toUpperCase(), entry.getField("month").get());
        assertEquals(fieldMap.get("abstract").toUpperCase(), entry.getField("abstract").get());
        assertEquals(fieldMap.get("doi").toUpperCase(), entry.getField("doi").get());
        assertEquals(fieldMap.get("issn").toUpperCase(), entry.getField("issn").get());
    }

    @Test
    public void testInternalAllTextFieldsField() throws Exception {
        FieldFormatterCleanup cleanup = new FieldFormatterCleanup(FieldName.INTERNAL_ALL_TEXT_FIELDS_FIELD, new UpperCaseFormatter());
        cleanup.cleanup(entry);

        assertEquals(fieldMap.get("title").toUpperCase(), entry.getField("title").get());
        assertEquals(fieldMap.get("booktitle").toUpperCase(), entry.getField("booktitle").get());
        assertEquals(fieldMap.get("year"), entry.getField("year").get());
        assertEquals(fieldMap.get("month"), entry.getField("month").get());
        assertEquals(fieldMap.get("abstract").toUpperCase(), entry.getField("abstract").get());
        assertEquals(fieldMap.get("doi"), entry.getField("doi").get());
        assertEquals(fieldMap.get("issn"), entry.getField("issn").get());
    }
}
