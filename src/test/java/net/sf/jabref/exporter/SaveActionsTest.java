package net.sf.jabref.exporter;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.model.entry.BibEntry;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
                "@comment{jabref-meta: saveActions:enabled;title[IdentityFormatter,]}"));

        ParserResult parserResult = parser.parse();

        List<String> saveActions = parserResult.getMetaData().getData(SaveActions.META_KEY);

        assertEquals("enabled", saveActions.get(0));
        assertEquals("title[IdentityFormatter,]", saveActions.get(1));

        SaveActions actions = new SaveActions(parserResult.getMetaData());

        BibEntry actedUpon = actions.applySaveActions(parserResult.getDatabase().getEntries().iterator().next());

        assertEquals("Educational session 1", actedUpon.getField("title"));
    }

    @Test
    public void invalidSaveActionSting() throws IOException {
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
                "@comment{jabref-meta: saveActions:enabled;title}"));

        ParserResult parserResult = parser.parse();

        SaveActions actions = new SaveActions(parserResult.getMetaData());

        BibEntry actedUpon = actions.applySaveActions(parserResult.getDatabase().getEntries().iterator().next());

        assertEquals("Educational session 1", actedUpon.getField("title"));
    }

    @Test
         public void checkLowerCaseSaveAction() throws IOException {
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
                "@comment{jabref-meta: saveActions:enabled;title[LowerCaseChanger,]}"));

        ParserResult parserResult = parser.parse();

        List<String> saveActions = parserResult.getMetaData().getData(SaveActions.META_KEY);

        assertEquals("enabled", saveActions.get(0));
        assertEquals("title[LowerCaseChanger,]", saveActions.get(1));

        SaveActions actions = new SaveActions(parserResult.getMetaData());

        BibEntry actedUpon = actions.applySaveActions(parserResult.getDatabase().getEntries().iterator().next());

        assertEquals("educational session 1", actedUpon.getField("title"));
    }

    @Test
    public void checkTwoSaveActionsForOneField() throws IOException {
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
                "@comment{jabref-meta: saveActions:enabled;title[LowerCaseChanger,IdentityFormatter,]}"));

        ParserResult parserResult = parser.parse();

        List<String> saveActions = parserResult.getMetaData().getData(SaveActions.META_KEY);

        assertEquals("enabled", saveActions.get(0));
        assertEquals("title[LowerCaseChanger,IdentityFormatter,]", saveActions.get(1));

        SaveActions actions = new SaveActions(parserResult.getMetaData());
        assertEquals(2, actions.getConfiguredActions().size());

        BibEntry actedUpon = actions.applySaveActions(parserResult.getDatabase().getEntries().iterator().next());

        assertEquals("educational session 1", actedUpon.getField("title"));
    }

    @Test
    public void checkThreeSaveActionsForOneField() throws IOException {
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
                "@comment{jabref-meta: saveActions:enabled;title[LowerCaseChanger,IdentityFormatter,DateFormatter,]}"));

        ParserResult parserResult = parser.parse();

        List<String> saveActions = parserResult.getMetaData().getData(SaveActions.META_KEY);

        assertEquals("enabled", saveActions.get(0));
        assertEquals("title[LowerCaseChanger,IdentityFormatter,DateFormatter,]", saveActions.get(1));

        SaveActions actions = new SaveActions(parserResult.getMetaData());
        assertEquals(3, actions.getConfiguredActions().size());

        BibEntry actedUpon = actions.applySaveActions(parserResult.getDatabase().getEntries().iterator().next());

        assertEquals("educational session 1", actedUpon.getField("title"));
    }


    @Test
    public void checkMultipleSaveActions() throws IOException {
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
                "@comment{jabref-meta: saveActions:enabled;pages[PageNumbersFormatter,]title[LowerCaseChanger,]}"));

        ParserResult parserResult = parser.parse();

        List<String> saveActions = parserResult.getMetaData().getData(SaveActions.META_KEY);

        assertEquals("enabled", saveActions.get(0));
        assertEquals("pages[PageNumbersFormatter,]title[LowerCaseChanger,]", saveActions.get(1));

        SaveActions actions = new SaveActions(parserResult.getMetaData());
        List<FieldFormatterCleanup> formatterCleanups = actions.getConfiguredActions();
        assertEquals(2, formatterCleanups.size());
        for(FieldFormatterCleanup cleanup: formatterCleanups){
            if(cleanup.getField().equals("title")){
                assertEquals("LowerCaseChanger", cleanup.getFormatter().getKey());
            }  else if(cleanup.getField().equals("pages")) {
                assertEquals("PageNumbersFormatter", cleanup.getFormatter().getKey());
            }
        }

        BibEntry actedUpon = actions.applySaveActions(parserResult.getDatabase().getEntries().iterator().next());

        assertEquals("educational session 1", actedUpon.getField("title"));
        assertEquals("1--7", actedUpon.getField("pages"));
    }

    @Test
    public void checkMultipleSaveActionsWithMultipleFormatters() throws IOException {
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
                "@comment{jabref-meta: saveActions:enabled;pages[PageNumbersFormatter,DateFormatter,]title[LowerCaseChanger,]}"));

        ParserResult parserResult = parser.parse();

        List<String> saveActions = parserResult.getMetaData().getData(SaveActions.META_KEY);

        assertEquals("enabled", saveActions.get(0));
        assertEquals("pages[PageNumbersFormatter,DateFormatter,]title[LowerCaseChanger,]", saveActions.get(1));

        SaveActions actions = new SaveActions(parserResult.getMetaData());
        List<FieldFormatterCleanup> formatterCleanups = actions.getConfiguredActions();
        assertEquals(3, formatterCleanups.size());
        for(FieldFormatterCleanup cleanup: formatterCleanups){
            if(cleanup.getField().equals("title")){
                assertEquals("LowerCaseChanger", cleanup.getFormatter().getKey());
            }  else if(cleanup.getField().equals("pages")) {
                if(!("PageNumbersFormatter".equals(cleanup.getFormatter().getKey()) || "DateFormatter".equals(cleanup.getFormatter().getKey()))) {
                    fail("Wrong formatter for pages field: "+ cleanup.getFormatter().getKey());
                }
            }
        }

        BibEntry actedUpon = actions.applySaveActions(parserResult.getDatabase().getEntries().iterator().next());

        assertEquals("educational session 1", actedUpon.getField("title"));
        assertEquals("1--7", actedUpon.getField("pages"));
    }

}
