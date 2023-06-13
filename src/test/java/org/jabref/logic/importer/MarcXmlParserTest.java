package org.jabref.logic.importer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.importer.fileformat.MarcXmlParser;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MarcXmlParserTest {

    private MarcXmlParser parser;

    @BeforeEach
    public void setUp() {
        parser = new MarcXmlParser();
    }

    @AfterEach
    public void tearDown() {
        parser = null;
    }

    @Test
    public void testParseEntries() throws Exception {
        String xmlData = "<xml>...</xml>";
        InputStream inputStream = new ByteArrayInputStream(xmlData.getBytes());

        List<BibEntry> entries = parser.parseEntries(inputStream);
        List<BibEntry> expectedEntries = new ArrayList<>();
        assertEquals(expectedEntries, entries);
    }
}
