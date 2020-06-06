package org.jabref.logic.importer;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jabref.logic.importer.fileformat.RisImporter;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RisImporterTest {
    RisImporter ri = null;

    @BeforeEach
    public void setUp() throws Exception {
        ri = new RisImporter();
    }

    @Test
    public void testAddDoi1() throws Exception {
        String testDoi = "10.1589/jpts.28.186";
        String doi = testDoi.toLowerCase(Locale.ENGLISH);
        Map<Field, String> fields = new HashMap<>();
        Method testAddDoiMethod = ri.getClass().getDeclaredMethod("addDoi", Map.class, String.class);
        testAddDoiMethod.setAccessible(true);

        testAddDoiMethod.invoke(ri, fields, doi);
        assertNotNull(fields.get(StandardField.DOI));
        assertEquals("10.1589/jpts.28.186", fields.get(StandardField.DOI));
    }

    @Test
    public void testAddDoi2() throws Exception {
        String testDoi = "doi:10.1589/jpts.28.186";
        String doi = testDoi.toLowerCase(Locale.ENGLISH);
        Map<Field, String> fields = new HashMap<>();
        Method testAddDoiMethod = ri.getClass().getDeclaredMethod("addDoi", Map.class, String.class);
        testAddDoiMethod.setAccessible(true);

        testAddDoiMethod.invoke(ri, fields, doi);
        assertNotNull(fields.get(StandardField.DOI));
        assertEquals("10.1589/jpts.28.186", fields.get(StandardField.DOI));
    }
}
