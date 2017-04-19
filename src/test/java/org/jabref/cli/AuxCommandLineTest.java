package org.jabref.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

import static org.mockito.Mockito.mock;

public class AuxCommandLineTest {

    private ImportFormatPreferences importFormatPreferences;

    @Before
    public void setUp() throws Exception {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
    }

    @Test
    public void test() throws URISyntaxException, IOException {
        InputStream originalStream = AuxCommandLineTest.class.getResourceAsStream("origin.bib");

        File auxFile = Paths.get(AuxCommandLineTest.class.getResource("paper.aux").toURI()).toFile();
        try (InputStreamReader originalReader = new InputStreamReader(originalStream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences).parse(originalReader);

            AuxCommandLine auxCommandLine = new AuxCommandLine(auxFile.getAbsolutePath(), result.getDatabase());
            BibDatabase newDB = auxCommandLine.perform();
            Assert.assertNotNull(newDB);
            Assert.assertEquals(2, newDB.getEntries().size());
        }
    }

}
