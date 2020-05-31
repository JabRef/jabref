package org.jabref.logic.importer;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.RisImporter;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;

/**
 * RisImporter Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>05/31/2020</pre>
 */
public class RisImporterTest {
    RisImporter ri = null;

    @BeforeEach
    public void setUp() throws Exception {
        ri = new RisImporter();
    }

    /**
     * Method: addDoi(Map<Field, String> hm, String val)
     */
    @Test
    public void testAddDoi1() throws Exception {
        String testDoi = "10.1589/jpts.28.186";
        String doi = testDoi.toLowerCase(Locale.ENGLISH);
        Map<Field, String> fields = new HashMap<>();
        Method testAddDoiMethod = ri.getClass().getDeclaredMethod("addDoi", Map.class, String.class);
        testAddDoiMethod.setAccessible(true);

        testAddDoiMethod.invoke(ri, fields, doi);
//        System.out.println(fields.get(StandardField.DOI));
        assertNotNull(fields.get(StandardField.DOI));
        assertEquals("10.1589/jpts.28.186",fields.get(StandardField.DOI));
    }

    @Test
    public void testAddDoi2() throws Exception {
        String testDoi = "doi:10.1589/jpts.28.186";
        String doi = testDoi.toLowerCase(Locale.ENGLISH);
        Map<Field, String> fields = new HashMap<>();
        Method testAddDoiMethod = ri.getClass().getDeclaredMethod("addDoi", Map.class, String.class);
        testAddDoiMethod.setAccessible(true);

        testAddDoiMethod.invoke(ri, fields, doi);
//        System.out.println(fields.get(StandardField.DOI));
        assertNotNull(fields.get(StandardField.DOI));
        assertEquals("10.1589/jpts.28.186",fields.get(StandardField.DOI));
    }
}
