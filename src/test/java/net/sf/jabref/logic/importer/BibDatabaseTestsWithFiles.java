package net.sf.jabref.logic.importer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BibDatabaseTestsWithFiles {

    private ImportFormatPreferences importFormatPreferences;

    @Before
    public void setUp() {
        importFormatPreferences = JabRefPreferences.getInstance().getImportFormatPreferences();
    }

    @Test
    public void resolveStrings() throws IOException {
        try (FileInputStream stream = new FileInputStream("src/test/resources/net/sf/jabref/util/twente.bib");
                InputStreamReader fr = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            ParserResult result = BibtexParser.parse(fr, importFormatPreferences);

            BibDatabase db = result.getDatabase();

            assertEquals("Arvind", db.resolveForStrings("#Arvind#"));
            assertEquals("Patterson, David", db.resolveForStrings("#Patterson#"));
            assertEquals("Arvind and Patterson, David", db.resolveForStrings("#Arvind# and #Patterson#"));

            // Strings that are not found return just the given string.
            assertEquals("#unknown#", db.resolveForStrings("#unknown#"));
        }
    }

}
