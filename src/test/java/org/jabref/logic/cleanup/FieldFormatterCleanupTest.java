package org.jabref.logic.cleanup;


import java.util.HashMap;
import java.util.Map;

import org.jabref.logic.formatter.casechanger.UpperCaseFormatter;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.FieldName;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FieldFormatterCleanupTest {

    private BibEntry entry;
    private Map <String, String> fieldMap;

    @Before
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

        Assert.assertEquals(fieldMap.get("title").toUpperCase(), entry.getField("title").get());
        Assert.assertEquals(fieldMap.get("booktitle").toUpperCase(), entry.getField("booktitle").get());
        Assert.assertEquals(fieldMap.get("year").toUpperCase(), entry.getField("year").get());
        Assert.assertEquals(fieldMap.get("month").toUpperCase(), entry.getField("month").get());
        Assert.assertEquals(fieldMap.get("abstract").toUpperCase(), entry.getField("abstract").get());
        Assert.assertEquals(fieldMap.get("doi").toUpperCase(), entry.getField("doi").get());
        Assert.assertEquals(fieldMap.get("issn").toUpperCase(), entry.getField("issn").get());
    }

    @Test
    public void testInternalAllTextFieldsField() throws Exception {
        FieldFormatterCleanup cleanup = new FieldFormatterCleanup(FieldName.INTERNAL_ALL_TEXT_FIELDS_FIELD, new UpperCaseFormatter());
        cleanup.cleanup(entry);

        Assert.assertEquals(fieldMap.get("title").toUpperCase(), entry.getField("title").get());
        Assert.assertEquals(fieldMap.get("booktitle").toUpperCase(), entry.getField("booktitle").get());
        Assert.assertEquals(fieldMap.get("year"), entry.getField("year").get());
        Assert.assertEquals(fieldMap.get("month"), entry.getField("month").get());
        Assert.assertEquals(fieldMap.get("abstract").toUpperCase(), entry.getField("abstract").get());
        Assert.assertEquals(fieldMap.get("doi"), entry.getField("doi").get());
        Assert.assertEquals(fieldMap.get("issn"), entry.getField("issn").get());
    }
}
