package net.sf.jabref.exporter;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.entry.BibEntry;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SaveActionsTest {

    @BeforeClass
    public static void setUpPreferences() {
        if (Globals.prefs == null) {
            Globals.prefs = JabRefPreferences.getInstance();
        }
    }

    @Test
    public void checkSaveActionsParsing() throws IOException {
        BibtexParser parser = new BibtexParser(new StringReader("@InProceedings{6055279,\n" +
                "  Title                    = {Educational session 1},\n" +
                "  Booktitle                = {Custom Integrated Circuits Conference (CICC), 2011 IEEE},\n" +
                "  Year                     = {2011},\n" +
                "  Month                    = {Sept},\n" +
                "  Pages                    = {1-7},\n" +
                "  Abstract                 = {Start of the above-titled section of the conference proceedings record.},\n" +
                "  DOI                      = {10.1109/CICC.2011.6055279},\n" +
                "  ISSN                     = {0886-5930}\n" +
                "}\n" +
                "\n" +
                "@comment{jabref-meta: saveActions:title;IdentityFormatter;}"));

        ParserResult parserResult = parser.parse();

        List<String> saveActions = parserResult.getMetaData().getData(SaveActions.META_KEY);

        assertEquals("title", saveActions.get(0));
        assertEquals("IdentityFormatter", saveActions.get(1));

        SaveActions actions = new SaveActions(parserResult.getMetaData());

        BibEntry actedUpon = actions.applySaveActions(parserResult.getDatabase().getEntries().iterator().next());

        assertEquals("Educational session 1", actedUpon.getField("title"));

    }
}
